# Runtime verification

Set `JAVA_21_HOME` and `JAVA_25_HOME`. Supply matching published RinLib JARs and an existing accepted EULA file. Every test uses an isolated world; no player save is selected.

## Server behavior and persistence

```sh
python3 runtime-tests/run.py --side server \
  --rinlib-dir /path/to/rinlib-jars --eula /path/to/eula.txt \
  --output /path/to/server-results
```

This delegates to `sealed-matrix.py`. Every supported Minecraft/loader target runs three phases: save with the mod, reopen with the mod and its classes absent, and reopen after reinstalling it. Full component and exact NBT comparisons cover plain items, independent shell edits and nested containers. Actual damage and repair transactions test use/tick/attack callbacks, construction and mutation boundaries, iron-sword exclusion, copy/split/network behavior, anvil rename, crafting repair and grindstones. Platform resource codecs are tested on Fabric and modern NeoForge.

Read `results.json`, per-phase logs and pass markers. A successful Gradle exit without the expected marker is a failure. Existing worlds are never overwritten by a new save-phase test. All launchers share a build lock because loaded and unloaded probes use different source sets.

## Client rendering

```sh
python3 runtime-tests/run.py --side client \
  --rinlib-dir /path/to/rinlib-jars --eula /path/to/eula.txt \
  --worlds /path/to/server-results --output /path/to/client-results
```

Each client loads a copy of its verified server world, renders healthy and sealed swords, shields and armor, renders the original tooltip plus the broken warning, captures a screenshot and exits. This checks real world loading and rendered output; it is not described as a manual gameplay playthrough.

Use repeated `--minecraft` and `--loader` arguments to select a subset.

## Packaged Fabric installation

After `scripts/build-matrix.py`, use the official Fabric installer to check the built JARs in normal servers:

```sh
python3 runtime-tests/packaged-fabric.py \
  --installer /path/to/fabric-installer.jar \
  --rinlib-dir /path/to/rinlib-jars --eula /path/to/eula.txt \
  --output /path/to/packaged-results
```

Only Keep My Sword and RinLib are placed in `mods`. Keep My Sword bundles its necessary Fabric base, networking and registry-sync modules. The test checks server startup, creates a zero-durability sword, saves and shuts down. Neither a separate full Fabric API installation nor the optional Transfer API is supplied.

Test items, entrypoints, datapacks and fake players are outside production source sets.
