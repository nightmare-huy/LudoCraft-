"""Reject blank/incorrect real-client captures; visual review remains mandatory.
The controlled scene has no cyan blocks, mobs or equipment besides the pickaxe.
Exclude the hotbar so its item icon cannot make an empty world capture pass.
"""
from pathlib import Path
import sys
from PIL import Image

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'run/model-smoke/screenshots')
for name in ('before-first-right', 'after-first-right', 'after-first-left',
             'after-third-front', 'after-third-side', 'after-third-left', 'after-first-swing'):
    image = Image.open(root / f'hand-{name}.png').convert('RGB')
    w, h = image.size
    assert w >= 900 and h >= 500
    points = [(x, y) for y in range(80, h-50) for x in range(w)
              if (lambda r,g,b: g > 70 and g > r*1.6 and b > r*1.5)(*image.getpixel((x,y)))]
    assert len(points) >= (20 if 'third' in name else 500), f'{name}: no visible pickaxe in the world (not just hotbar)'
    bounds = min(x for x,y in points), min(y for x,y in points), max(x for x,y in points), max(y for x,y in points)
    if name in ('after-first-left', 'after-first-right'):
        assert bounds[0] > 20 and bounds[2] < w-20, f'{name}: diamond head clips horizontal screen edge'
    print(f'PASS capture {name}: {len(points)} diamond-colored pixels outside HUD; bounds {bounds}')
