package dev.rinchan.keepmysword;

import dev.rinchan.rinlib.v2.Ids;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public record KeepMySwordConfig(Set<String> excludedItems, Set<String> repairViewExcludedMenus) {
    public static final Set<String> DEFAULT_REPAIR_VIEW_EXCLUSIONS = Set.of(
            "minecraft:generic_9x1", "minecraft:generic_9x2", "minecraft:generic_9x3",
            "minecraft:generic_9x4", "minecraft:generic_9x5", "minecraft:generic_9x6",
            "minecraft:shulker_box", "minecraft:hopper", "minecraft:generic_3x3",
            "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer",
            "net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu");

    public KeepMySwordConfig {
        excludedItems = Set.copyOf(excludedItems);
        repairViewExcludedMenus = Set.copyOf(repairViewExcludedMenus);
    }

    public KeepMySwordConfig(Set<String> excludedItems) {
        this(excludedItems, DEFAULT_REPAIR_VIEW_EXCLUSIONS);
    }

    public static KeepMySwordConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var file = CommentedFileConfig.builder(directory.resolve("keep_my_sword.toml")).sync().build()) {
            file.load();
            if (!file.contains("excludedItems")) {
                file.set("excludedItems", List.of());
            }
            if (!file.contains("repairViewExcludedMenus")) {
                file.set("repairViewExcludedMenus", DEFAULT_REPAIR_VIEW_EXCLUSIONS.stream().sorted().toList());
                file.setComment("repairViewExcludedMenus", "Menu IDs or menu class names (including subclasses) that must keep items sealed. Add modded storage/backpack menus here.");
            }
            file.save();
            Object value = file.get("excludedItems");
            if (!(value instanceof List<?> entries) || entries.stream().anyMatch(entry ->
                    !(entry instanceof String selector) || !Ids.isValid(selector.startsWith("#") ? selector.substring(1) : selector))) {
                throw new IllegalArgumentException("keep_my_sword.toml: excludedItems must contain item IDs or #item_tag IDs");
            }
            Object menus = file.get("repairViewExcludedMenus");
            if (!(menus instanceof List<?> menuEntries) || menuEntries.stream().anyMatch(entry ->
                    !(entry instanceof String name) || !(Ids.isValid(name) || name.matches("[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)+")))) {
                throw new IllegalArgumentException("keep_my_sword.toml: repairViewExcludedMenus must contain menu IDs or fully qualified menu class names");
            }
            return new KeepMySwordConfig(entries.stream().map(String.class::cast).collect(Collectors.toSet()),
                    menuEntries.stream().map(String.class::cast).collect(Collectors.toSet()));
        }
    }

    public boolean excludesRepairView(Class<?> menuClass, String menuId) {
        if (repairViewExcludedMenus.contains(menuId)) return true;
        for (Class<?> type = menuClass; type != null; type = type.getSuperclass())
            if (repairViewExcludedMenus.contains(type.getName())) return true;
        return false;
    }

    public boolean matches(String itemId, Stream<String> tags) {
        return excludedItems.contains(itemId) || tags.anyMatch(tag -> excludedItems.contains("#" + tag));
    }
}
