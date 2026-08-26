# Development

Use quick compilation, static inspection and focused pure unit tests. Do not launch a Minecraft client, dedicated server, full-pack server, temporary server, GameTest runtime or staged distribution without explicit authorization.

## Invariants

- Preserve the full upstream durability calculation and intervene only immediately before `ItemStack.shrink(1)` destroys a managed stack.
- Leave `#keep_my_sword:excluded` empty in the mod; modpacks own additions to that tag.
- Keep broken-item behavior suppression on standard ItemStack hooks. `inventoryTick` remains blocked by product decision.
- Render the warning only in inventory-like GUI slots. Never recolor hand-held, dropped, displayed or world models.
- Keep the GUI warning light enough that the original icon, count and durability bar remain readable.
