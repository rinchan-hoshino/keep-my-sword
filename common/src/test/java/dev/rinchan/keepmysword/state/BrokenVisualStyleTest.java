package dev.rinchan.keepmysword.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BrokenVisualStyleTest {
    @Test
    void badgeFitsInBottomRightAboveDurabilityBar() {
        assertEquals(5, BrokenVisualStyle.BADGE_SIZE);
        assertEquals(5, BrokenVisualStyle.BADGE_PIXELS.length);
        for (int[] row : BrokenVisualStyle.BADGE_PIXELS) {
            assertEquals(5, row.length);
        }
        assertTrue(BrokenVisualStyle.OFFSET_X >= 0 && BrokenVisualStyle.OFFSET_X + BrokenVisualStyle.BADGE_SIZE <= 16);
        assertTrue(BrokenVisualStyle.OFFSET_Y >= 0 && BrokenVisualStyle.OFFSET_Y + BrokenVisualStyle.BADGE_SIZE <= 14);
        assertEquals(0, BrokenVisualStyle.colorFor(0));
        assertNotEquals(0, BrokenVisualStyle.colorFor(1));
        assertNotEquals(0, BrokenVisualStyle.colorFor(2));
        assertNotEquals(0, BrokenVisualStyle.colorFor(3));
    }
}
