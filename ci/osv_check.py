"""Checks every resolved Maven dependency against the OSV vulnerability database (osv.dev)."""
import json, re, sys, urllib.request

deps = set()
for line in open(sys.argv[1], encoding='utf-8'):
    m = re.search(r'([\w.\-]+):([\w.\-]+):([\w.\-]+)(?: -> ([\w.\-]+))?', line)
    if m and '---' in line:
        g, a, v, override = m.groups()
        deps.add((g, a, override or v))
deps = sorted(deps)
print(f'Checking {len(deps)} dependencies against osv.dev ...')
queries = [{'package': {'ecosystem': 'Maven', 'name': f'{g}:{a}'}, 'version': v} for g, a, v in deps]
found = []
for i in range(0, len(queries), 500):
    req = urllib.request.Request('https://api.osv.dev/v1/querybatch', data=json.dumps({'queries': queries[i:i+500]}).encode(), headers={'Content-Type': 'application/json'})
    res = json.load(urllib.request.urlopen(req, timeout=60))
    for q, r in zip(queries[i:i+500], res.get('results', [])):
        for v in r.get('vulns', []) or []:
            found.append((q['package']['name'], q['version'], v['id']))
if found:
    print(f'{len(found)} known vulnerabilities:')
    for n, v, vid in found: print(f'  {n}:{v}  {vid}  https://osv.dev/vulnerability/{vid}')
else:
    print('No known vulnerabilities found in resolved release dependencies.')
