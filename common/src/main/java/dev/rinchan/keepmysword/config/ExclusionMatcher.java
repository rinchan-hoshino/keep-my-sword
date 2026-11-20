package dev.rinchan.keepmysword.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Compiled exclusion matcher for item IDs ("namespace:path") and item tags ("#namespace:path"). */
public final class ExclusionMatcher {
    private final Set<String> itemIds;
    private final Set<String> tagIds;
    private final List<String> normalizedEntries;

    private ExclusionMatcher(Set<String> itemIds, Set<String> tagIds, List<String> normalizedEntries) {
        this.itemIds = Collections.unmodifiableSet(itemIds);
        this.tagIds = Collections.unmodifiableSet(tagIds);
        this.normalizedEntries = Collections.unmodifiableList(normalizedEntries);
    }

    public static ExclusionMatcher compile(Collection<String> rawEntries) {
        Set<String> items = new LinkedHashSet<>();
        Set<String> tags = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();
        if (rawEntries != null) {
            for (String raw : rawEntries) {
                if (raw == null) continue;
                String trimmed = raw.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("#")) {
                    String tagId = normalizeId(trimmed.substring(1));
                    if (tagId != null && tags.add(tagId)) {
                        normalized.add("#" + tagId);
                    }
                } else {
                    String itemId = normalizeId(trimmed);
                    if (itemId != null && items.add(itemId)) {
                        normalized.add(itemId);
                    }
                }
            }
        }
        return new ExclusionMatcher(items, tags, normalized);
    }

    public static List<String> parseCommaSeparated(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> entries = new ArrayList<>();
        for (String part : text.split("[,\\n\\r]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) entries.add(trimmed);
        }
        return entries;
    }

    public static String formatCommaSeparated(Collection<String> entries) {
        if (entries == null || entries.isEmpty()) return "";
        return String.join(", ", entries);
    }

    public static String normalizeId(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        int colon = trimmed.indexOf(':');
        String namespace;
        String path;
        if (colon < 0) {
            namespace = "minecraft";
            path = trimmed;
        } else {
            if (trimmed.indexOf(':', colon + 1) >= 0) return null;
            namespace = trimmed.substring(0, colon);
            path = trimmed.substring(colon + 1);
        }
        if (!isValidNamespace(namespace) || !isValidPath(path)) return null;
        return namespace + ":" + path;
    }

    public static boolean isValidNamespace(String ns) {
        if (ns == null || ns.isEmpty()) return false;
        for (int i = 0; i < ns.length(); i++) {
            char c = ns.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
            if (!ok) return false;
        }
        return true;
    }

    public static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) return false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '_' || c == '-' || c == '.' || c == '/';
            if (!ok) return false;
        }
        return true;
    }

    public boolean matches(String itemId, Predicate<String> hasTag) {
        if (itemId != null && itemIds.contains(itemId)) return true;
        if (!tagIds.isEmpty() && hasTag != null) {
            for (String tagId : tagIds) {
                if (hasTag.test(tagId)) return true;
            }
        }
        return false;
    }

    public Set<String> itemIds() {
        return itemIds;
    }

    public Set<String> tagIds() {
        return tagIds;
    }

    public List<String> normalizedEntries() {
        return normalizedEntries;
    }
}
