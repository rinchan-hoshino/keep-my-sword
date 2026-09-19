# Development

Use Java 21 for Minecraft 1.21.1 and Java 25 for 26.x. Set `JAVA_HOME` for an individual build, or `JAVA_21_HOME` and `JAVA_25_HOME` for the matrix runner.

```sh
python3 scripts/build-matrix.py --rinlib-dir /absolute/path/to/rinlib-jars
```

The dependency directory contains the published `rinlib-1.0.2+<minecraft>-<loader>.jar` files. `profiles.json` owns supported game, loader and toolchain versions. Builds use separate directories for each target; the six artifacts and hashes are collected in `build/releases/manifest.json`.

Fabric development runs include Fabric API for the probes. Release JARs do not bundle or require Fabric API: the carrier uses vanilla registries and components. Minecraft 1.21.1 uses remapped Mojang names; 26.x uses native names.

## Design and validation

- [Shared implementation and persistence boundaries](docs/sealed-items.md)
- [Runtime test commands](runtime-tests/README.md)
- [Current validation](docs/validation.md)
- [Historical 1.0.2 validation](docs/validation-1.0.2.md)

Keep original gameplay data private while an item is broken. Repair calculations may inspect snapshots but must not reactivate input slots. Verify an encoded snapshot before changing the live item; unknown or invalid snapshots remain protected. Storage saves the native carrier directly, without identity conversion or loader-specific adapters. Existing timed effects expire naturally. Exclusion tags belong to modpacks; the default tag stays empty.

Do not package runtime probes or test-only registry objects. Development validation does not publish a release.
