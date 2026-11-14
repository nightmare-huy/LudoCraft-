import numpy as np, soundfile as sf

SR = 44100
rng = np.random.default_rng(777)

def t_axis(dur):
    return np.arange(int(dur * SR)) / SR

def note_hz(name):
    names = {'C':-9,'C#':-8,'D':-7,'D#':-6,'E':-5,'F':-4,'F#':-3,'G':-2,'G#':-1,'A':0,'A#':1,'Bb':1,'B':2,'Ab':-1,'Eb':-6}
    n, octave = name[:-1], int(name[-1])
    return 440.0 * 2 ** ((names[n] + 12 * (octave - 4)) / 12)

def add(buf, sig, start):
    i = int(start * SR)
    end = min(len(buf), i + len(sig))
    buf[i:end] += sig[:end - i]

def bell(freq, dur=1.2, amp=1.0, bright=1.0):
    t = t_axis(dur)
    partials = [(1.0, 1.0, 0.9), (2.0, 0.55, 0.55), (2.76, 0.40 * bright, 0.35), (5.40, 0.22 * bright, 0.18), (8.93, 0.12 * bright, 0.10)]
    s = np.zeros_like(t)
    for ratio, a, tau in partials:
        f = freq * ratio
        if f > SR / 2.2: continue
        s += a * np.sin(2 * np.pi * f * t + rng.uniform(0, 2*np.pi)) * np.exp(-t / tau)
    s *= np.clip(t / 0.002, 0, 1)
    return amp * s / 2.3

def clack(amp=1.0):
    n = int(0.06 * SR); t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    # crude band-pass: difference of two one-pole lowpasses
    def lp(x, fc):
        a = np.exp(-2*np.pi*fc/SR); y = np.zeros_like(x); acc = 0.0
        for i, v in enumerate(x):
            acc = (1-a)*v + a*acc; y[i] = acc
        return y
    bp = lp(noise, 4200) - lp(noise, 1200)
    click = bp * np.exp(-t / 0.008)
    thunk = np.sin(2*np.pi*110*t) * np.exp(-t / 0.025)
    s = 2.2 * click + 0.8 * thunk
    return amp * s / np.max(np.abs(s))

def square_soft(freq, dur, amp=1.0, harmonics=9):
    t = t_axis(dur); s = np.zeros_like(t)
    for k in range(1, harmonics*2, 2):
        if freq * k > 9000: break
        s += np.sin(2*np.pi*freq*k*t) / k
    e = np.clip(t / 0.004, 0, 1) * np.exp(-t / (dur * 0.45))
    return amp * s * e * 0.8

def brass_chord(freqs, dur, amp=1.0):
    t = t_axis(dur); s = np.zeros_like(t)
    vib = 1 + (2 ** (14/1200) - 1) * np.sin(2*np.pi*5.6*t) * np.clip((t - 0.25) / 0.3, 0, 1)
    for f0 in freqs:
        for detune in (-5, 0, 5):
            f = f0 * 2 ** (detune / 1200)
            phase = 2*np.pi*np.cumsum(f * vib) / SR
            for n in range(1, 14):
                if f*n > 8000: break
                # brightness swells with the envelope (brass-like)
                s += (1.0 / n**1.15) * np.sin(n * phase + rng.uniform(0, 2*np.pi)) * np.exp(-((n*f) / 3800.0) ** 2)
    attack = np.clip(t / 0.035, 0, 1)
    decay = 0.75 + 0.25 * np.exp(-t / 0.15)
    release = np.clip((dur - t) / 0.55, 0, 1) ** 1.5
    e = attack * decay * release
    s = s * e
    return amp * s / np.max(np.abs(s))

def payout_bell(dur, rate=17.0, freq=2093.0, amp=1.0):
    out = np.zeros(int(dur * SR))
    k = 0
    while k / rate < dur - 0.05:
        strike = bell(freq * (1 + rng.uniform(-0.002, 0.002)), dur=0.25, amp=1.0, bright=0.6)
        add(out, strike * (0.75 + 0.25 * rng.random()), k / rate)
        k += 1
    t = t_axis(dur)
    out *= np.clip((dur - t) / 0.6, 0, 1)
    return amp * out / np.max(np.abs(out))

def coin(amp=1.0):
    f0 = rng.uniform(2600, 4300)
    t = t_axis(0.18); s = np.zeros_like(t)
    for ratio, a, tau in ((1.0, 1.0, 0.05), (1.47, 0.6, 0.035), (2.09, 0.45, 0.025), (2.83, 0.3, 0.018)):
        f = f0 * ratio
        if f > SR / 2.2: continue
        s += a * np.sin(2*np.pi*f*t + rng.uniform(0, 2*np.pi)) * np.exp(-t / tau)
    s *= np.clip(t / 0.0008, 0, 1)
    return amp * s / 2.0

def jackpot():
    dur = 3.1
    out = np.zeros(int(dur * SR))
    # 1) three reels stop on 7-7-7: clack + ascending ding
    for i, n in enumerate(['C6', 'E6', 'G6']):
        t0 = 0.02 + i * 0.17
        add(out, clack(0.35), t0)
        add(out, bell(note_hz(n), dur=0.9, amp=0.55), t0 + 0.004)
    # 2) seven-note arpeggio ("777")
    arp = ['C6', 'E6', 'G6', 'C7', 'E7', 'G7', 'C8']
    t1 = 0.56
    for i, n in enumerate(arp):
        add(out, square_soft(note_hz(n) / 2, 0.16, amp=0.22), t1 + i * 0.052)
        add(out, bell(note_hz(n), dur=0.5, amp=0.20, bright=0.5), t1 + i * 0.052)
    # 3) fanfare chord
    t2 = t1 + 7 * 0.052 + 0.01
    add(out, brass_chord([note_hz(n) for n in ('C4', 'G4', 'C5', 'E5', 'G5')], 1.75, amp=0.42), t2)
    add(out, bell(note_hz('C6'), dur=1.6, amp=0.35), t2)
    add(out, bell(note_hz('G6'), dur=1.6, amp=0.22), t2 + 0.01)
    # 4) payout bell ringing
    add(out, payout_bell(1.6, amp=0.12), t2 + 0.05)
    # 5) coin cascade
    tc = t2
    n_coins = 46
    times = tc + np.sort(rng.exponential(0.55, n_coins))
    for tt in times:
        if tt < dur - 0.2:
            add(out, coin(rng.uniform(0.18, 0.42)), tt)
    # master: gentle soft clip + normalize + fades
    out -= np.mean(out)
    out = np.tanh(out * 1.15) / np.tanh(1.15)
    out /= np.max(np.abs(out)); out *= 0.89
    fade = int(0.04 * SR); out[-fade:] *= np.linspace(1, 0, fade)
    return out.astype(np.float32)

if __name__ == '__main__':
    from pathlib import Path
    target = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/ludocraft/sounds/jackpot_777.ogg'
    target.parent.mkdir(parents=True, exist_ok=True)
    sf.write(target, jackpot(), SR, format='OGG', subtype='VORBIS')
    print(target)
