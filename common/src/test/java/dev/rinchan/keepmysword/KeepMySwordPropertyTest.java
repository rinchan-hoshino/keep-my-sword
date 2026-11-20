package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.*;

import dev.rinchan.keepmysword.config.ExclusionMatcher;
import dev.rinchan.keepmysword.config.KeepMySwordConfig;
import dev.rinchan.keepmysword.state.DurabilityPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Property-based randomized invariant tests (>= 1,000 fixed-seed iterations per property).
 */
class KeepMySwordPropertyTest {
    private static final int ITERATIONS = 1_500;

    @Test
    void durabilityProtectionNeverDestroysAndIsReversibleAcrossRepairSequences() {
        Random rng = new Random(0x5A17_2026_1120L);

        for (int trial = 0; trial < ITERATIONS; trial++) {
            final int trialIndex = trial;
            int maxDamage = 1 + rng.nextInt(10_000); // 1..10000
            boolean excluded = rng.nextInt(10) == 0;
            boolean elytra = !excluded && rng.nextInt(10) == 0;
            boolean protectable = !excluded && !elytra;
            int damage = rng.nextInt(maxDamage); // start intact: 0..maxDamage-1
            boolean currentlyBroken = false;

            int steps = 20 + rng.nextInt(20);
            for (int step = 0; step < steps; step++) {
                // Mix of damage hits (0..50000) and repairs (-maxDamage..-1)
                int delta;
                boolean isRepair = rng.nextInt(4) == 0;
                if (isRepair) {
                    delta = -(1 + rng.nextInt(maxDamage));
                } else {
                    delta = rng.nextInt(50_001); // 0..50000
                }

                // Simulate random enchantment/unbreaking effective damage reduction (0..delta)
                int unbreakingLevel = rng.nextInt(4);
                int effectiveDelta = delta;
                if (delta > 0 && unbreakingLevel > 0 && rng.nextBoolean()) {
                    effectiveDelta = Math.max(0, delta / (unbreakingLevel + 1));
                }

                DurabilityPolicy.HurtOutcome outcome =
                        DurabilityPolicy.evaluateHurt(damage, effectiveDelta, maxDamage, protectable);

                if (protectable) {
                    // Invariant 1: Protected item damage is ALWAYS clamped to [0, maxDamage]
                    assertTrue(
                            outcome.resultingDamage() >= 0 && outcome.resultingDamage() <= maxDamage,
                            () -> "Out-of-range damage on trial " + trialIndex + ": " + outcome.resultingDamage());

                    // Invariant 2: Any damaging hit reaching or exceeding maxDamage MUST cancel destruction
                    if (effectiveDelta >= 0 && (currentlyBroken || (long) damage + effectiveDelta >= maxDamage)) {
                        assertTrue(outcome.cancelDestruction(), "Protected item must never be destroyed");
                        assertEquals(maxDamage, outcome.resultingDamage());
                        if (currentlyBroken) {
                            assertFalse(outcome.firstBreak(), "Already-broken item must not re-fire firstBreak");
                            assertTrue(outcome.alreadyBroken());
                        } else {
                            assertTrue(outcome.firstBreak(), "First crossing into maxDamage must fire firstBreak");
                            assertFalse(outcome.alreadyBroken());
                        }
                    } else if (effectiveDelta < 0) {
                        // Repair brings damage strictly below maxDamage when starting from maxDamage
                        int expected = Math.max(0, damage + effectiveDelta);
                        assertEquals(expected, outcome.resultingDamage());
                        assertFalse(outcome.cancelDestruction());
                        assertFalse(outcome.firstBreak());
                    }

                    damage = outcome.resultingDamage();
                    boolean nowBroken = DurabilityPolicy.isBroken(false, true, maxDamage, damage, excluded, elytra);
                    assertEquals(damage >= maxDamage, nowBroken);

                    // Invariant 3: After repairing a broken item by >= 1, it MUST unseal cleanly
                    if (currentlyBroken && effectiveDelta < 0) {
                        assertFalse(nowBroken, "Repaired item must unseal back to intact state");
                    }
                    currentlyBroken = nowBroken;
                } else {
                    // Unprotectable (excluded or Elytra) must never be classified as broken or intercepted
                    assertFalse(DurabilityPolicy.isBroken(false, true, maxDamage, damage, excluded, elytra));
                    assertFalse(outcome.cancelDestruction());
                    assertFalse(outcome.firstBreak());
                    damage = Math.max(0, Math.min(maxDamage - 1, outcome.resultingDamage()));
                }
            }
        }
    }

    @Test
    void tomlConfigRoundTripIsLosslessAcrossRandomizedConfigs() {
        Random rng = new Random(0xC0FFEE_2026L);
        String[] namespaces = {"minecraft", "slashblade", "tconstruct", "tetra", "custom_mod", "a1_b2"};
        String[] paths = {"diamond_sword", "iron_pickaxe", "tools/swords", "modular_items", "shield.v2", "bow-3"};

        for (int trial = 0; trial < ITERATIONS; trial++) {
            boolean allowMending = rng.nextBoolean();
            boolean showNotification = rng.nextBoolean();
            boolean showOverlay = rng.nextBoolean();

            int count = rng.nextInt(8);
            List<String> rawEntries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                boolean isTag = rng.nextBoolean();
                boolean omitDefaultNs = !isTag && rng.nextInt(4) == 0;
                String ns = namespaces[rng.nextInt(namespaces.length)];
                String path = paths[rng.nextInt(paths.length)];
                if (isTag) {
                    rawEntries.add("#" + ns + ":" + path);
                } else if (omitDefaultNs) {
                    rawEntries.add(path); // normalizes to minecraft:<path>
                } else {
                    rawEntries.add(ns + ":" + path);
                }
            }

            KeepMySwordConfig original =
                    new KeepMySwordConfig(allowMending, showNotification, showOverlay, rawEntries);
            String serialized = original.toToml();
            KeepMySwordConfig parsed = KeepMySwordConfig.fromToml(serialized);

            assertEquals(original.allowMendingWhenBroken(), parsed.allowMendingWhenBroken(),
                    "allowMendingWhenBroken mismatch on trial " + trial);
            assertEquals(original.showBreakNotification(), parsed.showBreakNotification(),
                    "showBreakNotification mismatch on trial " + trial);
            assertEquals(original.showBrokenOverlay(), parsed.showBrokenOverlay(),
                    "showBrokenOverlay mismatch on trial " + trial);
            assertEquals(original.excludedItems(), parsed.excludedItems(),
                    "excludedItems mismatch on trial " + trial + "\n" + serialized);
            assertEquals(serialized, parsed.toToml(), "Second serialization must be idempotent");
        }
    }

    @Test
    void exclusionMatcherMatchesExactIdsAndTagsWithoutFalsePositives() {
        Random rng = new Random(0xE4C1_2026L);
        String[] namespaces = {"minecraft", "mod_a", "mod_b"};
        String[] items = {"sword", "axe", "pickaxe", "shovel", "hoe", "bow", "shield"};
        String[] tags = {"mod_a:swords", "mod_b:tools", "minecraft:axes"};

        for (int trial = 0; trial < ITERATIONS; trial++) {
            List<String> rules = new ArrayList<>();
            Set<String> expectedIds = new HashSet<>();
            Set<String> expectedTags = new HashSet<>();

            int ruleCount = rng.nextInt(6);
            for (int i = 0; i < ruleCount; i++) {
                int kind = rng.nextInt(3);
                if (kind == 0) {
                    String id = namespaces[rng.nextInt(namespaces.length)] + ":" + items[rng.nextInt(items.length)];
                    rules.add(id);
                    expectedIds.add(id);
                } else if (kind == 1) {
                    String tag = tags[rng.nextInt(tags.length)];
                    rules.add("#" + tag);
                    expectedTags.add(tag);
                } else {
                    // Inject invalid garbage that must be safely ignored
                    rules.add("INVALID::" + i + "!!");
                }
            }

            ExclusionMatcher matcher = ExclusionMatcher.compile(rules);

            // Comma-separated round-trip invariant
            List<String> validOnly = ExclusionMatcher.parseCommaSeparated(
                    ExclusionMatcher.formatCommaSeparated(rules));
            assertEquals(
                    ExclusionMatcher.parseCommaSeparated(String.join(",", validOnly)),
                    validOnly,
                    "Comma-separated normalization must be idempotent");

            // Check matching behavior on random candidate item + tag set
            String candidateId = namespaces[rng.nextInt(namespaces.length)] + ":" + items[rng.nextInt(items.length)];
            Set<String> candidateTags = new HashSet<>();
            for (String tag : tags) {
                if (rng.nextBoolean()) {
                    candidateTags.add(tag);
                }
            }

            boolean expectedMatch = expectedIds.contains(candidateId)
                    || candidateTags.stream().anyMatch(expectedTags::contains);
            assertEquals(expectedMatch, matcher.matches(candidateId, candidateTags::contains));
        }
    }
}
