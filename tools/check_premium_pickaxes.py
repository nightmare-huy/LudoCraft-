"""Regression checks for the refined hooked pickaxe silhouettes and per-face UVs."""
from collections import deque
import json
from pathlib import Path
from PIL import Image, ImageChops
import build_pickaxe_model as base
import build_premium_pickaxes as premium

VARIANTS = (
    ('vip_gamblers_pickaxe', premium.VIP_PALETTE, premium.VIP_BOXES, 'blade'),
    ('highroller_pickaxe', premium.HR_PALETTE, premium.HR_BOXES, 'blade'),
)


def is_connected(voxels):
    start = next(iter(voxels))
    visited = {start}
    queue = deque([start])
    while queue:
        point = queue.popleft()
        for axis, sign in base.FACES.values():
            adjacent = list(point)
            adjacent[axis] += sign
            adjacent = tuple(adjacent)
            if adjacent in voxels and adjacent not in visited:
                visited.add(adjacent)
                queue.append(adjacent)
    return visited == set(voxels)


def expected_boxes(name, original_boxes, blade_material):
    boxes = list(original_boxes)
    premium.add_symmetric_blades(boxes, blade_material, 'edge')
    return boxes


def check():
    widths = [x1 - x0 for x0, _, x1, _, _, _ in premium.BLADE_SEGMENTS]
    depths = [z1 - z0 for _, _, _, _, z0, z1 in premium.BLADE_SEGMENTS]
    heights = [(y0 + y1) / 2 for _, y0, _, y1, _, _ in premium.BLADE_SEGMENTS]
    assert all(a > b for a, b in zip(widths, widths[1:])), 'Blade steps must taper toward the tips'
    assert all(a > b for a, b in zip(depths, depths[1:])), 'Blade thickness must taper toward the tips'
    assert all(a > b for a, b in zip(heights, heights[1:])), 'Both blades must hook downward'
    assert premium.BLADE_SEGMENTS[-1][1] < 7, 'Blade tips should extend below the centered head'
    for name, palette, original_boxes, blade_material in VARIANTS:
        boxes = expected_boxes(name, original_boxes, blade_material)
        model = json.loads((premium.ASSETS / 'models/item' / f'{name}.json').read_text())
        expected = premium.face_model(name, palette, boxes)
        assert model == expected, f'{name}: stale model; regenerate with tools/build_premium_pickaxes.py'
        assert model['display'] == premium.DISPLAY, f'{name}: held-item pose changed'
        assert 100 < len(model['elements']) < 500
        base.MATERIALS, base.COLORS, base.BOXES = list(palette), list(palette.values()), boxes
        assert is_connected(base.voxels()), f'{name}: disconnected floating geometry'

        directions = set()
        for element in model['elements']:
            assert set(element) <= {'from', 'to', 'faces', 'shade'}
            assert {'from', 'to', 'faces'} <= set(element)
            assert all(0 <= lo < hi <= 16 for lo, hi in zip(element['from'], element['to']))
            assert len(element['faces']) == 1
            face, spec = next(iter(element['faces'].items()))
            assert face in premium.FACES
            material = spec['texture'].removeprefix('#')
            assert material in palette
            assert len(spec['uv']) == 4 and all(0 <= uv <= 16 for uv in spec['uv'])
            axis, _ = premium.FACES[face]
            uv_axes = [i for i in range(3) if i != axis]
            u_size = element['to'][uv_axes[0]] - element['from'][uv_axes[0]]
            v_size = element['to'][uv_axes[1]] - element['from'][uv_axes[1]]
            uv = spec['uv']
            assert abs((uv[2] - uv[0]) - u_size) < 1e-6, f'{name}: stretched U on {face}'
            assert abs((uv[3] - uv[1]) - v_size) < 1e-6, f'{name}: stretched V on {face}'
            assert element.get('shade', True) == (material not in premium.EMISSIVE), f'{name}: emissive shading flag mismatch'
            directions.add(face)
        assert directions == set(premium.FACES), f'{name}: incomplete six-sided mesh'

        for material in palette:
            texture_path = premium.ASSETS / 'textures/item' / f'{name}_3d_{material}.png'
            texture = Image.open(texture_path).convert('RGBA')
            assert texture.size == (64, 64)
            assert texture.getchannel('A').getextrema() == (255, 255)
            expected_texture = premium.make_texture(material, palette[material], name)
            assert ImageChops.difference(texture, expected_texture).getbbox() is None, f'{name}: stale texture {material}'
        icon_path = premium.ASSETS / 'textures/item' / f'{name}_3d.png'
        icon = Image.open(icon_path).convert('RGBA')
        assert icon.size == (64, 64)
        assert ImageChops.difference(icon, premium.make_particle_icon(name, palette)).getbbox() is None
        for texture in model['textures'].values():
            location = texture.removeprefix('ludocraft:item/')
            assert (premium.ASSETS / 'textures/item' / f'{location}.png').is_file()
        print(f'PASS: {name}, {len(model["elements"])} closed-surface quads, connected voxel body, proportional UVs and shade:false emissive faces')


if __name__ == '__main__':
    check()
