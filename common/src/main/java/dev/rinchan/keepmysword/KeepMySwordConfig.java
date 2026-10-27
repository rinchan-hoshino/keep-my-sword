package dev.rinchan.keepmysword;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public record KeepMySwordConfig(Set<String> excludedItems) {
    public KeepMySwordConfig { excludedItems = Set.copyOf(excludedItems); }

    public static KeepMySwordConfig load(Path directory) throws IOException {
        Files.createDirectories(directory);
        try (var file = CommentedFileConfig.builder(directory.resolve("keep_my_sword.toml")).sync().build()) {
            file.load();
            if (!file.contains("excludedItems")) {
                file.set("excludedItems", List.of());
                file.save();
            }
            Object value = file.get("excludedItems");
            if (!(value instanceof List<?> entries) || entries.stream().anyMatch(entry ->
                    !(entry instanceof String selector) || !VersionTags.validSelector(selector))) {
                throw new IllegalArgumentException("keep_my_sword.toml: excludedItems must contain item IDs or #item_tag IDs");
            }
            return new KeepMySwordConfig(entries.stream().map(String.class::cast).collect(Collectors.toSet()));
        }
    }

    public boolean matches(String itemId, Stream<String> tags) {
        return excludedItems.contains(itemId) || tags.anyMatch(tag -> excludedItems.contains("#" + tag));
    }
}
