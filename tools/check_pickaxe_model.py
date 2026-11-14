"""Structural/texture/pose regression tests of the ACTUAL exported Minecraft JSON."""
import json
import math
from collections import deque
from itertools import product
from PIL import Image, ImageChops
from build_pickaxe_model import MODEL, TEXTURE, ASSETS, GRID, FACES, DISPLAY, voxels, surface, make_model, make_texture


def rotate(point, angles):
    # JOML rotationXYZ: R_x R_y R_z (applied to the vector right-to-left).
    x, y, z = point
    a, b, c = (math.radians(v) for v in angles)
    x, y = x*math.cos(c)-y*math.sin(c), x*math.sin(c)+y*math.cos(c)
    x, z = x*math.cos(b)+z*math.sin(b), -x*math.sin(b)+z*math.cos(b)
    y, z = y*math.cos(a)-z*math.sin(a), y*math.sin(a)+z*math.cos(a)
    return x, y, z


def check():
    model = json.loads(MODEL.read_text())
    assert model == make_model(), 'Export is stale; regenerate with build_pickaxe_model.py'
    assert 'parent' not in model and 'loader' not in model, 'Do not inherit item/generated or an external loader'
    assert model['gui_light'] == 'side' and not model['ambientocclusion']
    assert 12 < len(model['elements']) <= 256, 'Unexpected geometry or excessive quad count'
    for location in model['textures'].values():
        ns, path = location.split(':')
        file = ASSETS.parent/ns/'textures'/(path+'.png')
        assert file.is_file(), f'Missing sprite {location}'
    texture = Image.open(TEXTURE).convert('RGBA')
    assert texture.size == (64, 32)
    assert texture.getchannel('A').getextrema() == (255, 255), 'Unintended transparent holes'
    assert ImageChops.difference(texture.convert('RGB'), make_texture().convert('RGB')).getbbox() is None, 'Texture export is stale'
    occupied = voxels()
    reached = {next(iter(occupied))}; queue = deque(reached)
    while queue:
        p = queue.popleft()
        for axis, sign in FACES.values():
            q = list(p); q[axis] += sign; q = tuple(q)
            if q in occupied and q not in reached: reached.add(q); queue.append(q)
    assert reached == occupied.keys(), 'Floating/disconnected model pieces'
    expected = {(face, plane, u, v) for (face, plane, _), cells in surface(occupied).items() for u,v in cells}
    actual = set()
    normals = set()
    for element in model['elements']:
        assert set(element) == {'from', 'to', 'faces'}
        lo, hi = element['from'], element['to']
        assert all(math.isfinite(a) and 0 <= a < b <= 16 for a,b in zip(lo,hi)), 'Invalid or zero-size element'
        assert len(element['faces']) == 1
        face, spec = next(iter(element['faces'].items())); normals.add(face)
        assert face in FACES and spec['texture'] == '#surface'
        assert set(spec) == {'uv', 'texture'}, 'No block-only cullface, tint index or unsupported rotation'
        u0,v0,u1,v1 = spec['uv']
        assert 0 <= u0 < u1 <= 16 and 0 <= v0 < v1 <= 16
        axis, sign = FACES[face]; axes = [i for i in range(3) if i != axis]
        plane = round((hi[axis] if sign > 0 else lo[axis]) * GRID)
        for u in range(round(lo[axes[0]]*GRID), round(hi[axes[0]]*GRID)):
            for v in range(round(lo[axes[1]]*GRID), round(hi[axes[1]]*GRID)):
                key = face, plane, u, v
                assert key not in actual, 'Overlapping coplanar faces (z-fighting)'
                actual.add(key)
    assert actual == expected, 'Missing exposed faces or buried/internal geometry'
    assert normals == FACES.keys(), 'Missing a side of the model'
    assert model['display'].keys() == DISPLAY.keys()
    for context, transform in model['display'].items():
        assert all(math.isfinite(v) for vec in transform.values() for v in vec)
        assert all(0 < s <= 1 for s in transform['scale'])
        assert all(abs(t) <= 16 for t in transform['translation'])
        if context in ('gui', 'fixed'):
            points = [rotate(tuple((c[i]-8)*transform['scale'][i] for i in range(3)), transform['rotation'])
                      for e in model['elements'] for c in product(*zip(e['from'], e['to']))]
            assert all(-8 <= p[i] + transform['translation'][i] <= 8 for p in points for i in (0,1)), f'{context} clips a 16px icon/frame'
    print(f'PASS: pickaxe JSON, {len(model["elements"])} quads, all 6 face directions, connected volume, exact closed surface, no overlapping faces, UV/texture and 8 poses')
    return model

if __name__ == '__main__': check()
