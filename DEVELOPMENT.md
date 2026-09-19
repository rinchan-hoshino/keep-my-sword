# Development

Use Java 21 for Minecraft 1.21.1, or Java 25 for 26.x. Set `JAVA_HOME` accordingly; Fabric Loom requires the Gradle JVM to match the newer game requirement.

```sh
./gradlew :fabric:build :neoforge:build \
  -Pminecraft_version=1.21.1 -PrinlibDir=/absolute/path/to/rinlib-jars
```

The dependency directory contains `rinlib-1.0.2+<minecraft>-<loader>.jar` files. Use the matching published RinLib artifacts. Select another supported Minecraft version through `-Pminecraft_version`; `profiles.json` owns loader and toolchain versions.

## Invariants

- Intervene immediately before vanilla destroys the stack, retaining upstream durability calculation.
- Keep the default exclusion tag empty; modpacks own its contents.
- Broken items lose standard use, mining, attack, ticking and equipment-attribute behavior.
- Only inventory GUI slots receive a warning overlay; held and dropped models retain their colors.
- Keep runtime probes outside production artifacts.

## Build and test the full matrix

Set `JAVA_21_HOME` and `JAVA_25_HOME`, then run:

```sh
python3 scripts/build-matrix.py --rinlib-dir /absolute/path/to/rinlib-jars
python3 runtime-tests/run.py --rinlib-dir /absolute/path/to/rinlib-jars \
  --eula /path/to/accepted/eula.txt --output /path/to/test-results
python3 runtime-tests/run.py --side client --rinlib-dir /absolute/path/to/rinlib-jars \
  --eula /path/to/accepted/eula.txt --output /path/to/test-results
```

Builds use separate output directories for every Minecraft/loader target. Release JARs and their checksums are collected in `build/releases/`.

Fabric 1.21.1 uses Mojang mappings and remapped dependencies. Fabric 26.x uses the [unobfuscated Loom plugin](https://www.fabricmc.net/2026/03/14/261.html), without artificial mapping files. Both 26.x targets compile the same gameplay and Minecraft API adapters. NeoForge 26.2 uses `iconFile` metadata; earlier targets use `logoFile`.
