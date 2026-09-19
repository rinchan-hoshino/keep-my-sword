# Broken-item implementation

This is an unpublished 1.1.0 development build. Runtime validation is recorded separately from release approval.

The subsequent [identity and save-safety review](identity-safety-review.md) keeps
this implementation experimental. Covered storage tests do not establish safety
for arbitrary serializers. The owner rejected a behavior-first replacement;
[vanilla carrier research](vanilla-carrier-research.md) examines a different
persistence representation while retaining whole-item isolation.

## Runtime behavior

A managed item with no durability becomes a real inert item. Its original identity, components and count are kept in a private snapshot; normal item queries expose the inert item. This prevents further original-item use, attack callbacks, inventory ticks, attributes and enchantments. Existing timed effects are left to expire normally.

The transition covers normal break processing, component mutations and construction from an exhausted component patch. Native break transactions and private decoding/repair calculations finish inside a scoped snapshot context, so intermediate copies cannot be mistaken for live equipment. Excluded items retain vanilla break behavior.

RinLib supplies the shared durability rules. The sealing, snapshot ownership, storage envelope and mutation logic are shared by all six targets. Small adapters handle changed Minecraft field types, names, codec entry points and rendering APIs.

## Persistence

The normal disk form uses the original item's registry identity and components. An unmodified sealed item needs no custom storage envelope. Independent edits to the inert item are stored under a namespaced annotation in vanilla custom data, without overwriting original custom data.

The annotation records its schema and Minecraft data version. Schema 3 distinguishes an inherited display name from an independently edited shell name, so anvil renaming survives another break. Previous schemas remain readable. Unknown future or malformed annotations remain opaque and recoverable. Shell snapshots retain exact NBT types through SNBT, and Minecraft's data fixer upgrades their item representation.

Storage boundaries covered:

- Native `ItemStack` disk codecs, including optional/single forms on 1.21.1 and map codecs on 26.x.
- `ItemStackTemplate` disk storage on 26.x, used by nested containers. Templates retain their deferred creation lifecycle and transient registry context.
- NeoForge 26.x `ItemResource` and its optional disk codec. Native stack handlers use the covered item codecs.
- Fabric Transfer `ItemVariant` disk storage, using its native schema. This integration is optional. Fabric builds bundle only the base, networking and registry-sync modules needed for item registration; a separate full Fabric API install is not required.

Network snapshots retain the inert runtime identity. Decoding the private original payload does not reactivate it.

The original codec is deliberately internal. A third-party serializer which bypasses the covered boundaries and manually writes registry IDs/components can persist the inert identity. Uninstall safety cannot be promised for every such custom format. Removing or corrupting the private original component is also outside normal supported mutation.

## Repair and appearance

- Anvils calculate against private originals; real input slots remain sealed. Vanilla owns material consumption, experience costs and output collection.
- Crafting repair uses the original inputs and vanilla output rules, including vanilla's normal loss of non-curse enchantments and other discarded data.
- Grindstones accept sealed inputs, preserve the native repair/disenchantment transaction and calculate experience from original enchantments.
- Mending does not run while broken: it is a passive effect, and the requested behavior disables new passive effects.
- Client rendering receives a display copy of the original. Gameplay getters do not expose that copy. Broken items retain the red inventory warning and a localized broken-item tooltip.

Custom repair systems need an explicit repair integration; exposing original gameplay components globally would undo the inert-item guarantee.

## Verification

`runtime-tests/sealed-matrix.py` runs fresh isolated worlds through save, real uninstall, and reinstall phases. It checks full native data equality (including NBT numeric/array types), nested containers, network/copy/split behavior, real break events, active/passive callback controls, repair transactions, direct mutation, constructor transitions and exclusion tags.

`runtime-tests/run.py --side client` renders healthy and broken items and records an actual screenshot. These are local development checks; they do not publish or modify a player's world.
