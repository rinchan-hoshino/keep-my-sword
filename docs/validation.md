# Validation — 1.0.2

Verified locally on macOS on 2026-09-19, using the matching published RinLib 1.0.2 JARs.

| Minecraft | Loader | Server gameplay | Client startup |
| --- | --- | --- | --- |
| 1.21.1 | fabric | 28/28 | Passed |
| 1.21.1 | neoforge | 28/28 | Passed |
| 26.1.2 | fabric | 28/28 | Passed |
| 26.1.2 | neoforge | 28/28 | Passed |
| 26.2 | fabric | 28/28 | Passed |
| 26.2 | neoforge | 28/28 | Passed |

## Coverage

- Real `ItemStack.hurtAndBreak` calls retain the original stack, its name and zero-durability state. Excess and repeated damage do not delete it.
- Native `AnvilMenu` repairs the broken sword, consumes the inputs and deducts experience. Repair restores item attributes and harvesting capability.
- A test datapack excludes iron swords; they break normally. Empty and non-damageable stacks remain unaffected.
- Broken tools lose harvesting/attribute bonuses, and a broken shield cannot begin or sustain use.
- Each client renders the title screen for 20 ticks before exiting, checking client loading and GUI mixin application. This is a startup check, not a full interactive playthrough.
- The two README screenshots came from an authenticated Minecraft 1.21.1 NeoForge session: a real attack left the sword at damage 1561, and native anvil repair changed it to 1171 while consuming one diamond and reducing experience from 30 to 29.

## Reproduction

Run the commands in [DEVELOPMENT.md](../DEVELOPMENT.md). The executable test cases and matrix results are in [runtime-tests/scenarios.json](../runtime-tests/scenarios.json). Raw logs and pass markers are written to the selected output directory.

Test-only entrypoints, fake players and datapacks are kept outside the production source set.

## Release packaging

All six release builds passed. The three existing common resource/style unit tests passed on each Minecraft profile (9 executions). All six JARs contain the approved icon and no runtime probe classes. Fabric 1.21.1 includes its generated refmap; Fabric 26.x uses native names. The local RinLib dependency files match their published Modrinth SHA-512 checksums.
