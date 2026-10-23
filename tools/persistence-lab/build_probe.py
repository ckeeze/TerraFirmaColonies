"""Compile against exact installed SRG runtime; no dependency downloads or Gradle changes."""
from pathlib import Path
import argparse, os, shutil, subprocess, zipfile

repo = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline-server', type=Path, required=True, help='Prepared Forge 1.20.1 server to copy read-only')
parser.add_argument('--lab-root', type=Path, default=repo/'run/persistence-lab')
args_cli = parser.parse_args()
base = args_cli.baseline_server.resolve()
root = args_cli.lab_root.resolve()
if base == root or base in root.parents or root in base.parents:
    parser.error('--baseline-server and --lab-root must be separate, non-nested directories')
runtime_jars = list((base/'libraries/net/minecraft/server').rglob('*-srg.jar'))
if len(runtime_jars) != 1:
    parser.error('Expected exactly one Minecraft SRG server JAR in the prepared baseline')
out = root/'probe-classes'
out.mkdir(parents=True, exist_ok=True)
javac = str(Path(os.environ['JAVA_HOME'])/'bin/javac') if os.environ.get('JAVA_HOME') else shutil.which('javac')
if not javac: raise SystemExit('Java 17 javac is required (JAVA_HOME or PATH)')
mc = runtime_jars[0]
jars = [mc] + [p for p in (base/'libraries').rglob('*.jar') if '/net/minecraft/server/' not in p.as_posix()] + list((base/'mods').glob('*.jar'))
args = ['-proc:none', '-encoding', 'UTF-8', '-classpath', os.pathsep.join(str(p) for p in jars), '-d', str(out), str(Path(__file__).parent/'src/PersistenceProbe.java')]
argfile = out.parent/'javac.args'
argfile.write_text('\n'.join('"'+s.replace('\\','/')+'"' for s in args), encoding='utf-8')
subprocess.run([javac, '@'+str(argfile)], check=True)
with zipfile.ZipFile(out.parent/'persistence-probe.jar', 'w') as z:
    for p in out.rglob('*.class'): z.write(p, p.relative_to(out).as_posix())
    z.write(Path(__file__).parent/'probe.mods.toml', 'META-INF/mods.toml')
print('Probe built')
