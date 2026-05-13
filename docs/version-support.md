# Version support

Keep My Sword targets the same Minecraft matrix as RinLib. Every published build must keep the same player-facing behavior on its supported loader: broken damageable items stay repairable, lose gameplay function, and render as broken.

## Target matrix

| Minecraft | Forge | NeoForge | Fabric | Java | Status |
| --- | --- | --- | --- | --- | --- |
| 1.7.10 | Forge `10.13.4.1614` | Not applicable | Not applicable | 8 | Target |
| 1.12.2 | Forge `14.23.5.2864` | Not applicable | Not applicable | 8 | Target |
| 1.16.5 | Forge `36.2.42` | Not applicable | Fabric API `0.42.0+1.16` | 8 | Target |
| 1.20.1 | Forge `47.4.20` | NeoForge `47.1.106` | Fabric API `0.92.9+1.20.1` | 17 | Target |
| 1.21.1 | Not targeted | NeoForge `21.1.229` | Fabric API `0.116.12+1.21.1` | 21 | Implemented first |
| 26.1.x | Not targeted | NeoForge `26.1.2.48-beta` | Fabric API `0.148.2+26.1.2` | 25 | NeoForge implemented; Fabric blocked by missing mappings |

Forge is intentionally capped at Minecraft 1.20.1. NeoForge starts at Minecraft 1.20.1. Fabric starts at Minecraft 1.16.5. For 26.1.x, Fabric artifacts exist but dev mappings for `26.1.2` are not currently published, so this branch keeps a NeoForge-only build until Fabric mappings/tooling are available.

## Porting order

1. 1.20.1, because it is closest to the current implementation and covers Forge, NeoForge, and Fabric together.
2. 1.16.5, because it is the oldest Fabric target and a Forge bridge point.
3. 26.1.x, because it is current but may move while beta loader APIs settle.
4. 1.12.2 and 1.7.10, because legacy Forge needs separate Gradle, mappings, and Java 8 handling.

## Behavior contract

A supported build must do all of the following:

- Prevent damageable stacks from being destroyed when durability reaches zero.
- Keep the broken stack movable, droppable, and repairable.
- Treat broken stacks as model-only items for gameplay: no item use, no special mining speed, no correct-tool harvest, no item attack hook, no continuous use, and no equipment attributes.
- Add no tooltip text.
- Render a red slot overlay in inventory-like GUI slots.
- Render a red surface tint when the item is shown outside a GUI slot.

Compatibility glue may differ per Minecraft version and loader. Do not add broad fallback paths that hide a broken version contract; fix the version-specific hook instead.

## Per-release modern status
<!-- modern-status:1.21.2 -->
- `1.21.2`: Implemented Fabric `0.106.1+1.21.2` against RinLib branch `mc/1.21.2`; NeoForge skipped for now pending per-version NeoGradle/ModDev wiring; Forge not targeted after 1.20.1.
<!-- modern-status:1.21.4 -->
- `1.21.4`: Implemented Fabric `0.119.4+1.21.4` against RinLib branch `mc/1.21.4`; NeoForge skipped for now pending per-version NeoGradle/ModDev wiring; Forge not targeted after 1.20.1.
<!-- modern-status:1.21.9 -->
- `1.21.9`: Implemented Fabric `0.134.1+1.21.9` against RinLib branch `mc/1.21.9`; NeoForge skipped for now pending per-version NeoGradle/ModDev wiring; Forge not targeted after 1.20.1.
