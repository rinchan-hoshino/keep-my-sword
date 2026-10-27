# Keep My Sword

<img src="docs/artwork/icon.png" alt="Keep My Sword" width="96" height="96">

Keep My Sword prevents items from disappearing when their durability runs out. Broken items remain in your inventory in an unusable state and can still be repaired.

Items in the `#keep_my_sword:excluded` item tag are ignored by this mod.

## Configuration

Starting in 1.2.0 (Minecraft 1.21.1 / NeoForge), configuration is available at `config/keep_my_sword.toml` and loaded on game startup (restart the game after editing):

```toml
# List of item IDs or item tags excluded from broken-item protection.
# Supports item IDs ("minecraft:diamond_sword") and item tags ("#slashblade:swords").
excludedItems = []
```

## Requirements

- [RinLib](https://modrinth.com/mod/rinlib)

## Screenshots

![Holding a diamond sword with no durability remaining](https://cdn.modrinth.com/data/pPO0fpz4/images/e0971797aee78a779a76036dd26df0320808efc2.png)

![Repairing the diamond sword in an anvil](https://cdn.modrinth.com/data/pPO0fpz4/images/8b706cf65c468aa224bccbdf6b2e49eab90d7d08.png)
