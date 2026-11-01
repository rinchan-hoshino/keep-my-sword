# Broken-item implementation

Unpublished 1.1.0 development build. This replaces the earlier custom-registry prototype. The approved architecture is whole-item isolation with an opaque snapshot in a vanilla carrier; per-effect compatibility patches were rejected.

## Runtime isolation

A managed item becomes a native recovery compass when native break processing would consume it. Its original registry identity and full persistent components are encoded privately inside `minecraft:custom_data`; the original is not a live nested container item. Normal gameplay sees the carrier, so the original item's use, attack, inventory callbacks, attributes and enchantments stop. The entity's native equipment state is refreshed immediately when an equipped item breaks. Existing timed effects are left to expire.

RinLib supplies durability and exclusion-adjacent state checks through `DamageState`. The snapshot format, transition, protection and repair rules are common code. Small adapters handle changed Minecraft API types, render methods and loader-specific damage/anvil signatures. Both 26.x targets use the same core and version adapter.

Only native break processing triggers the transition. The 1.21.1 adapters intercept immediately before `shrink(1)`. In 26.x, `hurtAndBreak` delegates to a level-less private `applyDamage`; its adapter retains the level on that stack only during the native call, clears it in finally, and intercepts immediately before shrink. This preserves the native durability advancement, damage update and break predicate without copying Minecraft code. Fabric's player-only lower overload drops non-player owners, so its equipment caller refreshes mobs separately; NeoForge passes LivingEntity directly. Vanilla elytra flight stops at one durability remaining and never reaches destruction: flight alone does not seal it. Exhausted-stack construction, direct damage setters, component patches, decoding and repair calculations do not trigger live conversion. The break hook uses its ServerLevel's registry access; repair entry points pass their own registries and client rendering uses Minecraft's current level. Excluded items keep vanilla behavior.

## Native persistence

No custom item or component type is registered. The recovery compass and its native data components are saved directly by Minecraft, independent serializers and platform storage APIs. There is no identity substitution during saving and no loader-specific storage adapter.

`keep_my_sword:sealed` inside native custom data stores the CompoundTag produced by ItemStack.CODEC directly, with a `DataVersion` field. A count-one original snapshot is paired with the actual carrier count, so copying/splitting does not create a second independent item. New snapshots have no compression, Base64, chunks, size budget or readback verification. A codec failure cancels destruction and retains the original.

Decoding uses Minecraft's item data fixer for older snapshots. `CarrierSnapshot` reads only native snapshots with `DataVersion`; `LegacyCarrierMigration` handles schema-1 chunked Base64 envelopes. The shared `SealedItems` read path immediately rewrites a successfully decoded legacy carrier's `keep_my_sword:sealed` payload in native format on the server, preserving its other custom data and carrier components. Subsequent reads, including uncached copies, use the native decoder. Clients decode legacy carriers for rendering and tooltips without writing them. Future versions, missing original registrations and malformed data remain sealed with their payload unchanged. A transient per-stack cache avoids repeated decoding during rendering; callers receive private copies, never the cached original. Recipe matching primes those copies with the matching level before vanilla crafting assembly; grindstones use the player's level.

Native NBT storage preserves exact numeric and array types. JSON-based serializers preserve the JSON representation, but JSON converts NBT byte/int/long arrays to lists and does not preserve exact NBT types. Removing the encoded envelope intentionally gives up that guarantee.

## Accidental-consumption protection

A recovery compass has no native use action and is not consumed by vanilla recipes. While the mod is installed, marked carriers additionally fail normal ingredient matching and non-repair recipe-manager results. Ordinary custom-data edits can remove or replace the snapshot; there is no metadata mutation interception. Unrecognized snapshots get the same ingredient protection.

Inventory moves, copies, splitting and ordinary dropping remain normal operations; count setters are not globally blocked, which would break transfers and risk duplication. This is not item-loss immunity: destruction in lava, deliberate deletion, or third-party systems that discard native item metadata are not prevented.

## Repair and appearance

- Anvils calculate on private originals while preserving the real input slots and vanilla material/XP transactions. Renaming followed by another break preserves the new name.
- Crafting repair uses private originals and vanilla output rules. Non-curse enchantments/data discarded by native crafting repair are not added back artificially.
- Grindstones validate private originals, repair/disenchant with native rules, and consume inputs only through the native transaction.
- Invalid snapshots produce no repair result; input carriers and materials remain untouched.
- Mending does not act on the carrier: the original enchantments are inactive while broken.
- Rendering and tooltips display a private original copy and the broken warning. Unreadable snapshots fall back to the carrier's native display instead of crashing.
- The private snapshot is authoritative for repair. Carrier-only visual/help metadata is not mistaken for original gameplay components.

## Uninstall and recovery

Uninstalling Keep My Sword leaves a recovery compass with the original item's name, a plain fallback tooltip explaining how to repair it, and the complete saved snapshot. It does not automatically become the original sword.

Reinstalling recognizes that carrier without a storage migration. Its original appearance and supported repairs become available again. A successful repair produces the original item through the native transaction; a failed decode leaves the carrier in place. Uninstalling the mod that defines the original item may prevent recovery until that mod is restored, while the opaque snapshot itself remains retained.

The previous registry-backed prototype was never released and is not a supported public save format. This change does not publish artifacts or migrate player worlds.

## Verification

See [current validation](validation.md) and [reproduction commands](../runtime-tests/README.md). The carrier tests use fresh worlds and actual save/uninstall/reinstall cycles; the original prototype's validation is historical evidence, not evidence for this implementation.
