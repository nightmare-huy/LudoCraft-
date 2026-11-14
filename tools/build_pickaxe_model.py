"""Build an original, closed voxel-union pickaxe as vanilla 1.21.1 item JSON.
No OBJ loader/GeckoLib; only exposed faces are exported, so overlapping design
boxes cannot create internal faces or coplanar z-fighting. Requires Pillow.
"""
from pathlib import Path
from collections import defaultdict
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/ludocraft'
MODEL = ASSETS / 'models/item/gamblers_pickaxe.json'
TEXTURE = ASSETS / 'textures/item/gamblers_pickaxe_3d.png'
GRID = 4  # quarter-pixel design grid, Minecraft model units
# Face normal axis, sign; UV axes are the two other axes in ascending order.
FACES = {'west': (0, -1), 'east': (0, 1), 'down': (1, -1),
         'up': (1, 1), 'north': (2, -1), 'south': (2, 1)}
MATERIALS = ['shaft', 'grip', 'gold', 'gold_light', 'diamond', 'diamond_light', 'badge', 'dark_gold']
COLORS = ['#292035', '#71324c', '#ce9437', '#ffdc82', '#25b7c5', '#a0f9e9', '#201b2b', '#805329']
# Model-space cuboids; the later surface material wins wherever boxes overlap.
BOXES = [
    ('shaft', (7.25, 1.25, 7.25), (8.75, 12.5, 8.75)),
    ('grip', (7, 2, 7), (9, 8.75, 9)),
    ('gold', (6.75, .5, 6.75), (9.25, 2, 9.25)),
    ('gold_light', (7, .5, 7), (9, .75, 9)),
    ('diamond', (7.5, 1, 6.5), (8.5, 1.5, 6.75)),
    ('diamond', (7.5, 1, 9.25), (8.5, 1.5, 9.5)),
    ('gold', (6.75, 3, 6.75), (9.25, 3.5, 9.25)),
    ('gold', (6.75, 6, 6.75), (9.25, 6.5, 9.25)),
    ('gold_light', (6.75, 8.25, 6.75), (9.25, 8.75, 9.25)),
    ('gold', (7, 10.25, 7), (9, 11.75, 9)),
    ('gold', (3, 13, 7), (13, 14.5, 9)),
    ('gold_light', (4, 14.25, 7.25), (12, 14.75, 8.75)),
    ('diamond', (2, 11.75, 7), (6.25, 13.5, 9)),
    ('diamond', (1, 10.5, 7.25), (3, 12.5, 8.75)),
    ('diamond_light', (.5, 10, 7.5), (1.5, 11.5, 8.5)),
    ('diamond_light', (2, 13.25, 7), (5.75, 13.75, 9)),
    ('diamond', (9.75, 11.75, 7), (13.5, 13.5, 9)),
    ('diamond', (12.75, 10.5, 7), (14.5, 12.5, 9)),
    ('diamond', (13.75, 9, 7.25), (15, 11, 8.75)),
    ('diamond_light', (14.5, 8.5, 7.5), (15.5, 10, 8.5)),
    ('diamond_light', (10.25, 13.25, 7), (13.25, 13.75, 9)),
    ('dark_gold', (6, 11.25, 6.5), (10, 14.5, 9.5)),
    ('gold', (6.25, 11.5, 6.25), (9.75, 14.25, 9.75)),
    ('gold_light', (6.25, 14, 6.25), (9.75, 14.5, 9.75)),
    ('badge', (6.5, 11.75, 6), (9.5, 13.75, 6.25)),
    ('badge', (6.5, 11.75, 9.75), (9.5, 13.75, 10)),
]
# Upright 3D mesh, not a diagonal generated sprite. Minecraft mirrors left-hand Y/Z itself.
DISPLAY = {
    'gui': {'rotation': [15, -35, -35], 'translation': [0, 0, 0], 'scale': [.75, .75, .75]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
    'fixed': {'rotation': [0, 180, -40], 'translation': [0, 0, 0], 'scale': [.7, .7, .7]},
    'head': {'rotation': [0, 0, 0], 'translation': [0, 13, 0], 'scale': [.8, .8, .8]},
    'thirdperson_righthand': {'rotation': [45, 90, 0], 'translation': [0, -.85, 1.5], 'scale': [.65, .65, .65]},
    'thirdperson_lefthand': {'rotation': [45, 90, 0], 'translation': [0, -.85, 1.5], 'scale': [.65, .65, .65]},
    # Keep the Punchy diamond-pickaxe angle/depth, nudged horizontally inward to center the wider 3D head.
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [0, 3.2, 1.13], 'scale': [.51, .51, .51]},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [0, 3.2, 1.13], 'scale': [.51, .51, .51]},
}

def voxels():
    occupied = {}
    for material, lo, hi in BOXES:
        low = [round(v * GRID) for v in lo]
        high = [round(v * GRID) for v in hi]
        for x in range(low[0], high[0]):
            for y in range(low[1], high[1]):
                for z in range(low[2], high[2]):
                    occupied[x, y, z] = material
    return occupied

def surface(occupied):
    """Unit-square surface keyed by (face, plane, material)."""
    planes = defaultdict(set)
    for pos, material in occupied.items():
        for face, (axis, sign) in FACES.items():
            neighbor = list(pos); neighbor[axis] += sign
            if tuple(neighbor) in occupied:
                continue
            uv_axes = [i for i in range(3) if i != axis]
            mat = 'dark_gold' if material == 'badge' and axis != 2 else material
            planes[face, pos[axis] + (sign > 0), mat].add(tuple(pos[i] for i in uv_axes))
    return planes

def rectangles(cells):
    cells = set(cells)
    while cells:
        u, v = min(cells)
        width = 1
        while (u + width, v) in cells: width += 1
        height = 1
        while all((x, v + height) in cells for x in range(u, u + width)): height += 1
        cells.difference_update((x, y) for x in range(u, u + width) for y in range(v, v + height))
        yield u, v, width, height

def tile_uv(material):
    index = MATERIALS.index(material)
    # Texture is 64x32; model UV coordinates always span 0..16, independently per axis.
    return [(index % 4) * 4, (index // 4) * 8, (index % 4 + 1) * 4, (index // 4 + 1) * 8]

def make_model():
    elements = []
    for (face, plane, material), cells in sorted(surface(voxels()).items()):
        axis, sign = FACES[face]
        axes = [i for i in range(3) if i != axis]
        for u, v, width, height in rectangles(cells):
            lo, hi = [0., 0., 0.], [0., 0., 0.]
            lo[axis], hi[axis] = (plane - (sign > 0)) / GRID, (plane + (sign < 0)) / GRID
            for i, a, size in zip(axes, (u, v), (width, height)):
                lo[i], hi[i] = a / GRID, (a + size) / GRID
            elements.append({'from': lo, 'to': hi, 'faces': {
                face: {'uv': tile_uv(material), 'texture': '#surface'}
            }})
    return {'credit': 'Original LudoCraft 3D pickaxe; tools/build_pickaxe_model.py',
            'ambientocclusion': False, 'gui_light': 'side',
            'textures': {'surface': 'ludocraft:item/gamblers_pickaxe_3d',
                         'particle': 'ludocraft:item/gamblers_pickaxe_3d'},
            'display': DISPLAY, 'elements': elements}

def make_texture():
    image = Image.new('RGBA', (64, 32))
    draw = ImageDraw.Draw(image)
    for i, (mat, color) in enumerate(zip(MATERIALS, COLORS)):
        x, y = (i % 4) * 16, (i // 4) * 16
        draw.rectangle((x,y,x+15,y+15), fill=color)
        if mat == 'badge':
            draw.rectangle((x,y,x+15,y+15), fill='#241b30', outline='#e9b84c')
            draw.line((x+2,y+2,x+13,y+2), fill='#fff1ac')
            for dx in (2,6,10):
                for dy, row in enumerate(('111','001','001','010','010','010')):
                    for px, bit in enumerate(row):
                        if bit == '1': draw.point((x+dx+px,y+5+dy), fill='#ffdf83')
            draw.line((x+3,y+13,x+12,y+13), fill='#be345d')
        elif mat in ('diamond', 'diamond_light'):
            draw.line((x,y,x+15,y), fill='#ccfff4')
            draw.line((x+1,y+1,x+1,y+14), fill='#77e6de')
            draw.line((x+3,y+12,x+11,y+4), fill='#57d6d4', width=2)
        elif mat in ('gold', 'gold_light', 'dark_gold'):
            draw.line((x,y,x+15,y), fill='#fff0b2')
            draw.line((x,y+15,x+15,y+15), fill='#99602c')
            draw.point((x+3,y+3), fill='#ffe4a1')
        elif mat == 'grip':
            for yy in range(1,16,4):
                draw.line((x,y+yy,x+15,y+max(0,yy-2)), fill='#b05871')
                draw.line((x,y+min(15,yy+1),x+15,y+max(0,yy-1)), fill='#492236')
        else:
            for xx in (3,8,13): draw.line((x+xx,y,x+xx,y+15), fill='#453048')
    return image

if __name__ == '__main__':
    MODEL.write_text(json.dumps(make_model(), indent=2) + '\n')
    make_texture().save(TEXTURE)
    print(f'Wrote {len(make_model()["elements"])} exposed quads, texture 64x32; no external model loader.')
