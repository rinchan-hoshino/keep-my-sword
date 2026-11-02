# Runtime verification

Set `JAVA_21_HOME` and `JAVA_25_HOME`. Supply an existing accepted EULA file. Every test uses an isolated world; no player save is selected.

## Server behavior and persistence

```sh
python3 runtime-tests/run.py --side server \
  --eula /path/to/eula.txt \
  --output /path/to/server-results
```

This delegates to `sealed-matrix.py`. Every supported Minecraft/loader target runs three phases: save with the mod, reopen with the mod and its classes absent, and reopen after reinstalling it. Full component and exact native NBT comparisons cover native carriers, nested containers, unknown snapshots and large payloads. Independent NBT/JSON serializers and platform resource codecs need no Keep My Sword save adapter. Native NBT keeps numeric/array types; JSON checks the JSON representation because it converts typed NBT arrays to lists. Uninstalled probes also decode the snapshot independently of the mod.

Actual damage and repair transactions test sword attacks, incoming armor damage, dispenser shears, entityless damage, use/tick/attack callbacks, immediate removal of equipped attributes, iron-sword exclusion, copy/split/network behavior, anvil rename, crafting repair and grindstones. Construction and direct component mutations remain unsealed. Legacy chunked Base64 carriers and old native DataVersions decode and repair through the real anvil transaction. Invalid snapshots cannot consume inputs. Ordinary custom-data edits are no longer intercepted.

Read `results.json`, per-phase logs and pass markers. A successful Gradle exit without the expected marker is a failure. Existing worlds are never overwritten by a new save-phase test. All launchers share a build lock because loaded and unloaded probes use different source sets.

## Client rendering

```sh
python3 runtime-tests/run.py --side client \
  --eula /path/to/eula.txt \
  --worlds /path/to/server-results --output /path/to/client-results
```

Each client loads a copy of its verified server world, renders healthy and sealed swords, shields and armor, renders the original tooltip plus the broken warning, captures a screenshot and exits. This checks real world loading and rendered output; it is not described as a manual gameplay playthrough.

Use repeated `--minecraft` and `--loader` arguments to select a subset.

## Packaged Fabric installation

After `scripts/build-matrix.py`, use the official Fabric installer to check the built JARs in normal servers:

```sh
python3 runtime-tests/packaged-fabric.py \
  --installer /path/to/fabric-installer.jar \
  --eula /path/to/eula.txt \
  --output /path/to/packaged-results
```

Only Keep My Sword is placed in `mods`. Keep My Sword bundles no Fabric API modules. The test checks server startup, constructs a zero-durability sword without sealing it, saves and shuts down. Neither Fabric API nor Transfer API is supplied.

Test items, entrypoints, datapacks and fake players are outside production source sets.
