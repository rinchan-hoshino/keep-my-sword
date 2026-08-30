package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

final class BrokenItemPolicyTest {
    @Test
    void managesExactlyBrokenStacksThatDidNotOptOut() {
        for (boolean excluded : new boolean[] {false, true}) {
            for (boolean broken : new boolean[] {false, true}) {
                assertEquals(
                    !excluded && broken,
                    BrokenItemPolicy.isManaged(excluded, broken),
                    "excluded=" + excluded + ", broken=" + broken
                );
            }
        }
    }

    @Test
    void excludedStackKeepsVanillaDestructionEvenWhenBroken() {
        assertFalse(BrokenItemPolicy.preserveAtDestruction(true));
        assertFalse(BrokenItemPolicy.isManaged(true, true));
    }
}
