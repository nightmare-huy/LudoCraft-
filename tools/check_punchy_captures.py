"""Basic actual-frame guard, not a replacement for looking at the rendered grip.
Exclude the hotbar. Check the large cyan head components, not the tiny pommel.
"""
from pathlib import Path
import sys
from PIL import Image

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'run/punchy-smoke/screenshots')
for name in ('after-first-right', 'after-first-left', 'looking-down', 'mining-recovered'):
    image = Image.open(root / f'punchy-{name}.png').convert('RGB')
    w,h = image.size
    pixels = set()
    for y in range(80,h):
        for x in range(w):
            if 290 <= x <= 665 and y >= 450: continue
            r,g,b = image.getpixel((x,y))
            if g > 70 and g > r*1.6 and b > r*1.5: pixels.add((x,y))
    large = []
    while pixels:
        start = pixels.pop(); component = [start]; todo = [start]
        while todo:
            x,y = todo.pop()
            for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)):
                p=x+dx,y+dy
                if p in pixels: pixels.remove(p); todo.append(p); component.append(p)
        if len(component) > 40: large.extend(component)
    assert len(large) > 250, f'{name}: held head missing'
    bounds = min(x for x,y in large), min(y for x,y in large), max(x for x,y in large), max(y for x,y in large)
    assert bounds[0] > 10 and bounds[2] < w-10 and bounds[3] < h-4, f'{name}: head hits image edge: {bounds}'
    print(f'PASS Punchy idle frame {name}: diamond head bounds {bounds}')
# Mining poses may legitimately leave the frame; require all sampled frames plus recovery.
for name in ('mining-start', 'mining-middle', 'mining-end', 'after-first-swing',
             'after-third-front', 'after-third-side', 'after-third-left'):
    assert Image.open(root/f'punchy-{name}.png').size == (960,540)
print('PASS: eleven actual Punchy frames; idle head clearance, mining and recovery evidence present')
