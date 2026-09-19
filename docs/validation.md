# Validation — 1.1.0-dev vanilla carrier

Local verification on macOS, 2026-09-19. **Unpublished.** This report supersedes the custom-registry experiment; its [historical results](validation-sealed-prototype.md) are kept separately.

| Minecraft | Loader | Save / uninstall / reinstall assertions | Client world and rendering |
| --- | --- | --- | --- |
| 1.21.1 | fabric | 64 / 26 / 36 — passed | Passed |
| 1.21.1 | neoforge | 63 / 25 / 35 — passed | Passed |
| 26.1.2 | fabric | 64 / 26 / 36 — passed | Passed |
| 26.1.2 | neoforge | 64 / 26 / 36 — passed | Passed |
| 26.2 | fabric | 64 / 26 / 36 — passed | Passed |
| 26.2 | neoforge | 64 / 26 / 36 — passed | Passed |

All 18 server phases passed: **753 assertions** across the six targets. The common unit tests passed in all three NeoForge builds (6 executions). All six clients loaded copied test worlds, rendered healthy/broken swords, shields and armor, and captured screenshots. These are actual world/rendering checks, not a claim of manual playthroughs.

## Persistence and recovery

- Complete native component and exact NBT equality after saving, removing the mod and its classes, and reinstalling.
- Independent NBT/JSON serializers, Fabric Transfer and NeoForge ItemResource preserve the carrier without any Keep My Sword save adapters.
- The uninstalled probe independently decodes original snapshots without loading mod classes.
- Nested shulker storage, unknown snapshots and a 180,000-byte random data payload survive the cycle.
- Old pre-component item data passes through Minecraft's item data fixer.
- Unknown/corrupt payloads yield no repair output and leave carriers/materials untouched.
- Metadata deletion/overwrite attempts cannot discard the reserved original snapshot.
- An intentionally overlong native NBT string exercises unsuccessful encoding: destruction is cancelled and the original remains. Its logged NBT encoding error is expected.

## Gameplay

- Actual final durability loss, constructor and direct component mutation paths.
- Custom active, passive and attack callback controls; broken armor/enchantment/shield isolation.
- Immediate removal of cached armor attributes on equipped break; existing timed effects remain.
- Copy, split, reconstruction and network round trips without losing or duplicating items.
- Actual anvil input/material/XP consumption, rename and rebreak, crafting repair and grindstone repair/disenchantment.
- Normal carrier-item ingredient control versus blocked marked carriers; a test consumption recipe cannot consume a sealed item.
- Excluded iron swords retain native destruction behavior.

## Final artifacts

All six JARs were rebuilt from clean profile directories. They contain the approved icon, no runtime probes, no deleted prototype classes and no nested Fabric API modules.

For every Minecraft version, a normal Fabric server with **only Keep My Sword and RinLib** in `mods` started from the final JAR. Console commands placed an exhausted sword in a chest and verified both the native recovery-compass identity and its snapshot before saving and stopping. Artifact hashes match the build manifest. No separate Fabric API or Transfer API was installed.

The server matrix preceded formatting, unused-helper cleanup and an outer encoding-exception guard. Final source passed compilation and all six client runs; final clean-built Fabric artifacts passed the packaged checks. Results and source/artifact fingerprints are recorded explicitly rather than treating historical prototype tests as current validation.

## Uninstall behavior and scope

Uninstall leaves a recovery compass with its original data and a readable recovery tooltip. Reinstall makes its original appearance and supported repairs available again. It does not automatically become a sword while the mod is absent.

Unencodable source items are retained rather than destroyed. Vanilla repair rules still apply; deliberate item deletion, ordinary environmental destruction and arbitrary third-party systems that discard native metadata are outside this protection. No public release, push, production merge or player-save migration was performed.

See [implementation](sealed-items.md), [reproduction](../runtime-tests/README.md), and [machine-readable evidence and hashes](validation.json).
