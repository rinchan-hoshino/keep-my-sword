# Generic repair view, 1.3.0

The approved design makes the exact original item live in non-player work-menu
slots while their menu is open. Native workstations own validation, repair,
material/XP costs and enchantment changes. This replaces the need to add a repair
accessor for each new workstation mod. Snapshot encoding, decoding, unknown-data
protection and native persistence are unchanged.

## Boundaries and decisions

- `RepairView` is common to both loaders. A click exposes work inputs and only the
  cursor/source inventory item needed for that transaction, then seals broken
  cursor/inventory items in `finally`. Pickup, shift, hotbar swaps and drag use
  native click handling. Inventory slots themselves stay sealed between clicks.
- Player ticks expose preexisting machine inputs and sweep the player's entire
  inventory every 20 ticks. Equipped items are refreshed when the sweep seals
  them. Player drops seal before item entities spawn. Close, death and disconnect
  seal all non-player menu slots before Minecraft returns/drops/saves their items.
- The player's own inventory menu is excluded. Storage is excluded by configured
  menu ID or class (including subclasses), or when all non-player slots are exact
  ordinary `Slot`s backed by block-entity containers or a `CompoundContainer`.
  Defaults cover vanilla chest sizes, shulkers, hoppers and dispensers, plus
  Sophisticated Backpacks/Storage menu classes. `repairViewExcludedMenus` replaces
  the defaults when explicitly supplied; restart after editing the TOML.
- Registry IDs keep vanilla defaults stable under Fabric remapping. Mod classes
  may be used for menus sharing a type or lacking a registered type. Click enums
  are coerced to their shared enum contract because `ClickType` was renamed to
  `ContainerInput` in 26.x; no extra dependency or per-loader implementation is
  needed. [MixinExtras supports this coercion](https://github.com/LlamaLad7/MixinExtras/releases/tag/0.5.0).
- The Infuser-specific repair accessor was removed. Its mixin now only rejects
  enchanting of inputs that are still sealed (including unreadable snapshots).
  Exposed originals may use native enchanting; if still broken at exit they are
  sealed with their new enchantments.
- The existing anvil hook remains: detached repair callers still need private
  snapshot views, invalid snapshots must block results, and rename-only results
  must preserve sealing. Existing crafting and grindstone hooks remain for their
  corresponding detached/recipe transactions. The original regression exercises
  these paths in addition to the new active-menu tests.

## Verification

Verified 2026-11-07 from `fix/infuser-repair` (`4abacbd`) on the local
`feat/generic-repair-view` branch. The new restricted-workbench probe failed
against that base at native slot acceptance before implementation. Final runs:

| Scenario | Fabric 1.21.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Vanilla anvil repair, source/material consumption and exact XP | PASS | PASS |
| Advanced Enchanting Infuser, eight XP levels and exact original components | PASS | PASS |
| Repair Station 1.3.0 native fuel ticks, one healthy output, metadata retained | PASS | PASS |
| Pickup, shift in/out, hotbar swaps and drag | PASS | PASS |
| Close/reopen persistent machine; still broken reseals; repaired stays original | PASS | PASS |
| Chest, hopper, dispenser, shulker and unlisted ordinary-slot block storage | PASS | PASS |
| One-second inventory sweep and throwing from workbench | PASS | PASS |
| Actual player death: newly spawned drop and machine input remain sealed | PASS | PASS |
| Infuser enchantment change while broken, then close/reseal with changes retained | PASS | PASS |
| Unknown snapshot rejection and exact native components through close | PASS | PASS |
| Existing Infuser regression, including low XP and prior-work penalty | 19/19 | 19/19 |
| New three-mod probe | 34/34 | 34/34 |

All six supported release builds passed. Unit tests passed 15/15 per game version
(45 total). The original six-target save/unload/reinstall matrix, now also
exercising the common active workbench/anvil boundary, passed all 18 phases and
1,005 assertions. Both 26.x targets also passed real pickup/shift/close/drop and
anvil transactions. The release JAR scan found no runtime probes or removed
repair/drop mixin classes. Snapshot/persistence implementation files have zero
changed lines.

The audit directory is `/Users/rin/.rin/work/kms-generic-20261107`: use
`regression-final/results.json`, `{fabric,neoforge}-verified/results.json`,
`infuser-final/run.log`, `infuser-fabric-final/results.json`, and
`build-verified.log`. Each server run requires its explicit pass marker as well
as a successful Gradle exit. Dependencies were verified against publisher
SHA-512 hashes; reused local dependencies were copied with SHA-256 equality.

Reproduction commands are in [runtime-tests/repair-view](../runtime-tests/repair-view/README.md)
and the existing [runtime matrix guide](../runtime-tests/README.md).

## Scope and remaining risks

The new three-mod repair checks target Minecraft 1.21.1 on Fabric and NeoForge;
26.x runs the common work-menu/anvil checks and original persistence matrix.
These are server-side transactions, not rendered GUI or manual playthroughs.
Modded storage that uses special slots and an unlisted menu needs a configuration
exclusion. The Sophisticated defaults exclude the whole storage menu, including
any embedded workbench upgrades; opting into those requires changing the list.
A mod bypassing native clicks/drop/close paths may leave an original outside its
machine until the player's next inventory sweep; automation into non-player
storage and abrupt process termination while a menu is open have no global
recovery sweep. No save-format or persistence interception was added for these
paths.
