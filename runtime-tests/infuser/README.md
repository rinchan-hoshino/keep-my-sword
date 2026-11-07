# Enchanting Infuser repair regression

Use Java 21 and Minecraft 1.21.1 / NeoForge 21.1.250. Supply a directory containing
Enchanting Infuser 21.1.4 and Puzzles Lib 21.1.66 from Modrinth, plus an existing
accepted EULA file. The probe is a development mod and is never packaged.

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
python3 runtime-tests/infuser/run.py --mods /path/to/infuser-jars \
  --eula /path/to/eula.txt --output /path/to/fresh-baseline --baseline
python3 runtime-tests/infuser/run.py --mods /path/to/infuser-jars \
  --eula /path/to/eula.txt --output /path/to/fresh-with-kms
```

Both runs create the same named, enchanted diamond sword with custom data and a
prior work penalty of 3. Baseline supplies it at damage 1561; the Keep My Sword
case reaches that state through a real break event. Compare `exhausted-sword.snbt`
between runs to verify identical original item data. A real server-side Advanced
Infuser menu receives the item, calculates the price, and processes the repair
button. Successful repair must restore the exact item data at zero damage and
charge eight levels. The unfixed mod fails at `advanced infuser accepts exhausted
sword`; its repair cost is zero.

Additional checks cover insufficient experience, unchanged sealed input during
previews, disabled enchanting of broken items, configured prior work penalties,
normal Infuser repair restrictions, unknown snapshot protection and ordinary
damaged swords. `infuser.pass` and a successful Gradle exit are both required.
The existing `runtime-tests/sealed-matrix.py` remains the regression suite for
break protection, anvil/crafting/grindstone repair and persistence.

The optional mixin uses the existing private repair snapshot accessor. Infuser
continues to own eligibility, pricing and the repair transaction; the live slot
keeps its carrier until repair succeeds. Enchanting a sealed carrier remains
disabled so it cannot replace the carrier with an unrepaired active item.

## Verification, 2026-11-07

| Run | Result |
| --- | --- |
| Infuser + Puzzles Lib, without Keep My Sword or RinLib | PASS: exhausted sword repaired for 8 levels |
| Keep My Sword 1.2.1 (`4e2b94b`) + Infuser | Expected FAIL: `canRepair=false`, repair cost 0 |
| Keep My Sword 1.2.2 + Infuser, NeoForge | PASS: all 19 assertions, including exact item data, no duplication and prior work penalty 3 → 7 |
| Keep My Sword 1.2.2 + Infuser, Fabric | PASS: the same 19 assertions with a Fabric lifecycle entrypoint |
| Six supported build targets | PASS; all 36 unit tests passed; no probe classes in release JARs |
| Original save/unload/reinstall suite across all six targets | PASS: 18 phases, 897 assertions |

Baseline, failing and fixed runs used identical original sword data, SHA-256
`845bde22c11f725c713a19695c66fc1719968ea4c4c6cc18380c13e0a46fcbfc`.
These are real dedicated-server menu transactions, not manual GUI playthroughs.
Infuser compatibility on Minecraft 26.x is outside this fix.

Fabric verification used Loader 0.19.5, Fabric API 0.116.12+1.21.1 and Forge Config
API Port 21.1.6. Puzzles Lib 21.1.66 requires Loader ≥0.19.4; its static mixin
metadata also requires a newer Loom for development remapping. The Fabric probe
used locally available Gradle 9.5.1 / Loom 1.17.21 through a temporary init script.
The normal six-target build used the repository's Gradle 9.2.1 / Loom 1.14.10.

Upstream repair checks: [InfuserMenu](https://github.com/Fuzss/enchanting-infuser/blob/ff3921953ce4cf1294e9cd502aca69adfe0c6e5b/common/src/main/java/fuzs/enchantinginfuser/world/inventory/InfuserMenu.java)
and [AllowedRepairItems](https://github.com/Fuzss/enchanting-infuser/blob/ff3921953ce4cf1294e9cd502aca69adfe0c6e5b/common/src/main/java/fuzs/enchantinginfuser/config/AllowedRepairItems.java).

## Generic view in 1.3.0

Open Infuser menus now receive live originals through the shared menu boundary.
The probe opens its repair menus and expects exact exposed-input previews; it
still checks the sealed-input enchanting guard before opening that extra-check
menu. The optional mixin no longer supplies an Infuser-specific repair accessor.
Use the [generic repair suite](../repair-view/README.md) for native clicks,
enchantment changes followed by resealing, machine close, and another repair mod.
The table above records the earlier 1.2.2 behavior.
