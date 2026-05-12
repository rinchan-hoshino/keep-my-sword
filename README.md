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

## Supported target

- Minecraft 1.21.1
- Fabric
- NeoForge
- Java 21

New Minecraft versions should be added as separate release lines after the 1.21.1 line is stable.

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
