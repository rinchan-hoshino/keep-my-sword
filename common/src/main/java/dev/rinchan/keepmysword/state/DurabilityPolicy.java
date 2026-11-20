package dev.rinchan.keepmysword.state;

/** Pure durability state-machine invariants shared across all Minecraft versions. */
public final class DurabilityPolicy {
    private DurabilityPolicy() {}

    public record HurtOutcome(
            int resultingDamage,
            boolean cancelDestruction,
            boolean firstBreak,
            boolean alreadyBroken
    ) {}

    /**
     * An item is broken when it is non-empty, damageable, has positive max durability,
     * is not excluded, is not a vanilla Elytra (which stops flying at 1 durability natively),
     * and its damage has reached or exceeded maxDamage.
     */
    public static boolean isBroken(
            boolean empty,
            boolean damageable,
            int maxDamage,
            int damage,
            boolean excluded,
            boolean elytra) {
        if (empty || !damageable || maxDamage <= 0 || excluded || elytra) return false;
        return damage >= maxDamage;
    }

    /**
     * Evaluates a durability hit on a protectable item.
     * Guarantees:
     * - Already-broken items stay clamped at maxDamage with firstBreak=false and alreadyBroken=true.
     * - First crossing from beforeDamage < maxDamage to >= maxDamage clamps at maxDamage with firstBreak=true.
     * - Non-breaking hits are untouched (cancelDestruction=false, firstBreak=false).
     */
    public static HurtOutcome evaluateHurt(int beforeDamage, int delta, int maxDamage, boolean protectable) {
        if (!protectable || maxDamage <= 0) {
            int next = Math.max(0, beforeDamage + delta);
            return new HurtOutcome(next, false, false, false);
        }
        if (beforeDamage >= maxDamage) {
            if (delta >= 0) {
                return new HurtOutcome(maxDamage, true, false, true);
            }
            int repaired = Math.max(0, beforeDamage + delta);
            return new HurtOutcome(repaired, false, false, false);
        }
        int next = beforeDamage + delta;
        if (next >= maxDamage) {
            return new HurtOutcome(maxDamage, true, true, false);
        }
        return new HurtOutcome(Math.max(0, next), false, false, false);
    }
}
