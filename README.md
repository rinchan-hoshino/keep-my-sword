# Keep My Sword

<img src="docs/artwork/icon.png" alt="Keep My Sword" width="96" height="96">

Items stay in your inventory after they break. A broken tool or weapon is kept in an unusable state and can still be repaired.

![Holding a diamond sword with no durability remaining](https://cdn.modrinth.com/data/pPO0fpz4/images/e0971797aee78a779a76036dd26df0320808efc2.png)

![Repairing the diamond sword in an anvil](https://cdn.modrinth.com/data/pPO0fpz4/images/8b706cf65c468aa224bccbdf6b2e49eab90d7d08.png)

## How it works

- A broken item stays in your inventory with its original data kept safe. It cannot be used as a normal recipe ingredient.
- Repair it at an anvil, grindstone or Enchanting Infuser. The repair uses the item's exact original data.
- Broken items stay sealed when moved into chests, hoppers, shulker boxes and other storage.
- Items in the `#keep_my_sword:excluded` tag, or listed in the config, are not protected.

## Configuration

The file is `config/keep_my_sword.toml`. Restart the game after editing it.

| Key | Default | Effect |
| --- | --- | --- |
| `excludedItems` | `[]` | Item IDs (`"minecraft:diamond_sword"`) or item tags (`"#slashblade:swords"`) that are not protected. |
| `repairViewExcludedMenus` | Chests, shulker boxes, hoppers, dispensers, Sophisticated Backpacks and Sophisticated Storage | Menu IDs or class names that keep items sealed while the menu is open. Add modded storage menus here. |

## Compatibility

- Minecraft 1.21.1
- Fabric and NeoForge
- Enchanting Infuser repair: supported from 1.3.0.

## Download

- [Modrinth](https://modrinth.com/mod/keep-my-sword)
- [GitHub releases](https://github.com/rinchan-hoshino/keep-my-sword/releases)
