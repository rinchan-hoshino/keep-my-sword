# Keep My Sword 2.0.0 — Feature Map

Zero-dependency, multi-version in-place broken durability engine for Minecraft.

## Entry Points

| Layer / Loader | Entry Class | Notes |
| --- | --- | --- |
| Common | `common/src/main/java/dev/rinchan/keepmysword/KeepMySword.java` | Mod ID (`keep_my_sword`), logger, config bootstrap |
| Fabric | `fabric/src/main/java/dev/rinchan/keepmysword/fabric/KeepMySwordFabric.java` | Initializes common config |
| Fabric ModMenu | `fabric/src/main/java/dev/rinchan/keepmysword/fabric/client/KeepMySwordModMenu.java` | Compile-time `modmenuStub`, zero runtime dependency |
| Forge (1.20.1) | `forge/src/main/java/dev/rinchan/keepmysword/forge/KeepMySwordForge.java` | Common init + dist-isolated `client/KeepMySwordForgeClient` |
| NeoForge (1.21.1+) | `neoforge/src/main/java/dev/rinchan/keepmysword/neoforge/KeepMySwordNeoForge.java` | Common init + dist-isolated `client/KeepMySwordNeoForgeClient` |

## Core Policy & Config (`common/src/main/java`)

- `config/KeepMySwordConfig.java`, `config/SimpleToml.java`, `config/ExclusionMatcher.java`: Zero-dependency TOML config (`allowMendingWhenBroken`, `showBreakNotification`, `showBrokenOverlay`, `excludedItems`).
- `state/DurabilityPolicy.java`: Pure state machine (`isBroken`, `evaluateHurt`) with Elytra exemption and strictly once-per-break transition.
- `state/BrokenVisualStyle.java`: 5×5 bottom-right broken-item crack badge constants.
- `mixin/EquipmentRefreshMixin.java` & `state/EquipmentRefresh.java`: Forces attribute/equipment refresh on first break.

## The 6 Engine Roots (`common/src/{v120,v121,v26}`)

1. **Root #1 — Destruction Prevention & First-Break Feedback**
   - `state/BrokenState.java`: Classifies protectable/broken/excluded/Elytra items; `onFirstBreak` plays break sound/particles, stops active item use (`stopUsingItem`), refreshes equipment, and sends actionbar/overlay notification.
   - `mixin/ItemStackMixin.java`: Clamps `hurtAndBreak` at `maxDamage`, short-circuits already-broken items, and in `v120` guards direct `ItemStack.hurt` callers (e.g. dispensers) via `ThreadLocal` so item count is never zeroed.
2. **Root #2 — Right-Click Use & Interaction Gates**
   - `mixin/ServerPlayerGameModeMixin.java` & `mixin/MultiPlayerGameModeMixin.java`: Blocks broken item use while allowing bare-hand block menu interactions (`v120` uses `state/HandMask.java` + `mixin/LivingEntityHandMaskMixin.java`).
   - `mixin/ItemStackMixin.java`: Gates `use`, `useOn`, `interactLivingEntity`, and `onUseTick` (stops continuous use) for direct/mod/FakePlayer callers.
3. **Root #3 — Melee Attack Neutralization**
   - `mixin/PlayerMixin.java` & `mixin/ItemStackMixin.java`: Strips broken weapon attack damage/sweep/crit and blocks `hurtEnemy` / `postHurtEnemy`.
4. **Root #4 — Mining Speed & Drops Gate**
   - `mixin/ItemStackMixin.java`: Resets `getDestroySpeed` to `1.0F`, `isCorrectToolForDrops` to `false`, and cancels `mineBlock`.
5. **Root #5 — Attributes, Passive Ticks & Crafting/Anvil/Grindstone**
   - `mixin/ItemStackMixin.java`: Clears slot/group attribute modifiers and cancels `inventoryTick` (plus `onArmorTick` in `v120`).
   - `mixin/IngredientProtectionMixin.java` & `mixin/RecipeProtectionMixin.java`: Prevents broken gear from being consumed in crafting recipes while keeping repair recipes working.
6. **Root #6 — Enchantment Suppression & Mending Gate**
   - `mixin/EnchantmentHelperMixin.java`: Suppresses combat/protection/movement enchantments on broken items across `getItemEnchantmentLevel`, `getEnchantmentLevel`, `runIterationOnItem`, and `getRandomItemWith`, while allowing Mending only when `allowMendingWhenBroken = true`.

## UI & Legacy Migration

- `client/KeepMySwordConfigScreen.java` & `mixin/GuiGraphicsMixin.java`: Native zero-dependency config screen and 5×5 broken badge overlay.
- `migration/LegacyCarrierUnsealer.java` & `mixin/LegacyMigrationPlayerMixin.java`: Automatically unseals legacy `keep_my_sword:sealed` recovery-compass carriers (native v1.2+ and schema-1 compressed payloads) on player inventory tick.

## 8-Target Build Matrix (`profiles.json`)

| Minecraft | Java | API Layer | Loaders |
| --- | --- | --- | --- |
| `1.20.1` | 17 | `v120` | `fabric`, `forge` |
| `1.21.1` | 21 | `v121` | `fabric`, `neoforge` |
| `26.1.2` | 25 | `v26` | `fabric`, `neoforge` |
| `26.2` | 25 | `v26` | `fabric`, `neoforge` |

## Verification Suites & Local Gates

- **Gate 1 — Packaged-JAR Black-Box E2E (`neoforge/src/gametest/.../KeepMySwordE2EGameTests.java` + `neoforge/src/test/.../KeepMySwordConfigScreenTest.java`)**:
  Stages the built `keep_my_sword-neoforge-2.0.0+1.21.1.jar` into `run-gametest-e2e/mods/` and runs real headless Minecraft `1.21.1` NeoForge GameTests (1-durability sword strike → broken/sealed damage neutralization → `AnvilMenu` repair & unseal, continuous-use shield/bow auto `stopUsingItem`, null-owner dispenser `hurtAndBreak` protection) plus headless `KeepMySwordConfigScreen` widget instantiation & save verification.
- **Gate 2 — Property-Based Invariant Tests (`common/src/test/.../KeepMySwordPropertyTest.java`)**:
  `>= 1,500` fixed-seed randomized trials per property covering durability/damage/repair/exclusion invariants, TOML config round-trip losslessness, and `ExclusionMatcher` normalization.
- **Gate 3 — Architecture Contract Guardrails (`common/src/test/.../ArchitectureContractTest.java`)**:
  Scans the real packaged `.jar` bytecode constant pool for dedicated-server client isolation, bidirectional Mixin config/JAR/source closure, and clean artifact redlines (zero `rinlib`, zero `modmenu` stub leakage, `<120KB`).

## Build & Verification Commands

- Enable local `pre-push` hook (`.githooks/pre-push`):
  ```bash
  ./scripts/setup-hooks.sh
  ```
- Full 3-gate verification (native if JDK 17/21/25 installed, or reproducible container via `Dockerfile.verify`):
  ```bash
  ./scripts/verify.sh [--container]
  ```
- Full 8-target build + `<120KB`, zero-`rinlib`, zero-`modmenu` verification:
  ```bash
  python3 scripts/build-matrix.py
  ```
- Single-profile build (e.g. `1.21.1` or `26.1.2`):
  ```bash
  python3 scripts/build-matrix.py --profile 1.21.1
  ```
- Run 1.21.1 packaged-JAR GameTest E2E server directly:
  ```bash
  ./gradlew :neoforge:runGameTestServer -Pminecraft_version=1.21.1 -Ploader=neoforge --no-configuration-cache
  ```
- Regenerate 512×512 icon from 64×64 pixel art:
  ```bash
  python3 scripts/draw_icon.py
  ```
