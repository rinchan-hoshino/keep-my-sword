package dev.rinchan.keepmysword.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BrokenVisualStyleTest {
    @Test
    void usesALightTranslucentInventoryWarningRed() {
        assertEquals(0x33, BrokenVisualStyle.SLOT_OVERLAY_ARGB >>> 24);
        assertEquals(0xFF6B6B, BrokenVisualStyle.SLOT_OVERLAY_ARGB & 0xFFFFFF);
    }
}
