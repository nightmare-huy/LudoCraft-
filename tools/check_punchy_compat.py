"""Scope/schema regression for the optional resource-only Punchy integration."""
import json
import math
from pathlib import Path

RESOURCE = Path('src/main/resources/assets/minecraft/punchy/compat/ludocraft_gamblers_pickaxe.json')
data = json.loads(RESOURCE.read_text())
assert set(data) == {'itemSpecific'}, 'Must not alter global arms, animations, or other items'
assert set(data['itemSpecific']) == {'ludocraft:gamblers_pickaxe'}
entry = data['itemSpecific']['ludocraft:gamblers_pickaxe']
assert set(entry) == {'kind', 'transforms'} and entry['kind'] == 'PICKAXE'
assert set(entry['transforms']) == {'right', 'left'}
fields = {'scale', 'offX', 'offY', 'offZ', 'rotX', 'rotY', 'rotZ', 'pivotX', 'pivotY', 'pivotZ'}
for pose in entry['transforms'].values():
    assert set(pose) == fields, 'Full pose required: omitted scale can hide the item in Punchy 3.2b'
    assert all(isinstance(x, (float, int)) and math.isfinite(x) for x in pose.values())
    assert 0 < pose['scale'] <= 1.5
right, left = entry['transforms']['right'], entry['transforms']['left']
assert right['rotX'] == left['rotX']
assert all(right[f] == -left[f] for f in ('rotY', 'rotZ', 'offX', 'pivotX'))
print('PASS: Punchy compatibility is item-only, both complete finite poses, no arm/global/animation changes')
