"""Fetch only the exact, hash-pinned user-reported Punchy build for isolated CI.
Never bundle it with LudoCraft or commit the downloaded JAR.
Metadata: https://api.modrinth.com/v2/version/aVZEe0j8
"""
from pathlib import Path
import hashlib
import urllib.request
import zipfile
import subprocess

url = 'https://cdn.modrinth.com/data/8aoMKplv/versions/aVZEe0j8/punchy-3.2b-neoforge-1.21.1.jar'
sha512 = '6f8426512d5ba99e356a151f4c73f85e6d287e6e2c19756b88037f7cf443e758cf2cbfb7964ba8e00ccd3ecb5826b572302e20bf1236e7a6d1e9676d541feb3d'
jar = Path('run/punchy-smoke/mods/punchy-3.2b-neoforge-1.21.1.jar')
jar.parent.mkdir(parents=True, exist_ok=True)
request = urllib.request.Request(url, headers={'User-Agent': 'LudoCraft-compatibility-test/1.0 (github.com/nightmare-huy/LudoCraft-)'})
with urllib.request.urlopen(request, timeout=90) as response:
    data = response.read()
assert len(data) == 4523878 and hashlib.sha512(data).hexdigest() == sha512, 'Punchy download hash mismatch'
jar.write_bytes(data)
print('Verified Punchy 3.2b NeoForge 1.21.1 SHA-512:', sha512)
# Small diagnostics: identifiers/schema only, not a redistributed mod or decompiled implementation.
report = [f'URL: {url}', f'SHA512: {sha512}']
with zipfile.ZipFile(jar) as z:
    report.append(z.read('META-INF/neoforge.mods.toml').decode())
    report += [n for n in z.namelist() if any(k in n.lower() for k in ('tuning', 'compat', 'itemrender', 'toolkind'))]
Path('run/punchy-inspection.txt').write_text('\n'.join(report))

# Development-only candidate packs; NOT part of the mod and NOT enabled by default.
import json
for angle in (f"{axis}{degrees}" for axis in ("x", "y") for degrees in (-90, -45, 0, 45, 90)):
    root = Path(f'run/punchy-smoke/resourcepacks/ludka-punchy-candidate-{angle}')
    target = root/'assets/minecraft/punchy/compat/ludocraft_candidate.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    (root/'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 34, 'description': 'Dev-only Punchy pose probe'}}))
    target.write_text(json.dumps({'itemSpecific': {'ludocraft:gamblers_pickaxe': {
        'kind': 'PICKAXE', 'transforms': {'right': {'scale': 1}, 'left': {'scale': 1}}
    }}}))
# Public profile signatures help confirm the configuration API of the exact pinned build.
with Path('run/punchy-inspection.txt').open('a') as out:
    for cls in ('punchy.client.render.ToolTuning', 'punchy.client.render.ToolTuning$Profile',
                'punchy.client.render.ToolTuning$TransformOverride'):
        subprocess.run(['javap', '-p', '-classpath', str(jar), cls], stdout=out, check=True)
