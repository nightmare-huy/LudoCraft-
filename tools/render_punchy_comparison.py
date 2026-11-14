"""Caption actual Punchy 3.2b test frames. No synthetic/retouched gameplay."""
from pathlib import Path
import os, sys
from PIL import Image, ImageDraw, ImageFont
version = sys.argv[1] if len(sys.argv) > 1 else '1.5.3'
root = Path(__file__).resolve().parents[1] / f'docs/previews/v{version}'
font = os.environ.get('LUDKA_PREVIEW_FONT', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
f = ImageFont.truetype(font,23); small = ImageFont.truetype(font,20)
sheet = Image.new('RGB',(1920,1900),'#151c27'); d=ImageDraw.Draw(sheet)
d.text((24,16),f'LudoCraft {version} · Совместимость с Punchy 3.2b · NeoForge 1.21.1',font=f,fill='#f3d591')
d.text((24,52),'Настоящий Minecraft с установленным Punchy, стандартные настройки · не макеты',font=small,fill='#c6d2e3')
if version == '1.5.3':
    before='punchy-before/punchy-after-first-right.png'
    before_label='До — LudoCraft 1.5.2 + Punchy 3.2b'
else:
    current = tuple(int(part) for part in version.split('.'))
    previous = []
    for capture in root.parent.glob('v*/punchy/punchy-after-first-right.png'):
        candidate = capture.parent.parent.name.removeprefix('v')
        try:
            candidate_version = tuple(int(part) for part in candidate.split('.'))
        except ValueError:
            continue
        if candidate_version < current:
            previous.append((candidate_version, candidate))
    if not previous:
        raise FileNotFoundError(f'No earlier Punchy capture exists for comparison with {version}')
    _, previous_version = max(previous)
    before=f'../v{previous_version}/punchy/punchy-after-first-right.png'
    before_label=f'До — LudoCraft {previous_version} + Punchy 3.2b'
frames=[(before,before_label),
        ('punchy/punchy-after-first-right.png','После — поза алмазной кирки'),
        ('punchy/punchy-after-first-left.png','Левая рука'),
        ('punchy/punchy-looking-down.png','Взгляд вниз перед добычей'),
        ('punchy/punchy-mining-middle.png','Добыча камня в выживании — нажатие атаки'),
        ('punchy/punchy-mining-recovered.png','После добычи — возврат в обычное положение')]
for i,(file,title) in enumerate(frames):
    x=i%2*960;y=95+i//2*590
    d.text((x+16,y),title,font=f,fill='white');image=Image.open(root/file).convert('RGB');assert image.size==(960,540)
    sheet.paste(image,(x,y+38))
d.text((24,1870),f'Исходные кадры не ретушированы. До: предыдущая версия; после: выживание с каменной мишенью.',font=small,fill='#c6d2e3')
sheet.save(root/'punchy-comparison.png')
