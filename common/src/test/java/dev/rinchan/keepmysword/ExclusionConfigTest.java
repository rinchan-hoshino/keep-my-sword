package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExclusionConfigTest {
    @TempDir Path directory;

    @Test void freshConfigLeavesNormalItemsManaged() throws Exception {
        var config = KeepMySwordConfig.load(directory);
        assertTrue(Files.isRegularFile(directory.resolve("keep_my_sword.toml")));
        assertFalse(config.matches("minecraft:diamond_sword", Stream.of("minecraft:swords")));
    }

    @Test void realTomlSupportsExactItemsAndWholeTags() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"),
            "# User compatibility policy\nexcludedItems = [\"minecraft:diamond_sword\", \"#slashblade:swords\"]\n");
        var config = KeepMySwordConfig.load(directory);
        assertTrue(config.matches("minecraft:diamond_sword", Stream.empty()));
        assertTrue(config.matches("slashblade:slashblade", Stream.of("slashblade:swords")));
        assertFalse(config.matches("minecraft:iron_sword", Stream.of("minecraft:swords")));
        assertEquals(config, KeepMySwordConfig.load(directory));
    }

    @Test void configurationChangesTheActualManagedItemPredicate() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = [\"minecraft:diamond_sword\"]\n");
        try {
            KeepMySword.initialize(directory);
            assertTrue(KeepMySword.isExcluded(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD)));
            assertFalse(KeepMySword.isExcluded(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD)));
        } finally {
            Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = []\n");
            KeepMySword.initialize(directory);
        }
    }

    @Test void malformedSelectorsAreRejectedAtTheConfigurationBoundary() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = [\"not an item\"]\n");
        assertThrows(IllegalArgumentException.class, () -> KeepMySwordConfig.load(directory));
    }
}
