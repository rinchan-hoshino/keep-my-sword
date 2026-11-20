# Development

Keep My Sword 2.0.0 has zero runtime dependencies and builds 8 targets across 4 Minecraft versions (`1.20.1`, `1.21.1`, `26.1.2`, `26.2`).

```sh
# Build default profile (1.21.1 Fabric + NeoForge) and run unit tests
./gradlew build

# Build and verify all 8 targets (<120KB budget, zero rinlib classes)
python3 scripts/build-matrix.py

# Regenerate the 512x512 icon from the 64x64 pixel-art script
python3 scripts/draw_icon.py
```

## Layout

- `common/src/main/java` — version-agnostic config (`SimpleToml`, `ExclusionMatcher`, `KeepMySwordConfig`), pure durability invariants (`DurabilityPolicy`), badge style (`BrokenVisualStyle`), and shared `EquipmentRefreshMixin`.
- `common/src/v120/` — thin `1.20.1` adapter and Mixins (NBT, `getAttributeModifiers`, `BlockState.use` hand masking, Forge `onArmorTick`).
- `common/src/v121/` — thin `1.21.1` adapter and Mixins (`DataComponents`, `forEachModifier`, data-driven `EnchantmentHelper`, legacy v1.x unsealer).
- `common/src/v26/` — thin `26.1.2` / `26.2` adapter and Mixins (`Identifier`, `GuiGraphicsExtractor`, `DataComponents.GLIDER`).
- `common/src/test/java` — JUnit 5 unit tests (config/TOML, randomized durability state-machine invariants, badge bounds, legacy unsealer).
- `fabric/`, `forge/`, `neoforge/` — minimal loader entrypoints, config screen bindings, and mod metadata.
