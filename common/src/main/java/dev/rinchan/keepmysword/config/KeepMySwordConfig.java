package dev.rinchan.keepmysword.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Configuration model and zero-dependency TOML persistence for Keep My Sword. */
public final class KeepMySwordConfig {
    public static final String FILE_NAME = "keep_my_sword.toml";
    public static final List<String> DEFAULT_EXCLUDED_ITEMS = List.of(
            "#slashblade:swords",
            "#tconstruct:modifiable",
            "#tetra:modular_items"
    );

    private static volatile KeepMySwordConfig current = defaults();
    private static volatile Path configPath;

    private final boolean allowMendingWhenBroken;
    private final boolean showBreakNotification;
    private final boolean showBrokenOverlay;
    private final List<String> excludedItems;
    private final ExclusionMatcher matcher;

    public KeepMySwordConfig(
            boolean allowMendingWhenBroken,
            boolean showBreakNotification,
            boolean showBrokenOverlay,
            List<String> excludedItems) {
        this.allowMendingWhenBroken = allowMendingWhenBroken;
        this.showBreakNotification = showBreakNotification;
        this.showBrokenOverlay = showBrokenOverlay;
        this.matcher = ExclusionMatcher.compile(excludedItems);
        this.excludedItems = this.matcher.normalizedEntries();
    }

    public static KeepMySwordConfig defaults() {
        return new KeepMySwordConfig(true, true, true, DEFAULT_EXCLUDED_ITEMS);
    }

    public static KeepMySwordConfig get() {
        return current;
    }

    public static void setForTest(KeepMySwordConfig config) {
        current = config == null ? defaults() : config;
    }

    public static synchronized KeepMySwordConfig load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        configPath = file;
        KeepMySwordConfig loaded = readFrom(file);
        current = loaded;
        if (!Files.exists(file)) {
            writeTo(file, loaded);
        }
        return loaded;
    }

    public static KeepMySwordConfig readFrom(Path file) {
        if (file == null || !Files.exists(file)) {
            return defaults();
        }
        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            return fromToml(raw);
        } catch (IOException ignored) {
            return defaults();
        }
    }

    public static KeepMySwordConfig fromToml(String toml) {
        Map<String, Object> parsed = SimpleToml.parse(toml);
        boolean allowMending = readBoolean(parsed, "allowMendingWhenBroken", true);
        boolean showNotify = readBoolean(parsed, "showBreakNotification", true);
        boolean showOverlay = readBoolean(parsed, "showBrokenOverlay", true);
        List<String> excluded = readStringList(parsed, "excludedItems", DEFAULT_EXCLUDED_ITEMS);
        return new KeepMySwordConfig(allowMending, showNotify, showOverlay, excluded);
    }

    public static synchronized void updateAndSave(KeepMySwordConfig updated) {
        current = updated == null ? defaults() : updated;
        if (configPath != null) {
            writeTo(configPath, current);
        }
    }

    public static boolean writeTo(Path file, KeepMySwordConfig config) {
        if (file == null || config == null) return false;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(file, config.toToml(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public String toToml() {
        return "# Keep My Sword configuration\n"
                + "# Changes saved from the in-game config screen take effect immediately.\n\n"
                + "# Allow Mending to repair broken items with experience orbs.\n"
                + "allowMendingWhenBroken = " + allowMendingWhenBroken + "\n\n"
                + "# Show an actionbar message when an item first enters the broken state.\n"
                + "showBreakNotification = " + showBreakNotification + "\n\n"
                + "# Draw a small broken-item badge on the bottom-right corner of GUI slots.\n"
                + "showBrokenOverlay = " + showBrokenOverlay + "\n\n"
                + "# Item IDs (\"minecraft:diamond_sword\") or item tags (\"#slashblade:swords\")\n"
                + "# excluded from broken-item protection. Missing mod tags are ignored safely.\n"
                + "# Items in the #keep_my_sword:excluded datapack tag are always excluded.\n"
                + "excludedItems = " + SimpleToml.formatStringArray(excludedItems) + "\n";
    }

    public boolean allowMendingWhenBroken() {
        return allowMendingWhenBroken;
    }

    public boolean showBreakNotification() {
        return showBreakNotification;
    }

    public boolean showBrokenOverlay() {
        return showBrokenOverlay;
    }

    public List<String> excludedItems() {
        return excludedItems;
    }

    public ExclusionMatcher exclusionMatcher() {
        return matcher;
    }

    private static boolean readBoolean(Map<String, Object> map, String key, boolean fallback) {
        Object val = map.get(key);
        return val instanceof Boolean b ? b : fallback;
    }

    @SuppressWarnings("unchecked")
    private static List<String> readStringList(Map<String, Object> map, String key, List<String> fallback) {
        Object val = map.get(key);
        if (val instanceof List<?> rawList) {
            List<String> out = new ArrayList<>(rawList.size());
            for (Object elem : rawList) {
                if (elem instanceof String s) out.add(s);
            }
            return out;
        }
        return fallback;
    }
}
