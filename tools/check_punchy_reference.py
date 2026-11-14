"""Check the shipped item and Punchy transforms against captured vanilla diamond-pickaxe defaults."""
import json
from pathlib import Path

model = json.loads(Path('src/main/resources/assets/ludocraft/models/item/gamblers_pickaxe.json').read_text())
compat = json.loads(Path('src/main/resources/assets/minecraft/punchy/compat/ludocraft_gamblers_pickaxe.json').read_text())
expected_display = {
    'right': {'rotation': [0, -90, 25], 'translation': [-2, 2.7, 1.13], 'scale': [.51, .51, .51]},
    'left': {'rotation': [0, 90, -25], 'translation': [-2, 2.7, 1.13], 'scale': [.51, .51, .51]},
}
for side, reference in expected_display.items():
    actual = model['display'][f'firstperson_{side}hand']
    assert actual == reference, f'{side} transform must keep the vanilla diamond angle/grip, at a compact 75% display size: {actual}'
for side, pose in compat['itemSpecific']['ludocraft:gamblers_pickaxe']['transforms'].items():
    assert pose['scale'] == 1
    assert all(pose[field] == 0 for field in ('offX','offY','offZ','rotX','rotY','rotZ','pivotX','pivotY','pivotZ'))
print('PASS: first-person orientation and grip match diamond pickaxe; LudoCraft mesh is 75% size')
