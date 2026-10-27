from pathlib import Path
import re, zipfile, sys
root=Path(__file__).resolve().parents[2]
archive=Path(sys.argv[1])
route_annotations=re.compile(r'@(Get|Post|Put|Patch|Delete)Mapping\s*\(\s*"([^"]*)"\s*\)')
base_annotation=re.compile(r'@RequestMapping\s*\(\s*"([^"]*)"\s*\)')
def extract(java):
    m=base_annotation.search(java)
    if not m:return set()
    return {(method.upper(), m.group(1).rstrip('/')+'/'+route.lstrip('/')) for method,route in route_annotations.findall(java)}
with zipfile.ZipFile(archive) as z:
    for f in z.namelist():
        if not f.endswith('Controller.java') or '/src/main/java/' not in f:continue
        new=root/f.replace('cinema-system/','',1)
        original=extract(z.read(f).decode())
        patched=extract(new.read_text())
        missing=original-patched
        assert not missing, f'Existing routes removed from {new}: {missing}'
        assert all(route.startswith('/api/v1/') for method,route in patched), f'Invalid API prefix: {patched}'
        print(f'{new.name}: {len(original)} original routes retained; {len(patched-original)} new')
print('PASS original API route contract preserved (route method annotations unchanged)')
