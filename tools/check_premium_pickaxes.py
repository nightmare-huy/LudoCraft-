"""Regression checks for the generated VIP and Highroller Minecraft item meshes."""
import json
from pathlib import Path
from PIL import Image
import build_pickaxe_model as base
import build_premium_pickaxes as premium

VARIANTS = (
    ('vip_gamblers_pickaxe', premium.VIP_MATS, premium.VIP_COLORS, premium.VIP_BOXES, '777'),
    ('highroller_pickaxe', premium.HR_MATS, premium.HR_COLORS, premium.HR_BOXES, 'rollers'),
)


def check():
    for name, materials, colors, boxes, style in VARIANTS:
        base.MATERIALS, base.COLORS, base.BOXES = materials, colors, boxes
        expected = base.make_model()
        expected['credit'] = 'Original LudoCraft premium 3D pickaxe mesh; tools/build_premium_pickaxes.py'
        expected['textures']['surface'] = f'ludocraft:item/{name}_3d'
        expected['textures']['particle'] = f'ludocraft:item/{name}_3d'
        model_path = premium.ASSETS / 'models/item' / f'{name}.json'
        texture_path = premium.ASSETS / 'textures/item' / f'{name}_3d.png'
        model = json.loads(model_path.read_text())
        assert model == expected, f'{name}: stale model; regenerate with tools/build_premium_pickaxes.py'
        assert len(model['elements']) > 100
        assert model['display'] == premium.DISPLAY
        for element in model['elements']:
            assert set(element) == {'from', 'to', 'faces'}
            assert all(0 <= lo < hi <= 16 for lo, hi in zip(element['from'], element['to']))
            assert len(element['faces']) == 1
            face, spec = next(iter(element['faces'].items()))
            assert face in premium.FACES and spec['texture'] == '#surface'
            assert len(spec['uv']) == 4 and all(0 <= uv <= 16 for uv in spec['uv'])
        texture = Image.open(texture_path).convert('RGBA')
        assert texture.size == (64, 32)
        assert texture.getchannel('A').getextrema() == (255, 255)
        assert name in model['textures']['surface']
        print(f'PASS: {name}, {len(model["elements"])} exported mesh faces and opaque 64x32 atlas')


if __name__ == '__main__':
    check()
