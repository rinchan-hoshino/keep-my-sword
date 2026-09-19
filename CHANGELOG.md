# Changelog

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
