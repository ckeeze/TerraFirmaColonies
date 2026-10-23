# Colony persistence regression lab

This fixture edits its own disposable colony on server startup. Never install
`persistence-probe.jar` in a player profile or production server. No player world
is copied. Keep baseline and fixed runs in separate lab directories.

## Prerequisites

- Java 17 (`JAVA_HOME` or `PATH`), Python 3.10+, and this repository.
- A prepared Forge Minecraft 1.20.1 dedicated server with `libraries`, `mods`,
  `config`, `defaultconfigs`, `server.properties`, and an accepted `eula.txt`.
  Start that server once to generate its runtime files, then stop it before
  using it as the baseline. The scripts copy runtime files and create a fresh
  world in the lab. The recorded automated runs used Forge 47.4.18.
- Tested mods: MineColonies 1.1.1237-snapshot, TFC 3.2.27, Firmalife 2.1.28,
  Structurize 1.0.806, BlockUI 1.0.190, Domum Ornamentum 1.0.288, Patchouli 85,
  FirmalifeEnhance 1.0.0. Hashes are recorded in the evidence.

`--baseline-server` is required for every build or launch command: supply your
own prepared server directory. It must be separate from, and not nested inside,
the lab directory (or vice versa). The baseline is read-only; no world is copied.
Copied mod hashes are checked against the supplied baseline JARs. An optional
replacement TerraFirmaColonies JAR is checked against the replacement file.

The launcher discovers the installed Forge 1.20.1 argument file for the current
OS (`win_args.txt` or `unix_args.txt`). If the baseline contains multiple Forge
versions, pass `--forge-version`, for example `--forge-version 47.4.18`, to every
suite or individual launch command. Detection does not imply compatibility with
arbitrary mod or Forge versions; use the tested versions above for reproduction.

The lab binds to `127.0.0.1:25576` and creates `persistence-world`. Do not run
suites concurrently. Existing result labels are refused; use a fresh
`--lab-root` for a new suite. There is no automatic world deletion.

## Verify the fix

Build the normal distributable JAR first; `reobfJar` is part of this build:

```powershell
$baseline = 'C:\path\to\prepared-forge-server'
.\gradlew.bat build
python tools/persistence-lab/build_probe.py --baseline-server "$baseline" --lab-root run/persistence-fixed-lab
python tools/persistence-lab/run_suite.py --baseline-server "$baseline" --expected fixed --lab-root run/persistence-fixed-lab --mod-jar build/libs/terrafirmacolonies-0.1.3.4.jar
python tools/persistence-lab/summarize.py --lab-root run/persistence-fixed-lab
```

Replace the example baseline path with your stopped, prepared server directory.
Use the same baseline for probe compilation and suite launches. Use a fresh lab
root if the example directory already exists.
The probe compiles against the server's SRG runtime without changing Gradle
dependencies or downloading anything.

The fixed suite performs five separate JVM launches:

1. Save a baseline colony with one town hall.
2. Verify missing/populated equipment, malformed/missing coordinates, explicit
   origin coordinates, list replacement on repeated load, and distinct kitchen
   pot/VAT positions. Add three unfinished buildings and finish research;
   execute `save-all flush` and `stop`.
3. Reload and verify buildings, colony name, research and absent equipment.
4. Save that state and forcibly kill only the owned Java child process.
5. Reload and verify colony state again, then stop normally.

A kill **after** a completed flush isolates serialization failure from ordinary
unsaved progress. It is not a power-loss test during a physical disk write.

## Reproduce the original defect

Use unmodified TerraFirmaColonies 0.1.3.4 in the baseline and a fresh lab root.
Do not supply `--mod-jar`:

```powershell
$baseline = 'C:\path\to\prepared-forge-server-with-original-mod'
python tools/persistence-lab/build_probe.py --baseline-server "$baseline" --lab-root run/persistence-original-lab
python tools/persistence-lab/run_lab.py --baseline-server "$baseline" --mode seed --label seed --lab-root run/persistence-original-lab
python tools/persistence-lab/run_lab.py --baseline-server "$baseline" --mode mutate --label graceful-mutate --lab-root run/persistence-original-lab
python tools/persistence-lab/run_suite.py --baseline-server "$baseline" --lab-root run/persistence-original-lab
python tools/persistence-lab/summarize.py --lab-root run/persistence-original-lab
```

`mutate` deliberately reproduces a shutdown error and reports nonzero. Continue
only if the log shows all assertions passed, `Saved the game`, and the expected
building-serialization `NullPointerException`. Arbitrary startup, assertion or
save failures are not successes. The first recorded baseline run used an earlier
harness which returned zero despite the shutdown exception; the summary checks
the actual log independently.

The baseline continuation verifies ordinary and forced-kill reloads, then tests
a control kitchen with both equipment positions populated, demonstrating that
the rollback depends on the serialization failure.

## Evidence and coverage

Each lab's `evidence/` retains logs, SNBT, captured saved NBT, hashes and results.
`summarize.py` independently decodes `data/capabilities.dat`. Read-only inspection:

```powershell
python tools/persistence-lab/inspect_nbt.py run/persistence-fixed-lab/evidence/fixed-mutate-data/data/capabilities.dat
```

The JVM, Forge persistence, registry-produced buildings and modules, transformed
mixins, research state, save commands, disk NBT and process reloads are real.
Manager insertion, equipment setup and research completion use reflection/API
fixtures. They do not cover Build Tool placement, blueprint construction,
university work cycles or citizen production/navigation. Follow the gameplay
checklist in [the report](../../docs/colony-persistence-investigation.md) before
claiming those behaviors are verified.

Manual gameplay checks use a separately prepared matching client/server profile
without the probe. Client-profile packaging and local modpack distribution are
outside this regression fixture.
