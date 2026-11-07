package dev.rinchan.keepmysword;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
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
            var excluded = mock(net.minecraft.world.item.ItemStack.class);
            when(excluded.getItem()).thenReturn(net.minecraft.world.item.Items.DIAMOND_SWORD);
            var managed = mock(net.minecraft.world.item.ItemStack.class);
            when(managed.getItem()).thenReturn(net.minecraft.world.item.Items.IRON_SWORD);
            assertTrue(KeepMySword.isExcluded(excluded));
            assertFalse(KeepMySword.isExcluded(managed));
        } finally {
            Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = []\n");
            KeepMySword.initialize(directory);
        }
    }

    @Test void storageDefaultsSurviveAnExistingItemOnlyConfig() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = []\n");
        var config = KeepMySwordConfig.load(directory);
        assertTrue(config.excludesRepairView(Object.class, "minecraft:hopper"));
        assertTrue(config.excludesRepairView(Object.class, "minecraft:generic_9x6"));
        assertFalse(config.excludesRepairView(Object.class, "minecraft:anvil"));
        assertEquals(config, KeepMySwordConfig.load(directory));
    }

    @Test void repairViewConfigOverridesDefaultsAndMatchesSubclasses() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"),
                "repairViewExcludedMenus = [\"mod:backpack\", \"java.util.AbstractList\"]\n");
        var config = KeepMySwordConfig.load(directory);
        assertTrue(config.excludesRepairView(Object.class, "mod:backpack"));
        assertTrue(config.excludesRepairView(java.util.ArrayList.class, "mod:workbench"));
        assertFalse(config.excludesRepairView(Object.class, "minecraft:hopper"));
    }

    @Test void malformedMenuSelectorsAreRejected() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"), "repairViewExcludedMenus = [42]\n");
        assertThrows(IllegalArgumentException.class, () -> KeepMySwordConfig.load(directory));
    }

    @Test void malformedSelectorsAreRejectedAtTheConfigurationBoundary() throws Exception {
        Files.writeString(directory.resolve("keep_my_sword.toml"), "excludedItems = [\"not an item\"]\n");
        assertThrows(IllegalArgumentException.class, () -> KeepMySwordConfig.load(directory));
    }
}
