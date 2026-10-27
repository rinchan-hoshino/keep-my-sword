## Keep My Sword 1.2.0 (Minecraft 1.21.1 / NeoForge)

### Features & Improvements
- **TOML configuration**: Added `config/keep_my_sword.toml` loaded via NightConfig on game startup (restart required after edits).
- **Item exclusion list**: Added `excludedItems` to configure items that bypass broken-item protection. Supports item IDs (such as `"minecraft:diamond_sword"`) and item tags (such as `"#slashblade:swords"`). Defaults to an empty list `[]`.
- **Existing compatibility**: The native `#keep_my_sword:excluded` item tag remains fully supported alongside the configuration file.
- **Core behavior preserved**: Broken equipment protection, anvil repair mechanics, and item data persistence remain unchanged.

# Changelog

## 1.1.2

- Aligned Fabric and NeoForge loader manifests to SPDX `GPL-3.0-only` and bundled the repository `LICENSE` file in both JAR distributions.
- Corrected platform license metadata on Modrinth and CurseForge to match the project's GNU General Public License v3.0.
- Released version 1.1.2 for Minecraft 1.21.1, 26.1.2, and 26.2 on Fabric and NeoForge with no gameplay changes.

## 1.1.1-wmf.1 — 2026-09-26

- Avoid damage and enchantment queries while decoding login inventory packets before a registry context is available.
- Preserve the original item and existing carrier format until snapshot encoding is possible.
- Add a regression test for unscoped item decoding. This pack hotfix is verified on Minecraft 1.21.1 / NeoForge.

## 1.1.0 — 2026-09-19

- Isolate broken items as vanilla carriers with complete original-item snapshots, stopping further original item callbacks, attributes and enchantments.
- Preserve broken-item data when Keep My Sword is uninstalled. Reinstall to display and repair the original item; uninstalled carriers remain recovery compasses with an explanatory tooltip.
- Prevent carriers from being used as ordinary recipe ingredients and preserve their snapshots during metadata edits.
- Support anvil, crafting and grindstone repair without reactivating input items.
- Keep original data intact when snapshot encoding or recovery fails; retain unknown and future snapshots.
- Remove custom item/component registration and loader-specific storage conversion adapters.

## 1.0.2 — 2026-09-19

- Fix the item-destruction hook on Minecraft 26.1.2 and 26.2.
- Share gameplay logic across all six supported Minecraft/loader targets, using RinLib 1.0.2 for durability state and clamping.
- Build Fabric 26.x with the native unobfuscated Loom pipeline.
- Remove the empty Fabric entrypoint, unnecessary Fabric API runtime dependency, and redundant policy wrapper.
- Refresh the icon and project description; add exclusion-tag translations.
- Add actual server checks for breakage, anvil repair, exclusions, excess damage and restored item behavior, plus client startup checks.

## 1.0.1 — 2026-08-31

- Keeps the same broken-item behavior while routing the destruction boundary through the independently tested policy owner.
- Executes the common compatibility and warning-visual contracts in the release gate.

## 1.0.0 — 2026-08-26

- Promotes the destruction-boundary durability contract, GUI-only warning tint, empty default exclusion policy and six supported Fabric/NeoForge anchors to the stable public API.

## 0.1.3 — 2026-08-26

- Preserve the upstream durability pipeline and cancel only the final stack-shrink operation.
- Remove all non-GUI broken-item model tinting.
- Replace the dark inventory overlay with a light translucent warning red.
- Keep the default exclusion tag empty; modpacks continue to own custom lifecycle exclusions.
- Continue blocking broken-item inventory ticks by design.
