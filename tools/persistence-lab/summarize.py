"""Produce auditable compact results from raw, ignored lab evidence."""
from pathlib import Path
import argparse, hashlib, json
from inspect_nbt import load, find

repo=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--lab-root',type=Path,default=repo/'run/persistence-lab')
args=parser.parse_args()
lab=args.lab_root.resolve()
evidence=lab/'evidence'
summary={'baseline':{},'runs':[]}
for p in (lab/'server/mods').glob('*.jar'):
    summary['baseline'][p.name]={'size':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
for p in sorted(evidence.glob('*-result.json')):
    r=json.loads(p.read_text())
    log=evidence/(r['label']+'.log')
    lines=log.read_text(encoding='utf-8').splitlines()
    r['shutdown_error_in_log']=any('Exception stopping the server' in s for s in lines)
    r['assertions']=[s[s.index('PERSISTENCE_PROBE ')+len('PERSISTENCE_PROBE '):] for s in lines if 'PERSISTENCE_PROBE ' in s]
    r['log_sha256']=hashlib.sha256(log.read_bytes()).hexdigest()
    cap=evidence/(r['label']+'-data/data/capabilities.dat')
    if cap.exists():
        r['capabilities_sha256']=hashlib.sha256(cap.read_bytes()).hexdigest()
        r['colonies']=[]
        for path,cols in find(load(cap)):
            for c in cols:
                r['colonies'].append({'path':path,'name':c.get('name'),'building_count':len(c['buildingManager']['buildings']),
                    'research_fields':{k:v for k,v in c.items() if 'research' in k.lower()}})
    summary['runs'].append(r)
out=evidence/'summary.json'
out.write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
print(out)
for r in summary['runs']: print(r['label'],r['probe_ready'],r['save_command_completed'],r['colonies'])
