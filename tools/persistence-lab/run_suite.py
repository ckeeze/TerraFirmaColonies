"""Continue from seeded baseline; fail on any unexpected result. Never run concurrently."""
from pathlib import Path
import argparse, subprocess, sys

here=Path(__file__).resolve().parent
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--expected',choices=['broken','fixed'],default='broken')
parser.add_argument('--lab-root',type=Path)
parser.add_argument('--baseline-server',type=Path,required=True,help='Prepared Forge 1.20.1 server to copy read-only')
parser.add_argument('--forge-version',help='Installed Forge version to select when the baseline has multiple versions')
parser.add_argument('--mod-jar',type=Path)
args=parser.parse_args()
common=['--expected',args.expected]
for key in ['lab_root','baseline_server','mod_jar','forge_version']:
    value=getattr(args,key)
    if value is not None: common+=['--'+key.replace('_','-'),str(value)]
steps=([('seed','fixed-seed',False),('mutate','fixed-mutate',False),('inspect','fixed-graceful-restart',False),
        ('control','fixed-crash',True),('inspect','fixed-crash-restart',False)] if args.expected=='fixed' else
       [('inspect','graceful-restart',False),('mutate','crash-mutate',True),('inspect','crash-restart',False),
        ('control','control-save',False),('inspect-control','control-restart',False)])
for mode,label,crash in steps:
    cmd=[sys.executable,str(here/'run_lab.py'),'--mode',mode,'--label',label]+common
    if crash: cmd.append('--crash')
    print('RUN',label,flush=True)
    subprocess.run(cmd,check=True)
print('SUITE PASSED',flush=True)
