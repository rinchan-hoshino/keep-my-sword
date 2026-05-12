# Keep My Sword

Keep My Sword keeps broken tools and equipment as repairable model-only items instead of destroying them.

## Behavior

- Damageable items stay in their slot when durability reaches zero.
- A zero-durability item remains repairable and movable.
- Broken items keep their model, but gameplay hooks act like the item has no usable function: no use action, no special mining speed, no correct-tool harvest, no item attack hook, no inventory ticking, and no equipment attribute modifiers.
- Broken items do not add tooltip text.
- In GUI slots, broken items show an empty durability bar and a red slot overlay.
- Outside GUI slots, broken items render with a red surface tint instead of a slot overlay.

## Dependency

Keep My Sword depends on RinLib.

## Supported targets

The first implemented line is Minecraft 1.21.1 for Fabric and NeoForge.

The full target matrix is tracked in [docs/version-support.md](docs/version-support.md): 1.7.10, 1.12.2, 1.16.5, 1.20.1, 1.21.1, and the latest 26.1.x line, with Forge capped at 1.20.1, NeoForge starting at 1.20.1, and Fabric starting at 1.16.5.

## Build

Publish RinLib locally first:

```bash
cd ../rinlib
./gradlew publishToMavenLocal
```

Then build Keep My Sword:

```bash
cd ../keep-my-sword
./gradlew build
```

## Local CI

Run the same local checks as the commit hook:

```bash
./scripts/ci.sh
```

The check builds all loaders on the current branch and starts each headless server run until it reaches startup. The repository includes `.githooks/pre-commit`; enable it once per clone with:

```bash
git config core.hooksPath .githooks
```

The containerized hook builds and installs RinLib from the matching RinLib branch name, falling back to `main` when that branch does not exist.
