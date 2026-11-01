/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.config;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, comment-preserving editor for the plugin's own config.yml.
 *
 * <p>Rewriting the file through SnakeYAML would drop every comment, so edits
 * are applied to the original text: the matching {@code key: value} line is
 * replaced in place (keeping indentation and a trailing comment). When a key is
 * missing, a small block is inserted beneath its parent section.</p>
 *
 * <p>All methods are pure and safe to call from unit tests. Actual file IO stays
 * in {@code FileStorage} on the storage thread.</p>
 */
public final class YamlDocument {

    private static final Pattern KEY_LINE =
        Pattern.compile("^(\\s*)([^\\s#][^:]*?):(\\s.*)?$");
    private static final String INDENT = "  ";

    private YamlDocument() {
    }

    /**
     * Parses YAML into mutable nested maps. Throws {@link IllegalArgumentException}
     * when the document is not a mapping, so callers can report a clear error
     * instead of writing a broken file.
     */
    public static Map<String, Object> parse(String text) {
        if (text == null || text.isBlank()) {
            return new LinkedHashMap<>();
        }
        Object loaded;
        try {
            LoaderOptions options = new LoaderOptions();
            loaded = new Yaml(new SafeConstructor(options)).load(text);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("invalid YAML: " + exception.getMessage());
        }
        if (loaded == null) {
            return new LinkedHashMap<>();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("config root must be a mapping");
        }
        return normalize(map);
    }

    /**
     * Returns a new document with one scalar replaced. The input text is not
     * modified. The result is parsed again and verified before being returned.
     */
    public static String set(String text, String path, Object value) {
        List<String> parts = splitPath(path);
        String source = text == null ? "" : text;
        String separator = source.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = new ArrayList<>(Arrays.asList(source.split("\r?\n", -1)));
        if (source.isEmpty()) {
            lines.clear();
        }

        List<Frame> stack = new ArrayList<>();
        int targetLine = -1;
        String targetPrefix = "";
        String targetKey = "";
        String targetComment = "";
        int parentLine = -1;
        int parentDepth = 0;

        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line.isBlank() || line.trim().startsWith("#") || line.trim().startsWith("-")) {
                continue;
            }
            Matcher matcher = KEY_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            int indent = matcher.group(1).length();
            String key = unquoteKey(matcher.group(2).trim());
            while (!stack.isEmpty() && stack.get(stack.size() - 1).indent() >= indent) {
                stack.remove(stack.size() - 1);
            }
            List<String> currentPath = new ArrayList<>();
            if (!stack.isEmpty()) {
                currentPath.addAll(stack.get(stack.size() - 1).path());
            }
            currentPath.add(key);
            stack.add(new Frame(indent, currentPath));

            if (currentPath.equals(parts)) {
                targetLine = index;
                targetPrefix = matcher.group(1);
                targetKey = matcher.group(2).trim();
                targetComment = trailingComment(line, matcher.end(2) + 1);
            } else if (parts.size() > currentPath.size() && isPrefix(currentPath, parts)
                && currentPath.size() > parentDepth) {
                parentLine = index;
                parentDepth = currentPath.size();
            }
        }

        if (targetLine >= 0) {
            lines.set(targetLine, targetPrefix + targetKey + ": " + formatScalar(value)
                + targetComment);
        } else {
            List<String> block = buildBlock(parts, parentDepth, value, separator);
            int insertAt;
            if (parentLine >= 0) {
                insertAt = parentLine + 1;
            } else {
                insertAt = lines.size();
                if (insertAt > 0 && lines.get(insertAt - 1).isEmpty()) {
                    insertAt--;
                }
            }
            lines.addAll(insertAt, block);
        }

        String updated = String.join(separator, lines);
        verify(updated, parts, value);
        return updated;
    }

    /**
     * Inserts or replaces a value inside an already parsed, mutable map tree.
     */
    @SuppressWarnings("unchecked")
    public static void setValue(Map<String, Object> root, String path, Object value) {
        List<String> parts = splitPath(path);
        Map<String, Object> current = root;
        for (int index = 0; index < parts.size() - 1; index++) {
            Object child = current.get(parts.get(index));
            if (!(child instanceof Map<?, ?>)) {
                child = new LinkedHashMap<String, Object>();
                current.put(parts.get(index), child);
            }
            current = (Map<String, Object>) child;
        }
        current.put(parts.get(parts.size() - 1), value);
    }

    /**
     * Reads a dot-separated path from a parsed document, or null when absent.
     */
    public static Object valueAt(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : splitPath(path)) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(part);
        }
        return current;
    }

    /**
     * Deep-copies a parsed document so the caller can mutate it freely.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), copyValue(entry.getValue()));
        }
        return copy;
    }

    /**
     * Formats a scalar as a YAML value, quoting strings that would be
     * misinterpreted or contain special characters.
     */
    public static String formatScalar(Object value) {
        if (value == null) {
            return "''";
        }
        if (value instanceof Boolean bool) {
            return bool ? "true" : "false";
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }

    /**
     * Splits a dot-separated config path, rejecting empty segments.
     */
    public static List<String> splitPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("empty config path");
        }
        List<String> parts = new ArrayList<>();
        for (String part : path.trim().split("\\.")) {
            if (part.isBlank()) {
                throw new IllegalArgumentException("invalid config path: " + path);
            }
            parts.add(part);
        }
        return List.copyOf(parts);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                copy.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
            }
            return copy;
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object element : list) {
                copy.add(copyValue(element));
            }
            return copy;
        }
        return value;
    }

    private static Map<String, Object> normalize(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            result.put(String.valueOf(entry.getKey()), copyValue(entry.getValue()));
        }
        return result;
    }

    private static List<String> buildBlock(List<String> parts, int depth, Object value,
                                            String separator) {
        List<String> block = new ArrayList<>();
        for (int level = depth; level < parts.size() - 1; level++) {
            block.add(INDENT.repeat(level) + parts.get(level) + ":");
        }
        block.add(INDENT.repeat(parts.size() - 1) + parts.get(parts.size() - 1) + ": "
            + formatScalar(value));
        return block;
    }

    private static void verify(String text, List<String> parts, Object expected) {
        Object actual;
        try {
            actual = valueAt(parse(text), String.join(".", parts));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("edit would produce invalid YAML: "
                + exception.getMessage());
        }
        if (!scalarEquals(expected, actual)) {
            throw new IllegalStateException("could not update " + String.join(".", parts)
                + " in config.yml");
        }
    }

    private static boolean scalarEquals(Object expected, Object actual) {
        if (expected == null || actual == null) {
            return expected == actual;
        }
        if (expected instanceof Number left && actual instanceof Number right) {
            return left.longValue() == right.longValue();
        }
        return String.valueOf(expected).equals(String.valueOf(actual));
    }

    private static boolean isPrefix(List<String> prefix, List<String> full) {
        for (int index = 0; index < prefix.size(); index++) {
            if (!prefix.get(index).equals(full.get(index))) {
                return false;
            }
        }
        return true;
    }

    private static String trailingComment(String line, int fromIndex) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int index = Math.max(0, fromIndex); index < line.length(); index++) {
            char character = line.charAt(index);
            if (inSingle) {
                if (character == '\'') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '\'') {
                        index++;
                    } else {
                        inSingle = false;
                    }
                }
            } else if (inDouble) {
                if (character == '\\') {
                    index++;
                } else if (character == '"') {
                    inDouble = false;
                }
            } else if (character == '\'') {
                inSingle = true;
            } else if (character == '"') {
                inDouble = true;
            } else if (character == '#' && index > 0
                && Character.isWhitespace(line.charAt(index - 1))) {
                int start = index;
                while (start > 0 && Character.isWhitespace(line.charAt(start - 1))) {
                    start--;
                }
                return line.substring(start);
            }
        }
        return "";
    }

    private static String unquoteKey(String key) {
        if (key.length() >= 2) {
            char first = key.charAt(0);
            char last = key.charAt(key.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                String inner = key.substring(1, key.length() - 1);
                return inner.replace("''", "'");
            }
        }
        return key;
    }

    private record Frame(int indent, List<String> path) {
    }
}
