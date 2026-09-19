# Identity and save-safety review

Review date: 2026-09-19. Reviewed implementation: `c05a9fe`.

## Decision

Keep the sealed-item implementation as an unpublished experiment. Passing its
targeted tests does not satisfy an unrestricted uninstall-safety requirement.
Do not describe it as safe for every third-party storage implementation.

The owner rejected behavior-by-behavior suppression: it creates an unbounded
compatibility task and does not meet the intended isolation requirement. That
proposal below is retained as a rejected alternative, not an implementation plan.
The follow-up [vanilla carrier research](vanilla-carrier-research.md) investigates
whole-item isolation without introducing a missing registry identity on uninstall.

## Evidence

The current `StackReplacementMixin` replaces both the item field and component map.
`StorageBoundary` converts them back only at covered serialization boundaries.
The runtime probe already records `ALTERNATE_SERIALIZER_RISK` when the unwrapped
codec writes `broken_item`. This observation is present in the final Fabric
1.21.1, 26.1.2 and 26.2 logs and the final NeoForge 26.1.2 and 26.2 logs. It is a
demonstration of a bypass, not evidence that a particular public storage mod is broken.

The locally cached 1.21.1 NeoForge Minecraft source uses `getItemHolder()` for
the native codec's identity and `components.asPatch()` for its component payload.
Gameplay methods also use `getItem()` to dispatch item behavior. Its enchantment
helper reads enchantments separately through component queries.

The locally cached Fabric Transfer API 5.4.4+7b3d111d19 source constructs an
`ItemVariant` from the stack's item and component patch. It caches this immutable
representation independently of the original stack. Consequently a query-time
disguise can escape the stack before any later save hook runs.

Inspection of the local Minecraft 26.2 bytecode confirms that `getItem()` resolves
`typeHolder()` and that `inventoryTick()` calls `getItem().inventoryTick(...)`.
These checks establish concrete shared query paths, not exhaustive compatibility
coverage of all supported versions.

Upstream references:

- [NeoForge items and stacks](https://docs.neoforged.net/docs/items/)
- [Fabric Transfer item storage](https://wiki.fabricmc.net/tutorial:transfer-api_item_storage)

## Alternatives assessed

| Approach | Result |
| --- | --- |
| Actual replacement plus save adapters | Strong isolation of original item callbacks, but unknown serializers may persist replacement identity. Current experiment. |
| Keep fields, globally return an inert item from getters | Storage callers can observe and retain the same disguise. Preserving a private field alone does not preserve the public persistence contract. |
| Hide all gameplay components in getters | Risks losing hidden data through serializers that read those getters; also requires separating repair, rendering and gameplay reads. |
| Report empty stacks or zero count while broken | Makes normal inventory management and serialization treat existing items as absent. Reject. |
| Use vanilla air/barrier as the replacement | Avoids a missing custom registry entry, but still replaces the original item. An opaque backup is not automatic restoration after uninstall. Reject. |
| Temporarily swap identity during known callbacks | Does not cover independent scanners; reentrant callbacks can save or copy the temporary state. Reject as a universal fix. |
| Preserve identity and data; suppress effect execution | Rejected by the owner: individual effect integrations do not meet the core requirement. |

There is no caller-purpose parameter on the ordinary identity/component queries.
A generic hook cannot infer whether an arbitrary caller is inspecting an item to
save it or to grant an effect. This rules out treating getter disguises as a
general solution; it is not a proof that every possible integration is impossible.

## Rejected behavior-first replacement

1. Keep the original registry identity, component map, count and normal codecs.
   Derive the disabled state from durability and the exclusion tag via RinLib;
   avoid a new persisted state flag or hidden original snapshot.
2. Preserve the stack at final durability loss without calling the destructive
   count decrement. Keep loader/version-specific damage semantics explicit.
3. Suppress execution of use, attacks, tool actions and inventory callbacks for
   disabled stacks. Leave identity/component queries truthful.
4. Filter disabled equipment from attribute and enchantment evaluation. Remove
   already-installed equipment attribute modifiers on transition; merely skipping
   future callbacks is insufficient. Leave already-applied timed effects alone.
5. Cover additional native component-driven behaviors (food, blocking, flight,
   projectiles and equipment effects) through their execution paths. Do not claim
   complete native coverage before a version-specific inventory and tests exist.
6. Keep native repair, copy, rendering and save operations operating on the real
   item. Validate repair restores behavior without a separate state reset.
7. Treat mod-owned player-tick scanners and custom effect dispatch as explicit
   compatibility work. They remain the unresolved scope decision.

## Required acceptance checks for that replacement

- Compare full native data, including exact NBT types, before and after breaking,
  saving, uninstalling and reinstalling. Only expected durability/count changes
  are allowed.
- Add a deliberately independent serializer that writes item identity and the
  complete component patch directly, then read it with Keep My Sword absent.
- Preserve the existing active/passive probe and add an external player-tick
  scanner to demonstrate the actual boundary rather than hiding it.
- Exercise equipped-item break transitions, attribute removal, enchantment
  callbacks, use-in-progress, repair/reactivation and excluded items.
- Run all six loader/version targets after implementation. The sealed prototype's
  547 assertions must not be reused as evidence for a different implementation.

This review changes documentation only. Production code and existing artifacts
remain unchanged; no publication or player-save operation occurred.
