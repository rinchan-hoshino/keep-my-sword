# Keep My Sword

Keep My Sword keeps broken tools and equipment as repairable model-only items instead of destroying them.

## Behavior

- Damageable items stay in their slot when durability reaches zero.
- The mod leaves the normal durability pipeline intact and intervenes only when Minecraft is about to shrink the broken stack.
- A zero-durability item remains movable, droppable and repairable.
- Standard broken-item gameplay hooks are disabled: use actions, special mining speed, correct-tool harvest, item attack hooks, inventory ticking and equipment attribute modifiers.
- Broken items do not add tooltip text.
- Inventory-like GUI slots show a light translucent red warning overlay.
- Hand-held, dropped, displayed and other non-GUI models keep their original colors.

Blocking `inventoryTick` intentionally prevents passive broken-item behavior. Third-party repair systems that require the item's own inventory tick cannot repair it until another mechanism first lowers its damage.

## Compatibility boundary

Keep My Sword modifies standard `ItemStack` boundaries instead of maintaining per-mod integrations. Durability enchantments, criteria and loader or mod adjustments that happen before vanilla stack destruction remain in control of their producer.

A modded item that owns a custom break, replacement or transformation lifecycle must opt out through the data-driven `#keep_my_sword:excluded` item tag. Excluded items bypass preservation, behavior suppression and rendering completely.

The default tag is empty. Modpacks own their compatibility policy:

```json
{
  "replace": false,
  "values": ["#examplemod:special_swords"]
}
```

No generic mod can detect every third-party callback or event outside the standard ItemStack contract. The exclusion tag is the explicit escape boundary for those non-standard lifecycles.

## Dependency

Keep My Sword depends on RinLib.

## Supported targets

The currently published line is Minecraft 1.21.1 for Fabric and NeoForge.

The broader port plan is tracked in [docs/version-support.md](docs/version-support.md); planned rows are not published support.

## Build

Publish the matching RinLib source locally first, then run the focused loader build required for the target branch. See [DEVELOPMENT.md](DEVELOPMENT.md).
