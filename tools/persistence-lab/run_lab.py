"""Isolated actual-Forge process/restart test. Raw logs and data remain under run/."""
from pathlib import Path
import hashlib, json, os, queue, shutil, subprocess, threading, time, argparse

repo = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--mode', choices=['seed','mutate','inspect','control','inspect-control'], required=True)
parser.add_argument('--label', required=True)
parser.add_argument('--crash', action='store_true')
parser.add_argument('--expected', choices=['broken','fixed'], default='broken')
parser.add_argument('--baseline-server', type=Path, required=True, help='Prepared Forge 1.20.1 server to copy read-only')
parser.add_argument('--forge-version', help='Installed Forge version to select when the baseline has multiple versions')
parser.add_argument('--lab-root', type=Path, default=repo/'run/persistence-lab')
parser.add_argument('--mod-jar', type=Path, help='Replacement TerraFirmaColonies JAR, only inside this lab')
args = parser.parse_args()
root=args.lab_root.resolve(); base=args.baseline_server.resolve(); server=root/'server'
if base == root or base in root.parents or root in base.parents:
    parser.error('--baseline-server and --lab-root must be separate, non-nested directories')
forge_args_name = 'win_args.txt' if os.name == 'nt' else 'unix_args.txt'
forge_libraries = base/'libraries/net/minecraftforge/forge'
forge_candidates = list(forge_libraries.glob('1.20.1-*/'+forge_args_name))
if args.forge_version:
    forge_candidates = [p for p in forge_candidates if p.parent.name == '1.20.1-'+args.forge_version]
if len(forge_candidates) != 1:
    parser.error('Expected one installed Forge 1.20.1 argument file; use --forge-version to select among multiple versions')
forge_args = forge_candidates[0].relative_to(base)
java = str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else shutil.which('java')
if not java: raise SystemExit('Java 17 is required (JAVA_HOME or PATH)')
assert args.label.replace('-','').replace('_','').isalnum(), 'Use an alphanumeric label'
assert not (root/'evidence'/(args.label+'-result.json')).exists(), 'Use a new label to preserve earlier evidence'
assert (root/'persistence-probe.jar').is_file(), 'Build probe first'
if not server.exists():
    assert args.mode == 'seed', 'First run must seed a fresh lab world'
    server.mkdir(parents=True)
    for d in ['libraries','mods','config','defaultconfigs']:
        shutil.copytree(base/d, server/d)
    # Fresh world, own port and configuration; the supplied baseline stays read-only.
    for f in ['eula.txt','server.properties']:
        shutil.copy2(base/f, server/f)
    props = (server/'server.properties').read_text()
    overrides={'level-name':'persistence-world','server-ip':'127.0.0.1','server-port':'25576','max-tick-time':'180000'}
    props='\n'.join(line for line in props.splitlines() if line.partition('=')[0] not in overrides)
    props+='\n'+'\n'.join(k+'='+v for k,v in overrides.items())+'\n'
    (server/'server.properties').write_text(props)
shutil.copy2(root/'persistence-probe.jar', server/'mods/persistence-probe.jar')
manifest=[{'file':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
          for p in (base/'mods').glob('*.jar')]
target_mods=list((server/'mods').glob('terrafirmacolonies-*.jar'))
assert len(target_mods)==1, 'Expected exactly one TerraFirmaColonies JAR'
if args.mod_jar:
    assert args.mod_jar.is_file(), 'Replacement JAR does not exist'
    shutil.copy2(args.mod_jar, target_mods[0])
for mod in manifest:
    installed=server/'mods'/mod['file']
    expected_hash=hashlib.sha256(args.mod_jar.read_bytes()).hexdigest() if args.mod_jar and installed==target_mods[0] else mod['sha256']
    assert hashlib.sha256(installed.read_bytes()).hexdigest() == expected_hash, f'Baseline hash mismatch: {installed.name}'
assert 'eula=true' in (server/'eula.txt').read_text()
evidence = root/'evidence'; evidence.mkdir(exist_ok=True)
def hashes():
    world = server/'persistence-world'
    files = list((world/'data').glob('*')) + list((world/'minecolonies').rglob('*.dat')) + list(world.glob('level.dat*'))
    return {str(p.relative_to(server)):dict(size=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest()) for p in files if p.is_file()}

proc = subprocess.Popen([java,'-Xms512M','-Xmx3G','-Dpersistence.lab=true',f'-Dpersistence.mode={args.mode}',f'-Dpersistence.label={args.label}',f'-Dpersistence.expected={args.expected}',
    '@'+forge_args.as_posix(),'nogui'], cwd=server,
    stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
q=queue.Queue()
def reader():
    for line in proc.stdout: q.put(line)
    q.put(None)
worker=threading.Thread(target=reader,daemon=True); worker.start()
ready=False; saved=False; fatal=False; shutdown_error=False; cleanup_kill=False; deadline=time.monotonic()+360
def send(cmd): proc.stdin.write(cmd+'\n'); proc.stdin.flush()
try:
    with (evidence/(args.label+'.log')).open('w',encoding='utf-8') as log:
        while time.monotonic()<deadline:
            try: line=q.get(timeout=1)
            except queue.Empty:
                if proc.poll() is not None: break
                continue
            if line is None: break
            log.write(line); log.flush()
            if 'PERSISTENCE_PROBE' in line or 'Saved the game' in line or 'Done (' in line: print(line.rstrip(),flush=True)
            if 'PERSISTENCE_PROBE FATAL' in line: fatal=True; send('stop')
            if 'Exception stopping the server' in line:
                shutdown_error=True
                # Vanilla cannot complete shutdown after this error. Preserve the error,
                # then explicitly classify the owned-process cleanup as a forced kill.
                deadline=min(deadline,time.monotonic()+8)
            if 'PERSISTENCE_PROBE READY' in line:
                ready=True
                # Commands execute after ServerStarted returns to the server loop.
                send('save-all flush')
            if 'Saved the game' in line and not saved:
                saved=True
                (evidence/(args.label+'-files.json')).write_text(json.dumps(hashes(),indent=2))
                world=server/'persistence-world'
                snap=evidence/(args.label+'-data')
                if not snap.exists():
                    snap.mkdir()
                    for d in ['data','minecolonies']:
                        if (world/d).exists(): shutil.copytree(world/d,snap/d)
                    shutil.copy2(world/'level.dat',snap/'level.dat')
                if args.crash:
                    proc.kill()  # Only this owned lab child; no stop/save on shutdown.
                else: send('stop')
        try: proc.wait(timeout=15)
        except subprocess.TimeoutExpired:
            cleanup_kill=True; proc.kill(); proc.wait(timeout=20)
finally:
    if proc.poll() is None: proc.kill(); proc.wait(timeout=20)
    worker.join(timeout=5)
result=dict(mode=args.mode,label=args.label,expected=args.expected,probe_ready=ready,save_command_completed=saved,fatal=fatal,shutdown_error=shutdown_error,cleanup_kill=cleanup_kill,forced_kill=args.crash,exit_code=proc.returncode,
    forge_arguments_file=forge_args.as_posix(),
    mod_sha256=hashlib.sha256(target_mods[0].read_bytes()).hexdigest(),
    probe_sha256=hashlib.sha256((server/'mods/persistence-probe.jar').read_bytes()).hexdigest())
(evidence/(args.label+'-result.json')).write_text(json.dumps(result,indent=2))
print(json.dumps(result),flush=True)
if fatal or not ready or not saved or shutdown_error or (not args.crash and proc.returncode!=0): raise SystemExit(1)
