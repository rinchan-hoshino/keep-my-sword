package dev.rinchan.keepmysword.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BrokenTintRenderStateTest {
    @Test
    void computesOnceAndReusesTheDecisionForEveryQuad() {
        BrokenTintRenderState state = new BrokenTintRenderState();
        AtomicInteger computations = new AtomicInteger();

        state.enter(() -> {
            computations.incrementAndGet();
            return true;
        });

        assertTrue(state.shouldTint());
        assertTrue(state.shouldTint());
        assertTrue(state.shouldTint());
        assertTrue(computations.get() == 1);
        state.exit();
        assertFalse(state.shouldTint());
    }

    @Test
    void nestedItemRenderingRestoresTheOuterDecision() {
        BrokenTintRenderState state = new BrokenTintRenderState();

        state.enter(() -> true);
        state.enter(() -> false);
        assertFalse(state.shouldTint());

        state.exit();
        assertTrue(state.shouldTint());

        state.exit();
        assertFalse(state.shouldTint());
        state.exit();
        assertFalse(state.shouldTint());
    }
}
