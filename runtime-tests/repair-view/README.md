# Generic repair view verification

These are actual dedicated-server menu clicks, inventory moves, player death and
third-party block-entity repair ticks in fresh worlds. They are not manual GUI
playthroughs. The probe is outside all production source sets.

Supply an existing accepted EULA and separate loader-specific dependency folders:
Enchanting Infuser 21.1.4, Puzzles Lib 21.1.66, and
[Repair Station 1.3.0+1.21.1](https://modrinth.com/mod/repair-station/versions).
Fabric also needs Forge Config API Port 21.1.6. Dependency downloads stay outside
the repository; verify each file against the publisher's SHA-512 before running.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
python3 runtime-tests/repair-view/run.py --loader neoforge \
  --mods /path/to/neoforge-deps --eula /path/to/eula.txt \
  --output /path/to/fresh-neoforge-results
python3 runtime-tests/repair-view/run.py --loader fabric \
  --mods /path/to/fabric-deps --eula /path/to/eula.txt \
  --output /path/to/fresh-fabric-results \
  --gradle /path/to/gradle-9.5.1/bin/gradle --tools-init /path/to/loom.init.gradle
```

For Puzzles Lib's Fabric development remapping, `loom.init.gradle` contains:

```groovy
settingsEvaluated { settings ->
    settings.pluginManagement.resolutionStrategy.eachPlugin {
        if (requested.id.id == 'fabric-loom') useVersion('1.17.21')
    }
}
```

This uses the same local tools as the existing Infuser probe; it does not change
normal build dependencies. The runner selects Fabric Loader 0.19.5 for Puzzles
Lib's minimum version. Release builds keep the versions in `gradle.properties`.

Require a successful process exit, `repair-view.pass`, `REPAIR_VIEW_ALL_PASS` in
`run.log`, and `results.json`. The runner shares the existing runtime build lock.
Tests cover native restricted slots, exact original components, pickup, shift
in/out, hotbar swaps, drag, persistent-machine close/reopen, partial repairs,
inventory sweeps, storage menus and automatic ordinary-slot storage detection.
They repair swords through vanilla anvil, Advanced Infuser and Repair Station,
and check XP/fuel, metadata, changed enchantments, unreadable snapshots, thrown
items and real death drops.

`RepairViewChecks` also runs through the existing save/unload/reinstall matrix on
all six game/loader targets, including 26.x's renamed `ContainerInput` enum.
The original Infuser regression remains available with its active-menu preview
expectations updated for 1.3.0; its sealed-input enchanting guard is still tested.

## Verification, 2026-11-07

See [the implementation record](../../docs/generic-repair-view.md) for final
results, limitations and the retained compatibility hooks.
