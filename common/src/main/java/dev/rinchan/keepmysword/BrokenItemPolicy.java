package dev.rinchan.keepmysword;

/** Loader-neutral decisions for the vanilla destruction boundary. */
public final class BrokenItemPolicy {
    private BrokenItemPolicy() {}

    public static boolean isManaged(boolean excluded, boolean broken) {
        return !excluded && broken;
    }

    public static boolean preserveAtDestruction(boolean excluded) {
        return !excluded;
    }
}
