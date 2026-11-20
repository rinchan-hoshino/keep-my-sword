package dev.rinchan.keepmysword.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DurabilityPolicyTest {
    @Test
    void brokenBoundaryAndElytraExemption() {
        assertFalse(DurabilityPolicy.isBroken(true, true, 100, 100, false, false));
        assertFalse(DurabilityPolicy.isBroken(false, false, 100, 100, false, false));
        assertFalse(DurabilityPolicy.isBroken(false, true, 0, 0, false, false));
        assertFalse(DurabilityPolicy.isBroken(false, true, 100, 100, true, false));
        assertFalse(DurabilityPolicy.isBroken(false, true, 432, 432, false, true)); // Elytra exempt

        assertFalse(DurabilityPolicy.isBroken(false, true, 100, 99, false, false));
        assertTrue(DurabilityPolicy.isBroken(false, true, 100, 100, false, false));
        assertTrue(DurabilityPolicy.isBroken(false, true, 100, 105, false, false));
    }

    @Test
    void firstBreakFiresStrictlyOnceUntilRepaired() {
        int max = 50;
        var hit1 = DurabilityPolicy.evaluateHurt(49, 1, max, true);
        assertEquals(50, hit1.resultingDamage());
        assertTrue(hit1.cancelDestruction());
        assertTrue(hit1.firstBreak());
        assertFalse(hit1.alreadyBroken());

        var hit2 = DurabilityPolicy.evaluateHurt(50, 5, max, true);
        assertEquals(50, hit2.resultingDamage());
        assertTrue(hit2.cancelDestruction());
        assertFalse(hit2.firstBreak());
        assertTrue(hit2.alreadyBroken());

        // Repair by 10 -> damage drops to 40, then break again -> firstBreak fires again
        var repaired = DurabilityPolicy.evaluateHurt(50, -10, max, true);
        assertEquals(40, repaired.resultingDamage());
        assertFalse(repaired.cancelDestruction());

        var hit3 = DurabilityPolicy.evaluateHurt(40, 15, max, true);
        assertEquals(50, hit3.resultingDamage());
        assertTrue(hit3.cancelDestruction());
        assertTrue(hit3.firstBreak());
    }

    @Test
    void randomizedStateMachineInvariants() {
        Random rng = new Random(0x20261120L);
        for (int trial = 0; trial < 200; trial++) {
            int max = 1 + rng.nextInt(500);
            int damage = 0;
            boolean broken = false;

            for (int step = 0; step < 50; step++) {
                int delta = rng.nextInt(4) == 0 ? -(1 + rng.nextInt(max)) : (1 + rng.nextInt(Math.max(2, max / 2)));
                var outcome = DurabilityPolicy.evaluateHurt(damage, delta, max, true);

                assertTrue(outcome.resultingDamage() >= 0 && outcome.resultingDamage() <= max,
                        "Damage must stay clamped within [0, maxDamage]");

                if (broken && delta >= 0) {
                    assertFalse(outcome.firstBreak(), "Already-broken item must never re-trigger firstBreak");
                    assertTrue(outcome.alreadyBroken());
                    assertEquals(max, outcome.resultingDamage());
                } else if (!broken && damage + delta >= max) {
                    assertTrue(outcome.firstBreak(), "First crossing into >= maxDamage must trigger firstBreak");
                    assertTrue(outcome.cancelDestruction());
                    assertEquals(max, outcome.resultingDamage());
                } else {
                    assertFalse(outcome.firstBreak());
                }

                damage = outcome.resultingDamage();
                broken = DurabilityPolicy.isBroken(false, true, max, damage, false, false);
            }
        }
    }
}
