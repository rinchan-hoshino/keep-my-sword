package dev.rinchan.keepmysword.state;

import net.minecraft.world.InteractionHand;

/** Masks a broken hand during 1.20.1 BlockState.use so blocks like pumpkins/beehives see an empty hand while menus still open. */
public final class HandMask {
    private static final ThreadLocal<InteractionHand> MASKED = new ThreadLocal<>();

    private HandMask() {}

    public static void set(InteractionHand hand) {
        MASKED.set(hand);
    }

    public static void clear() {
        MASKED.remove();
    }

    public static boolean isMasked(InteractionHand hand) {
        return hand != null && hand == MASKED.get();
    }
}
