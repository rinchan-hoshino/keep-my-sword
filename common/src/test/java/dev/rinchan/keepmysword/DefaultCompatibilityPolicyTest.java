package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DefaultCompatibilityPolicyTest {
    @Test
    void defaultExcludedTagIsEmpty() throws IOException {
        String json = readResource("/data/keep_my_sword/tags/item/excluded.json").replaceAll("\\s+", "");
        assertTrue(json.contains("\"values\":[]"));
    }

    @Test
    void clientMixinsOnlyOwnTheGuiWarning() throws IOException {
        String json = readResource("/keep_my_sword.client.mixins.json");
        assertTrue(json.contains("GuiGraphicsMixin"));
        assertFalse(json.contains("ItemRendererMixin"));
    }

    private String readResource(String path) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream(path)) {
            assertNotNull(stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
