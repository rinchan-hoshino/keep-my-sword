package dev.rinchan.keepmysword.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class KeepMySwordConfigTest {
    @Test
    void defaultsMatchSpec() {
        KeepMySwordConfig cfg = KeepMySwordConfig.defaults();
        assertTrue(cfg.allowMendingWhenBroken());
        assertTrue(cfg.showBreakNotification());
        assertTrue(cfg.showBrokenOverlay());
        assertEquals(
                List.of("#slashblade:swords", "#tconstruct:modifiable", "#tetra:modular_items"),
                cfg.excludedItems());
    }

    @Test
    void tomlRoundTripAndComments() {
        KeepMySwordConfig original = new KeepMySwordConfig(
                false,
                true,
                false,
                List.of("minecraft:diamond_sword", "#slashblade:swords", "iron_pickaxe"));

        String toml = original.toToml();
        assertTrue(toml.contains("# Allow Mending"));
        assertTrue(toml.contains("# Show an actionbar message"));
        assertTrue(toml.contains("# Draw a small broken-item badge"));
        assertTrue(toml.contains("allowMendingWhenBroken = false"));
        assertTrue(toml.contains("showBreakNotification = true"));
        assertTrue(toml.contains("showBrokenOverlay = false"));

        KeepMySwordConfig parsed = KeepMySwordConfig.fromToml(toml);
        assertFalse(parsed.allowMendingWhenBroken());
        assertTrue(parsed.showBreakNotification());
        assertFalse(parsed.showBrokenOverlay());
        assertEquals(
                List.of("minecraft:diamond_sword", "#slashblade:swords", "minecraft:iron_pickaxe"),
                parsed.excludedItems());
    }

    @Test
    void parsesMultilineTomlWithCommentsAndFallbacks() {
        String raw = """
                # user comment
                allowMendingWhenBroken = false # inline comment
                excludedItems = [
                    "#tconstruct:modifiable", # tinker tools
                    'minecraft:netherite_sword',
                    "INVALID::ENTRY",
                    ""
                ]
                """;
        KeepMySwordConfig cfg = KeepMySwordConfig.fromToml(raw);
        assertFalse(cfg.allowMendingWhenBroken());
        assertTrue(cfg.showBreakNotification()); // fallback to default
        assertTrue(cfg.showBrokenOverlay());     // fallback to default
        assertEquals(List.of("#tconstruct:modifiable", "minecraft:netherite_sword"), cfg.excludedItems());
    }

    @Test
    void fileLoadAndHotReload(@TempDir Path tempDir) throws Exception {
        KeepMySwordConfig loaded = KeepMySwordConfig.load(tempDir);
        Path file = tempDir.resolve(KeepMySwordConfig.FILE_NAME);
        assertTrue(Files.exists(file));
        assertEquals(KeepMySwordConfig.DEFAULT_EXCLUDED_ITEMS, loaded.excludedItems());

        KeepMySwordConfig updated = new KeepMySwordConfig(false, false, true, List.of("#custom:tools"));
        KeepMySwordConfig.updateAndSave(updated);

        assertFalse(KeepMySwordConfig.get().allowMendingWhenBroken());
        assertEquals(List.of("#custom:tools"), KeepMySwordConfig.readFrom(file).excludedItems());

        KeepMySwordConfig.setForTest(KeepMySwordConfig.defaults());
    }

    @Test
    void exclusionMatcherIgnoresUninstalledTagsSafely() {
        ExclusionMatcher matcher = ExclusionMatcher.compile(
                List.of("#slashblade:swords", "minecraft:wooden_sword", "not valid!!"));
        assertTrue(matcher.matches("minecraft:wooden_sword", tag -> false));
        assertFalse(matcher.matches("minecraft:diamond_sword", tag -> false));
        assertTrue(matcher.matches("slashblade:slashblade", Set.of("slashblade:swords")::contains));
        assertEquals(
                List.of("#a:b", "minecraft:c"),
                ExclusionMatcher.parseCommaSeparated(" #a:b , minecraft:c , \n "));
    }
}
