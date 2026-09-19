# Supported versions

| Minecraft | Fabric | NeoForge | Java |
| --- | --- | --- | --- |
| 1.21.1 | Yes | Yes | 21 |
| 26.1.2 | Yes | Yes | 25 |
| 26.2 | Yes | Yes | 25 |

`profiles.json` selects the loader versions and Minecraft API adapter. The two 26.x versions share the same source. All profiles use RinLib for durability state and clamping, with one shared gameplay implementation. Small API adapters bind the version-specific tag, use-result, attribute-group, rendering and destruction methods.

Historical port branches are development history, not additional supported releases.
