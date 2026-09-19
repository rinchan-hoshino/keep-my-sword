# Validation — 1.1.0-dev

Local verification on macOS, 2026-09-19. This build is **unpublished**.

**Release assessment:** these are targeted test results, not approval of the
architecture. The [identity and save-safety review](identity-safety-review.md)
identifies an unresolved persistence boundary; this experiment is not recommended
for release under an unrestricted uninstall-safety requirement.

| Minecraft | Loader | Save / uninstall / reinstall assertions | Client world and rendering |
| --- | --- | --- | --- |
| 1.21.1 | fabric | 63 / 15 / 14 — passed | Passed |
| 1.21.1 | neoforge | 59 / 13 / 11 — passed | Passed |
| 26.1.2 | fabric | 64 / 15 / 14 — passed | Passed |
| 26.1.2 | neoforge | 64 / 15 / 14 — passed | Passed |
| 26.2 | fabric | 64 / 15 / 14 — passed | Passed |
| 26.2 | neoforge | 64 / 15 / 14 — passed | Passed |

All 18 server phases passed, totaling **547 assertions**. All six clients loaded isolated worlds, rendered the broken-item fixtures and wrote screenshots. All six development JARs built; the common unit tests passed (6 executions).

## Packaged installation

Normal Fabric servers for all three versions started with only the built Keep My Sword JAR and matching RinLib in `mods`. Each created a zero-durability sword, saved and stopped successfully. No separate Fabric API or Transfer API was installed. The tested JAR hashes match the final build manifest.

Fabric JARs contain exactly three bundled modules: API base, networking and registry sync. All six JARs contain the approved icon and exclude runtime probes/test items.

## Coverage

- Real final-durability loss, direct component/setter updates and exhausted-stack construction.
- Inert runtime identity, custom active/passive/attack callbacks, armor attributes and shield use.
- Copy, split, reconstruction, network transfer and immutable original snapshots.
- Actual anvil consumption/experience, rename followed by another break, crafting repair and grindstone consumption.
- Exclusion tags and naturally expiring preexisting effects.
- Exact native data and NBT type equality after uninstall/reinstall, including nested containers, independent shell metadata, unknown annotations and old snapshot migration.
- Fabric Transfer and modern NeoForge ItemResource storage; Minecraft 26.x templates, including template application.

## Boundaries

These results validate the named storage and repair interfaces. They do not establish universal uninstall safety for a third-party mod which manually serializes an item identity/components outside those interfaces. Custom repair APIs need an explicit adapter. No publication or production save migration was performed.

## Reproduction and evidence

See [runtime test commands](../runtime-tests/README.md), [implementation design](sealed-items.md), and [machine-readable results and hashes](validation.json). Raw logs and screenshots live in the local evidence directory recorded in that JSON. Historical 1.0.2 results are kept separately.
