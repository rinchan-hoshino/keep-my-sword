# Development

Use Java 21 for Minecraft 1.21.1 and Java 25 for 26.x. Set `JAVA_HOME` for one build, or `JAVA_21_HOME` and `JAVA_25_HOME` for the matrix runner.

```sh
./gradlew build
python3 scripts/build-matrix.py
```

`profiles.json` lists the supported game, loader and toolchain versions. The default build writes to `build/profiles/1.21.1/<loader>/`. The matrix runner writes each target to its own directory and collects the artifacts and hashes in `build/releases/manifest.json`.

## Layout

- `common/src/main/java` holds the shared code. Both loaders compile it through `build.gradle`.
- `common/src/main/java/dev/rinchan/keepmysword/mixin` holds the Mixins. Each one has a comment that gives its injection point.
- `common/src/main/resources` holds the assets, data, and mixin configs.
- `common/src/test/java` holds the unit tests. The NeoForge build runs them with `./gradlew test`.
- `fabric/` and `neoforge/` hold the loader entrypoints, build scripts, and metadata templates.

## Dependencies

- RinLib is pinned in `gradle.properties` (`rinlib_version`). Fabric includes it with `include`; NeoForge includes it with `jarJar`. Players do not install RinLib separately.
- To build against a local RinLib jar, pass `-PrinlibJar=<path>` for Fabric or `-PrinlibNeoJar=<path>` for NeoForge.
- `DamageState` comes from `dev.rinchan.rinlib.item`. Keep-My-Sword does not carry its own copy.

## Design notes

- Broken items are stored as a carrier that holds the original item snapshot. See [docs/sealed-items.md](docs/sealed-items.md).
- Repair menus show the exact original while they are open. Items are sealed again when the menu closes, when they leave the player's control, or when the player disconnects or dies.
- Storage menus that must keep items sealed are listed in `repairViewExcludedMenus` (config).
- Exclusion tags belong to modpacks. The default `#keep_my_sword:excluded` tag is empty.

## Rules for changes

- Do not package runtime probes or test-only registry objects.
- Do not add release metadata other than the version in `gradle.properties`.
- Do not hard-code local paths.
