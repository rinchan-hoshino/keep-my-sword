package dev.rinchan.keepmysword.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal zero-dependency TOML parser and commented writer for flat boolean and string-list configs. */
public final class SimpleToml {
    private SimpleToml() {}

    public static Map<String, Object> parse(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (content == null || content.isEmpty()) return result;

        int i = 0;
        int len = content.length();
        while (i < len) {
            i = skipWhitespaceAndComments(content, i);
            if (i >= len) break;

            int keyStart = i;
            while (i < len && content.charAt(i) != '=' && content.charAt(i) != '\n' && content.charAt(i) != '\r') {
                i++;
            }
            if (i >= len || content.charAt(i) != '=') {
                i = skipToNextLine(content, i);
                continue;
            }
            String key = content.substring(keyStart, i).trim();
            i++; // consume '='
            i = skipInlineWhitespace(content, i);
            if (i >= len || key.isEmpty()) break;

            char c = content.charAt(i);
            if (c == '[') {
                List<String> list = new ArrayList<>();
                i++; // consume '['
                while (i < len) {
                    i = skipWhitespaceAndComments(content, i);
                    if (i >= len) break;
                    char cur = content.charAt(i);
                    if (cur == ']') {
                        i++;
                        break;
                    }
                    if (cur == ',') {
                        i++;
                        continue;
                    }
                    if (cur == '"' || cur == '\'') {
                        int[] next = new int[1];
                        String val = readString(content, i, next);
                        list.add(val);
                        i = next[0];
                    } else {
                        i = skipToDelimiter(content, i);
                    }
                }
                result.put(key, list);
            } else if (c == '"' || c == '\'') {
                int[] next = new int[1];
                result.put(key, readString(content, i, next));
                i = next[0];
            } else {
                int valStart = i;
                while (i < len && content.charAt(i) != '#' && content.charAt(i) != '\n' && content.charAt(i) != '\r') {
                    i++;
                }
                String raw = content.substring(valStart, i).trim();
                if ("true".equalsIgnoreCase(raw)) {
                    result.put(key, Boolean.TRUE);
                } else if ("false".equalsIgnoreCase(raw)) {
                    result.put(key, Boolean.FALSE);
                } else if (!raw.isEmpty()) {
                    result.put(key, raw);
                }
            }
            i = skipToNextLine(content, i);
        }
        return result;
    }

    public static String escapeString(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 4);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '"') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    public static String formatStringArray(List<String> values) {
        if (values == null || values.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(escapeString(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String readString(String s, int start, int[] nextOut) {
        char quote = s.charAt(start);
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (quote == '"' && c == '\\' && i + 1 < len) {
                char esc = s.charAt(i + 1);
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(esc);
                }
                i += 2;
                continue;
            }
            if (c == quote) {
                i++;
                break;
            }
            if (c == '\n' || c == '\r') break;
            sb.append(c);
            i++;
        }
        nextOut[0] = i;
        return sb.toString();
    }

    private static int skipWhitespaceAndComments(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                i++;
            } else if (c == '#') {
                i = skipToNextLine(s, i);
            } else {
                break;
            }
        }
        return i;
    }

    private static int skipInlineWhitespace(String s, int i) {
        int len = s.length();
        while (i < len && (s.charAt(i) == ' ' || s.charAt(i) == '\t')) i++;
        return i;
    }

    private static int skipToNextLine(String s, int i) {
        int len = s.length();
        while (i < len && s.charAt(i) != '\n') i++;
        return i < len ? i + 1 : len;
    }

    private static int skipToDelimiter(String s, int i) {
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == ',' || c == ']' || c == '\n' || c == '\r') break;
            i++;
        }
        return i;
    }
}
