# Changelog

## 2.0.3

- **Legacy carrier unseal hardening**: Automatically strips retired/unregistered `DataComponentType` keys (including `!` removal entries) on a copy of the upgraded item tag before `ItemStack.CODEC.parse` in `v121` and `v26`, logging a warning for each stripped component while leaving the original carrier untouched on failure.

## 2.0.2

- **1.20.1 Forge `pack.mcmeta` fix**: Added `pack.mcmeta` (`pack_format: 15`) to the Forge template resources to eliminate the `ResourcePackInfo` startup warning on Minecraft `1.20.1` Forge without affecting other targets.
- **Minecraft `26.3` support**: Added `26.3` profiles for Fabric and NeoForge, expanding the build matrix to 10 targets.
- **Verification**: Enforced `pack.mcmeta` JAR audit assertions and validated all 10 targets across the full verification suite.

## 2.0.1

- **Dedicated Server isolation**: Moved client-only config screen registration and client lifecycle classes into dedicated client packages (`KeepMySwordForgeClient`, `KeepMySwordNeoForgeClient`, `KeepMySwordModMenu`) so dedicated servers never load client GUI classes.
- **Verification**: Validated across the full build matrix, property-based invariant tests, packaged-JAR architecture contracts, and real Minecraft 1.21.1 NeoForge GameTest E2E.

## 2.0.0

- **In-place broken state engine**: Keeps broken tools, weapons, and armor in-place at 0 durability in a lightweight (~50–65 KB) standalone single JAR with zero external dependencies (not even Fabric API).
- **6 engine-root guards**:
  1. Destruction interception & one-time first-break sound, particles, and optional actionbar notice.
  2. Server & client right-click gating that blocks broken tool/weapon/entity use while falling back to bare-hand block interactions (so anvils, crafting tables, grindstones, and chests still open).
  3. Melee gating in `Player.attack` (bare-hand damage, no sweep or weapon hit hooks).
  4. Mining gating (`1.0F` speed, no tool drops, skipped `mineBlock`).
  5. Attribute & passive tick gating (zero armor/attack/mod modifiers; armor stays worn with 0 protection).
  6. Enchantment, Mending, and crafting-ingredient protection (blocks Protection/Thorns/Frost Walker/Looting/etc. and ordinary crafting while allowing `RepairItemRecipe` and configurable Mending).
- **Native in-game config screen**: Added a zero-dependency vanilla config screen wired to ModMenu (Fabric) and Forge/NeoForge config screen factories with instant hot-reload.
- **New visual polish**: Crisp 5×5 bottom-right broken badge replacing the full-slot red wash, dark-red tooltip line, `en_us`/`zh_cn`/`zh_tw`/`ja_jp` translations, and a redesigned 64×64 pixel-art icon.
- **Expanded compatibility**: Added Minecraft `1.20.1` (Fabric & Forge) alongside `1.21.1`, `26.1.2`, and `26.2` (Fabric & NeoForge), with `#slashblade:swords`, `#tconstruct:modifiable`, and `#tetra:modular_items` excluded by default.

## 1.3.0

- Anvils, Enchanting Infusers, repair stations and other repair menus can repair broken items. The exact original item is shown while the menu is open.
- A broken item is sealed again when you take it out, move it to another inventory, close the menu, disconnect, or drop it.
- Broken items stay sealed in chests, hoppers, dispensers, shulker boxes and other storage containers.
- The old Infuser-only repair hook is removed. Enchanting and anvil protection for sealed items still work as before.

## 1.2.2

- Fix Advanced Enchanting Infuser repair of sealed broken equipment on Minecraft 1.21.1.

## 1.2.1

- Bundled internal runtime helpers inside Keep My Sword, so no separate library download is needed.

## 1.2.0

- Add a config file, `config/keep_my_sword.toml`, read at startup (restart required after edits).
- Add `excludedItems` to exclude item IDs or item tags from protection. The default is an empty list.
- The `#keep_my_sword:excluded` item tag still works alongside the config file.
- Repair, anvil and item data behaviour is unchanged.

## 1.1.2

- Set the license to GPL-3.0-only in both loader files and include the `LICENSE` file in both jars.
- Correct the license on Modrinth and CurseForge.
- Release for Minecraft 1.21.1, 26.1.2 and 26.2 on Fabric and NeoForge. No gameplay changes.

## 1.1.1-wmf.1

- Avoid damage and enchantment lookups while decoding login inventory packets before the registry is ready.
- Keep the original item and its carrier format until the snapshot can be encoded.
- Add a regression test for item decoding without a scope. Verified on Minecraft 1.21.1 / NeoForge.

## 1.1.0

- Broken items are replaced by a carrier item that keeps the full original item. The carrier has no attributes or enchantments.
- Uninstalling the mod keeps the broken items' data. Reinstall to see and repair them again.
- Carriers cannot be used as ordinary recipe ingredients, and their stored data is kept when the item is edited.
- Anvil, crafting and grindstone repair work without reactivating the input items.
- If encoding or recovery fails, the original data is left intact.
- Custom item and component registration and loader-specific storage conversion are removed.

## 1.0.2

- Fix the item-destruction hook on Minecraft 26.1.2 and 26.2.
- Share gameplay logic across all six Fabric/NeoForge targets.
- Build Fabric 26.x with the native unobfuscated Loom pipeline.
- Remove the empty Fabric entrypoint, an unneeded Fabric API dependency and a redundant wrapper.
- Refresh the icon and description, and add exclusion-tag translations.
- Add server checks for breakage, anvil repair, exclusions and item behaviour after repair.

## 1.0.1

- Same broken-item behaviour, with the destruction boundary moved into its own tested policy.
- The compatibility and warning-visual checks run in the release gate.

## 1.0.0

- First stable release. Breakage boundary, GUI-only warning tint, an empty default exclusion list, and six supported Fabric/NeoForge targets.

## 0.1.3

- Keep the upstream durability pipeline and cancel only the final stack-shrink step.
- Remove all in-world broken-item tinting.
- Use a light translucent red overlay for broken items in the GUI.
- Keep the default exclusion tag empty.
- Keep blocking inventory ticks for broken items.
