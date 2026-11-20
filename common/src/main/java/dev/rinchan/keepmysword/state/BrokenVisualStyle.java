package dev.rinchan.keepmysword.state;

/**
 * Crisp 5x5 pixel broken-item corner badge rendered in the bottom-right of GUI slots.
 * Replaces the old full-slot red wash so the item silhouette and empty durability bar stay legible.
 */
public final class BrokenVisualStyle {
    private BrokenVisualStyle() {}

    public static final int BADGE_SIZE = 5;
    public static final int OFFSET_X = 11; // 16 - 5 = bottom-right corner
    public static final int OFFSET_Y = 9;  // sits cleanly just above the vanilla durability bar (y=13..14)

    public static final int COLOR_OUTLINE = 0xFF6B1418;  // deep crimson border
    public static final int COLOR_CORE    = 0xFFD93B3B;  // bright crimson body
    public static final int COLOR_CRACK   = 0xFFFFE0B2;  // warm amber-white crack highlight

    /**
     * 5x5 pixel mask:
     * 0 = transparent, 1 = outline, 2 = core, 3 = crack highlight.
     * Depicts a crisp mini cracked diamond/shard emblem.
     */
    public static final int[][] BADGE_PIXELS = {
            {0, 1, 1, 1, 0},
            {1, 2, 3, 2, 1},
            {1, 3, 2, 3, 1},
            {1, 2, 3, 2, 1},
            {0, 1, 1, 1, 0}
    };

    public static int colorFor(int code) {
        return switch (code) {
            case 1 -> COLOR_OUTLINE;
            case 2 -> COLOR_CORE;
            case 3 -> COLOR_CRACK;
            default -> 0;
        };
    }
}
