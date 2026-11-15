"""Build compact premium pickaxe heads, tapered hooked blades, UV textures and item JSON."""
from pathlib import Path
import json
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_pickaxe_model as base
from PIL import Image, ImageDraw

ASSETS = base.ASSETS
GRID = base.GRID
FACES = base.FACES
DISPLAY = base.DISPLAY
CREDIT = 'Original LudoCraft voxel pickaxe mesh; tools/build_premium_pickaxes.py'

VIP_PALETTE = {
    'shaft': '#171923', 'grip': '#292633', 'gold': '#d5a23a',
    'blade': '#353c49', 'edge': '#f2cf73', 'casing': '#151a22',
    'screen': '#063d2d', 'glow': '#52ffae',
}
HR_PALETTE = {
    'shaft': '#674594', 'grip': '#342448', 'gold': '#d8ad50',
    'blade': '#e8e4dc', 'edge': '#c9a24a', 'casing': '#f3f0e8',
    'drum': '#51257e', 'glow': '#e3a3ff', 'amethyst': '#7852a8',
}
EMISSIVE = {'screen', 'drum', 'glow'}

VIP_BOXES = [
    # Slim upright haft, with a dark wrapped grip and narrow gold ferrules.
    ('shaft', (7.35, 1.0, 7.25), (8.65, 12.1, 8.75)),
    ('grip', (7.05, 2.0, 7.0), (8.95, 8.9, 9.0)),
    ('gold', (6.9, 1.0, 6.9), (9.1, 1.55, 9.1)),
    ('gold', (6.9, 8.55, 6.9), (9.1, 9.05, 9.1)),
    ('gold', (7.0, 10.65, 7.0), (9.0, 11.05, 9.0)),
    # A small casino drum sits at the exact haft/head intersection.
    ('gold', (5.55, 11.15, 6.35), (10.45, 14.05, 9.65)),
    ('casing', (5.82, 11.42, 6.32), (10.18, 13.78, 9.68)),
    ('gold', (5.95, 13.72, 6.15), (10.05, 14.15, 9.85)),
    # Compact green 777 panels on both sides of the drum housing.
    ('screen', (6.15, 12.03, 6.02), (9.85, 13.48, 6.38)),
    ('screen', (6.15, 12.03, 9.62), (9.85, 13.48, 9.98)),
    ('glow', (5.96, 12.10, 6.05), (6.20, 12.36, 6.28)),
    ('glow', (9.80, 12.10, 6.05), (10.04, 12.36, 6.28)),
    ('glow', (5.96, 12.10, 9.72), (6.20, 12.36, 9.95)),
    ('glow', (9.80, 12.10, 9.72), (10.04, 12.36, 9.95)),
    # Narrow shoulders connect the drum to the hooked cutting wings.
    ('gold', (4.6, 11.7, 6.45), (6.1, 12.55, 9.55)),
]

HR_BOXES = [
    # Purple haft, wrapped grip and restrained gold hardware.
    ('shaft', (7.35, 0.9, 7.25), (8.65, 12.0, 8.75)),
    ('grip', (7.05, 2.0, 7.0), (8.95, 8.7, 9.0)),
    ('gold', (6.9, 0.9, 6.9), (9.1, 1.45, 9.1)),
    ('gold', (6.9, 8.4, 6.9), (9.1, 8.9, 9.1)),
    ('gold', (7.0, 10.55, 7.0), (9.0, 10.95, 9.0)),
    # Compact pearl casing; it is deliberately much narrower than the blades.
    ('gold', (5.65, 11.0, 6.35), (10.35, 14.0, 9.65)),
    ('casing', (5.92, 11.27, 6.32), (10.08, 13.73, 9.68)),
    # Three luminous rollers, grouped tightly inside the centered housing.
    ('drum', (6.08, 11.82, 6.02), (7.20, 13.30, 6.40)),
    ('drum', (7.44, 11.82, 6.02), (8.56, 13.30, 6.40)),
    ('drum', (8.80, 11.82, 6.02), (9.92, 13.30, 6.40)),
    ('drum', (6.08, 11.82, 9.60), (7.20, 13.30, 9.98)),
    ('drum', (7.44, 11.82, 9.60), (8.56, 13.30, 9.98)),
    ('drum', (8.80, 11.82, 9.60), (9.92, 13.30, 9.98)),
    # Crown integrated into the compact crown plate above the drum.
    ('gold', (6.45, 13.78, 6.6), (9.55, 14.22, 9.4)),
    ('gold', (6.4, 14.18, 6.65), (9.6, 14.48, 9.35)),
    ('gold', (6.55, 14.42, 7.0), (7.05, 15.25, 9.0)),
    ('gold', (7.75, 14.42, 7.0), (8.25, 15.62, 9.0)),
    ('gold', (8.95, 14.42, 7.0), (9.45, 15.25, 9.0)),
    ('glow', (7.82, 15.42, 7.15), (8.18, 15.72, 8.85)),
    # Small amethyst sockets and fine gold braces at the inner blade roots.
    ('amethyst', (4.7, 11.55, 6.18), (5.6, 12.55, 6.55)),
    ('amethyst', (4.7, 11.55, 9.45), (5.6, 12.55, 9.82)),
    ('gold', (4.6, 11.65, 6.5), (6.0, 12.4, 9.5)),
]

# Left-side stepped hook path: broad at the socket and progressively thinner
# toward its low, sharp tip. The right half is mirrored around model x=8.
BLADE_SEGMENTS = [
    (4.45, 11.70, 6.15, 12.65, 6.45, 9.55),
    (3.35, 10.60, 4.95, 12.05, 6.70, 9.30),
    (2.30, 9.15, 3.78, 10.95, 6.90, 9.10),
    (1.45, 7.80, 2.72, 9.63, 7.10, 8.90),
    (0.72, 6.50, 1.68, 8.28, 7.28, 8.72),
    (0.22, 5.70, 1.03, 6.98, 7.52, 8.48),
]


def add_symmetric_blades(boxes, body_material, edge_material):
    for x0, y0, x1, y1, z0, z1 in BLADE_SEGMENTS:
        for lo_x, hi_x in ((x0, x1), (16 - x1, 16 - x0)):
            boxes.append((body_material, (lo_x, y0, z0), (hi_x, y1, z1)))
            # Thin bright bevels along both visible broad faces, not thick caps.
            bevel = min(0.22, (z1 - z0) / 4)
            boxes.append((edge_material, (lo_x, y0, z0 - 0.01), (hi_x, y1, z0 + bevel)))
            boxes.append((edge_material, (lo_x, y0, z1 - bevel), (hi_x, y1, z1 + 0.01)))


def face_model(name, palette, boxes):
    base.MATERIALS = list(palette)
    base.COLORS = [palette[material] for material in base.MATERIALS]
    base.BOXES = boxes
    exposed = base.surface(base.voxels())
    elements = []
    for (face, plane, material), cells in sorted(exposed.items()):
        axis, sign = FACES[face]
        uv_axes = [i for i in range(3) if i != axis]
        for u, v, width, height in base.rectangles(cells):
            lo = [0.0, 0.0, 0.0]
            hi = [0.0, 0.0, 0.0]
            lo[axis] = (plane - (sign > 0)) / GRID
            hi[axis] = (plane + (sign < 0)) / GRID
            for i, start, size in zip(uv_axes, (u, v), (width, height)):
                lo[i], hi[i] = start / GRID, (start + size) / GRID
            uv_width = hi[uv_axes[0]] - lo[uv_axes[0]]
            uv_height = hi[uv_axes[1]] - lo[uv_axes[1]]
            element = {
                'from': lo,
                'to': hi,
                'faces': {
                    face: {
                        'uv': [0, 0, uv_width, uv_height],
                        'texture': f'#{material}',
                    }
                },
            }
            if material in EMISSIVE:
                element['shade'] = False
            elements.append(element)

    textures = {material: f'ludocraft:item/{name}_3d_{material}' for material in palette}
    textures['particle'] = f'ludocraft:item/{name}_3d'
    return {
        'credit': CREDIT,
        'ambientocclusion': False,
        'gui_light': 'side',
        'textures': textures,
        'display': DISPLAY,
        'elements': elements,
    }


def rgb(hex_color):
    hex_color = hex_color.lstrip('#')
    return tuple(int(hex_color[i:i + 2], 16) for i in (0, 2, 4))


def shade(color, factor):
    return tuple(max(0, min(255, round(channel * factor))) for channel in color)


def make_texture(material, color, name):
    image = Image.new('RGBA', (64, 64), (*rgb(color), 255))
    draw = ImageDraw.Draw(image)
    base_color = rgb(color)
    # Pixel-scale metal facets; the four-pixel/model-unit UV mapping stays crisp.
    for y in range(0, 64, 8):
        draw.line((0, y, 63, y), fill=(*shade(base_color, 1.12 if (y // 8) % 2 == 0 else .82), 255))
    for x in range(0, 64, 16):
        draw.line((x, 0, x, 63), fill=(*shade(base_color, 1.08), 255))
    if material in ('gold', 'edge'):
        draw.line((0, 2, 63, 2), fill=(255, 239, 176, 255), width=2)
        draw.line((0, 61, 63, 61), fill=(99, 62, 23, 255), width=2)
        for x in (8, 24, 40, 56):
            draw.point((x, 12), fill=(255, 240, 189, 255))
    elif material == 'grip':
        for y in range(-4, 68, 10):
            draw.line((0, y + 7, 63, y - 7), fill=(73, 54, 83, 255), width=3)
            draw.line((0, y + 10, 63, y - 4), fill=(18, 18, 27, 255), width=2)
    elif material == 'blade':
        draw.line((0, 0, 63, 0), fill=(255, 255, 255, 255), width=2)
        draw.line((0, 63, 63, 63), fill=(*shade(base_color, .48), 255), width=2)
        for y in (16, 32, 48):
            draw.line((4, y, 59, y), fill=(*shade(base_color, 1.3), 255), width=2)
    elif material == 'screen':
        draw.rectangle((0, 0, 63, 63), fill=(4, 48, 35, 255), outline=(83, 255, 178, 255), width=2)
        # Each 3x5 seven-segment numeral occupies exactly three pixels across.
        for start in (0, 5, 10):
            for dx, dy in ((0, 0), (1, 0), (2, 0), (2, 1), (2, 2), (2, 3), (2, 4)):
                draw.point((start + dx, dy), fill=(104, 255, 179, 255))
    elif material == 'drum':
        draw.rectangle((0, 0, 63, 63), fill=(46, 17, 75, 255), outline=(224, 166, 255, 255), width=2)
        # Slot symbol at the size used by the per-drum front-face UVs.
        for dx, dy in ((0, 0), (1, 0), (2, 0), (2, 1), (2, 2), (2, 3), (2, 4)):
            draw.point((dx, dy), fill=(255, 221, 255, 255))
        draw.line((0, 8, 15, 8), fill=(171, 88, 223, 255), width=2)
    elif material == 'glow':
        draw.rectangle((0, 0, 63, 63), fill=(*base_color, 255))
        for x in range(4, 64, 16):
            draw.line((x, 0, x, 63), fill=(255, 255, 255, 255), width=2)
        for y in range(4, 64, 16):
            draw.line((0, y, 63, y), fill=(255, 255, 255, 255), width=1)
    elif material == 'amethyst':
        draw.line((0, 4, 63, 4), fill=(215, 175, 255, 255), width=2)
        for y in (18, 38, 56):
            draw.line((5, y, 58, y - 8), fill=(112, 69, 157, 255), width=3)
    elif material == 'casing':
        draw.rectangle((0, 0, 63, 63), outline=(*shade(base_color, .66), 255), width=3)
        for y in (10, 54):
            draw.line((6, y, 57, y), fill=(*shade(base_color, .76), 255), width=2)
    elif material == 'shaft':
        for x in (8, 24, 40, 56):
            draw.line((x, 0, x, 63), fill=(*shade(base_color, .68), 255), width=2)
    # Particle sprite: a tiny swatch motif; gameplay-facing faces use the UV textures above.
    if material == 'screen':
        pass
    return image


def make_particle_icon(name, palette):
    image = Image.new('RGBA', (64, 64), (22, 22, 28, 255))
    draw = ImageDraw.Draw(image)
    colors = [rgb(value) for value in palette.values()]
    for i, color in enumerate(colors):
        x, y = (i % 4) * 16, (i // 4) * 16
        draw.rectangle((x, y, x + 15, y + 15), fill=(*color, 255))
    return image


def write_variant(name, palette, boxes):
    model = face_model(name, palette, boxes)
    model_path = ASSETS / 'models/item' / f'{name}.json'
    texture_dir = ASSETS / 'textures/item'
    texture_dir.mkdir(parents=True, exist_ok=True)
    model_path.write_text(json.dumps(model, indent=2) + '\n')
    make_particle_icon(name, palette).save(texture_dir / f'{name}_3d.png')
    for material, color in palette.items():
        make_texture(material, color, name).save(texture_dir / f'{name}_3d_{material}.png')
    print(f'{name}: {len(model["elements"])} exposed quads; {len(palette)} 64x64 UV textures')


if __name__ == '__main__':
    vip_boxes = list(VIP_BOXES)
    add_symmetric_blades(vip_boxes, 'blade', 'edge')
    write_variant('vip_gamblers_pickaxe', VIP_PALETTE, vip_boxes)

    highroller_boxes = list(HR_BOXES)
    add_symmetric_blades(highroller_boxes, 'blade', 'edge')
    write_variant('highroller_pickaxe', HR_PALETTE, highroller_boxes)
