# Whole-item isolation with a vanilla carrier

Research date: 2026-09-19. This is a feasibility experiment, not a replacement
release or a claim of universal compatibility.

## Conclusion

A runtime replacement need not introduce a custom registry identity. A vanilla
item can hold an opaque, versioned original-item snapshot in
`minecraft:custom_data`. Ordinary gameplay sees the carrier, without the original
item class, attributes, enchantments or mod-owned component keys. A faithful
serializer can persist that same representation without a special save adapter.
Minecraft can load the carrier and retain its data while Keep My Sword is absent.

This moves the uninstall tradeoff from **unknown item / potential loss** to
**known carrier / original data retained, recovery still needed**. Automatic
restoration after uninstall is not established. The preceding review mistakenly
treated automatic restoration as inseparable from data preservation; they are
different requirements. The owner has not accepted the carrier's uninstall UX.

The owner explicitly rejected per-effect compatibility patches. This candidate
retains whole-item replacement; it does not depend on discovering every original
item effect.

## Experiments

The research probe runs with Keep My Sword absent in both phases and asserts that
its classes cannot be loaded. A separate test-only item supplies a passive
callback control. There are no Keep My Sword serialization mixins in these runs.

| Minecraft | Loader | Save assertions | Reopen assertions | Result |
| --- | --- | --- | --- | --- |
| 1.21.1 | Fabric | 11 | 26 | Passed |
| 26.1.2 | Fabric | 11 | 26 | Passed |
| 26.2 | Fabric | 11 | 26 | Passed |

111 assertions across six server starts, including repeated checks across targets.
These are research checks, not the previous prototype's six-loader release matrix.
No client appearance, full repair transaction, installed-mod lifecycle or NeoForge
carrier integration is tested here.

### Native bundle

- A normal 1.21.1 world enables the bundle item; experimental recipe availability
  is not the same as item availability.
- A bundle with an exhausted sword survives a chest save and restart, including
  when nested in a shulker box.
- A deliberately independent codec writes item identity, count and component
  patch without calling the top-level `ItemStack.CODEC`. Its bundle also restores.
- Native inventory secondary-click extraction returns the exact sword, with name,
  enchantments and opaque custom data intact, including byte/int/long array types.
  The bundle becomes empty, so extraction does not duplicate the item.
- Ticking a bundle does not tick the contained test item. Original weapon
  attributes and enchantments are not active on the bundle itself.
- **Counterexample:** code that reads `BUNDLE_CONTENTS` can still identify the
  contained test item. A recursive inventory-effect scanner could therefore reach
  it. The bundle is not the preferred isolation representation.

### Opaque vanilla carrier

The proof uses paper with a maximum stack size of one. Its `custom_data` contains
a namespaced schema version, Minecraft data version and original snapshot encoded
as SNBT text. Paper is a convenient test fixture, **not a selected production
carrier**.

- The carrier has no `BUNDLE_CONTENTS` or `CONTAINER` and does not expose the
  original test item's identity to a direct item scanner.
- Ticking the carrier does not invoke the original item's callback.
- A real chest save/restart preserves the snapshot with Keep My Sword absent.
- Independent NBT and JSON serializers both retain the snapshot. Parsing it back
  produces exactly the original native NBT, including numeric and array types.
- The recovered sword can have its durability repaired while retaining its
  enchantments. This tests data reconstruction, not a completed anvil adapter.

The SNBT string avoids relying on JSON to preserve NBT numeric/array distinctions.
The experiment's helper unpacks the snapshot explicitly; vanilla does not
automatically restore a sword from this representation.

## Why this differs from the existing prototype

| Property | Existing sealed prototype | Opaque vanilla carrier candidate |
| --- | --- | --- |
| Runtime item | Custom `broken_item` | Existing vanilla item |
| Original payload | Custom registered component holding a stack | Opaque native custom data |
| Save representation | Converted back through selected save adapters | Same carrier and payload, no conversion required |
| Independent faithful serializer | Can persist an unknown custom identity | Persists a vanilla identity and vanilla component |
| Uninstall | Depends on which save adapter ran | Carrier remains loadable; original needs explicit recovery |
| Original effects | Original behavior removed from carrier | Original behavior removed from carrier |

No assertion here covers a serializer that discards native custom data or
deliberately rewrites items; such a serializer can also discard vanilla names,
enchantments or other ordinary item data. An external mod deliberately decoding
our private snapshot is likewise not prevented by a data format.

## Work required before recommending implementation

1. Select a vanilla carrier with minimal existing behavior and audit its use as a
   recipe input or other consumable. Paper can be consumed by ordinary recipes;
   it must not silently become the production choice. Do not repurpose or replace
   the global registered vanilla item object.
2. Define uninstall recovery honestly. While installed, rendering can show the
   original broken item and repair can restore it. While uninstalled, it remains
   a carrier. A tested recovery tool or reinstall path is necessary. No automatic
   restoration promise should be made.
3. Snapshot completely before replacing the live stack; keep the original intact
   on encoding failure. Avoid duplicate hidden copies or external save-side maps.
   Keep original data immutable and outer carrier annotations separate.
4. Keep schema and Minecraft data versions. Vanilla data fixing does not interpret
   opaque text inside custom data. Upgrade the original through the appropriate
   data fixer before decoding, and preserve unknown/future/invalid payloads
   without clearing them. Cross-version upgrades are not tested by this probe.
5. Address large payloads. A single SNBT string can exceed the NBT string encoding
   limit. A chunked textual encoding of binary NBT is a candidate; payload limits,
   network limits and decode validation need tests before choosing the format.
6. Scope private decoding so the zero-durability constructor hook does not reseal
   snapshots during display or repair. Reuse the shared core; adapt only version
   API differences through the existing architecture/RinLib where appropriate.
7. Validate attacks/use as well as ticking, attributes, exclusions, copy/split,
   component mutation, full repair transactions and true install/uninstall/
   reinstall on all six targets. Preserve the outer carrier atomically during
   recovery; do not delete it until decoding and destination insertion succeed.

## Evidence and reproduction

Source: `runtime-tests/carrier-research/RuntimeProbe.java` and `run.py`.

The final results are in the local evidence directory:

`../keep-my-sword-evidence/safety-research/vanilla-carrier-v2/results.json`

Set Java 21/25 homes, then run with a fresh output directory:

```sh
python3 runtime-tests/carrier-research/run.py \
  --output /path/to/fresh-carrier-results \
  --rinlib-dir /path/to/rinlib-jars \
  --eula /path/to/already-accepted-eula.txt
```

Upstream confirms that native custom data accepts arbitrary fields:
[Minecraft 1.20.5 item component specification](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-20-5).
Local 1.21.1 source also shows `V3818_3` treating bundle contents as nested item
stacks, unlike opaque custom data. The live experiment, rather than recipe-release
dates, establishes 1.21.1 bundle behavior above.

Only research probes and documentation changed. Production runtime code, release
artifacts and player worlds remain untouched.
