/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.yaml.snakeyaml.Yaml;

/**
 * Loads messages from lang/*.yml and renders them with MiniMessage.
 *
 * <p>All placeholders are inserted through {@link Placeholder#unparsed}, so a player cannot inject
 * clickable text or formatting through a report detail.
 */
public final class Messages {

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

  private static final DateTimeFormatter DEFAULT_DATE =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final Map<String, Object> values;

  private final String language;

  private Messages(Map<String, Object> values, String language) {

    this.values = values;

    this.language = language;
  }

  public static Messages empty() {

    return new Messages(Map.of(), "zh_CN");
  }

  public static Messages load(Path primary, Path fallback, Consumer<String> warning) {

    Map<String, Object> merged = new LinkedHashMap<>();

    // Bundled defaults come first so a lang file copied by an older plugin

    // version still renders every message after an upgrade. Server owners can

    // override any entry by editing their own file.

    merged.putAll(readBundled(primary, warning));

    merged.putAll(read(fallback, warning));

    merged.putAll(read(primary, warning));

    String language =
        primary == null ? "zh_CN" : primary.getFileName().toString().replace(".yml", "");

    return new Messages(Map.copyOf(merged), language);
  }

  public static Messages fromMap(Map<String, Object> map, String language) {

    Map<String, Object> flattened = new LinkedHashMap<>();

    flatten("", map, flattened);

    return new Messages(Map.copyOf(flattened), language == null ? "zh_CN" : language);
  }

  public static Messages fromStream(InputStream stream, String language) {

    Map<String, Object> flattened = new LinkedHashMap<>();

    try (InputStream input = stream) {

      Object loaded = new Yaml().load(input);

      if (loaded instanceof Map<?, ?> map) {

        Map<String, Object> stringMap = new LinkedHashMap<>();

        map.forEach((key, value) -> stringMap.put(String.valueOf(key), value));

        flatten("", stringMap, flattened);
      }

    } catch (IOException | RuntimeException exception) {

      // Caller gets an empty map; the plugin logs the failure separately.

    }

    return new Messages(Map.copyOf(flattened), language == null ? "zh_CN" : language);
  }

  public String language() {

    return language;
  }

  public boolean has(String key) {

    return values.containsKey(key);
  }

  public String raw(String key) {

    Object value = values.get(key);

    if (value instanceof String string) {

      return string;
    }

    if (value instanceof List<?> list && !list.isEmpty()) {

      return String.valueOf(list.get(0));
    }

    return key;
  }

  public List<String> rawList(String key) {

    Object value = values.get(key);

    if (value instanceof List<?> list) {

      List<String> result = new ArrayList<>();

      for (Object element : list) {

        if (element != null) {

          result.add(String.valueOf(element));
        }
      }

      return result;
    }

    if (value instanceof String string) {

      return List.of(string);
    }

    return List.of();
  }

  public Component component(String key) {

    return component(key, Map.of());
  }

  public Component component(String key, Map<String, String> placeholders) {

    return render(raw(key), placeholders);
  }

  public Component prefixed(String key, Map<String, String> placeholders) {

    Component prefix = component(MessageKeys.PREFIX);

    Component body = component(key, placeholders);

    return Component.empty().append(prefix).append(body);
  }

  /**
   * Renders a message and strips all styling. Useful when a component is used
   *
   * <p>as a placeholder inside another message or a webhook payload.
   */
  public String plain(String key) {

    return plain(key, Map.of());
  }

  public String plain(String key, Map<String, String> placeholders) {

    return PlainTextComponentSerializer.plainText().serialize(component(key, placeholders));
  }

  public String plain(Component component) {

    return PlainTextComponentSerializer.plainText().serialize(component);
  }

  public List<Component> components(String key) {

    return components(key, Map.of());
  }

  public List<Component> components(String key, Map<String, String> placeholders) {

    List<String> lines = rawList(key);

    if (lines.isEmpty()) {

      return List.of(component(key, placeholders));
    }

    List<Component> result = new ArrayList<>(lines.size());

    for (String line : lines) {

      result.add(render(line, placeholders));
    }

    return result;
  }

  public Component render(String template, Map<String, String> placeholders) {

    if (template == null) {

      return Component.empty();
    }

    try {

      if (placeholders == null || placeholders.isEmpty()) {

        return MINI_MESSAGE.deserialize(template);
      }

      // Messages use the familiar %name% token syntax, while MiniMessage

      // resolvers are tag based. Rewriting the trusted template tokens to

      // tags keeps the user-supplied values unparsed and injection safe.

      String prepared = template;

      for (String key : placeholders.keySet()) {

        if (key == null || key.isBlank()) {

          continue;
        }

        prepared = prepared.replace("%" + key + "%", "<" + key + ">");
      }

      TagResolver[] resolvers =
          placeholders.entrySet().stream()
              .map(
                  entry ->
                      Placeholder.unparsed(
                          entry.getKey(), entry.getValue() == null ? "" : entry.getValue()))
              .toArray(TagResolver[]::new);

      return MINI_MESSAGE.deserialize(prepared, resolvers);

    } catch (RuntimeException exception) {

      return Component.text(template);
    }
  }

  public DateTimeFormatter dateFormatter() {

    String pattern = raw(MessageKeys.TIME_FORMAT_DATE);

    try {

      return DateTimeFormatter.ofPattern(pattern);

    } catch (IllegalArgumentException | DateTimeParseException exception) {

      return DEFAULT_DATE;
    }
  }

  /**
   * Localized display name for a report category. The language files may define
   *
   * <p>{@code categories.<id>.name}; when they do, it wins over the config value so the GUI and
   * command output follow the selected language. Returns null when the current language has no
   * entry, so callers can fall back to the config display name.
   */
  public String categoryName(String categoryId) {

    if (categoryId == null || categoryId.isBlank()) {

      return null;
    }

    String id = categoryId.trim().toLowerCase(Locale.ROOT);

    if (!values.containsKey("categories." + id + ".name")) {

      return null;
    }

    String rendered = plain("categories." + id + ".name", Map.of());

    return rendered == null || rendered.isBlank() ? null : rendered;
  }

  /**
   * Localized description lines for a report category. The language files may define
   *
   * <p>{@code categories.<id>.description} as a list; when present it wins over the config value.
   * Returns an empty list when the current language has no entry so callers can fall back to the
   * config description.
   */
  public List<String> categoryDescription(String categoryId) {

    if (categoryId == null || categoryId.isBlank()) {

      return List.of();
    }

    String id = categoryId.trim().toLowerCase(Locale.ROOT);

    List<String> lines = rawList("categories." + id + ".description");

    if (lines.isEmpty()) {

      return List.of();
    }

    return lines.stream()
        .map(line -> plain(render(line, Map.of())))
        .filter(s -> !s.isBlank())
        .toList();
  }

  private static Map<String, Object> read(Path path, Consumer<String> warning) {

    if (path == null || !Files.isRegularFile(path)) {

      return Map.of();
    }

    try (InputStream input = Files.newInputStream(path)) {

      return read(input);

    } catch (IOException | RuntimeException exception) {

      if (warning != null) {

        warning.accept("Failed to read messages from " + path + ": " + exception.getMessage());
      }

      return Map.of();
    }
  }

  /**
   * Reads the bundled copy of the selected language so newly added keys are
   *
   * <p>available even when the server still has an older lang file on disk.
   */
  private static Map<String, Object> readBundled(Path path, Consumer<String> warning) {

    if (path == null) {

      return Map.of();
    }

    String name = path.getFileName().toString();

    try (InputStream input = Messages.class.getResourceAsStream("/lang/" + name)) {

      if (input == null) {

        return Map.of();
      }

      return read(input);

    } catch (IOException | RuntimeException exception) {

      if (warning != null) {

        warning.accept("Failed to read bundled messages " + name + ": " + exception.getMessage());
      }

      return Map.of();
    }
  }

  private static Map<String, Object> read(InputStream input) {

    Object loaded = new Yaml().load(input);

    if (!(loaded instanceof Map<?, ?> map)) {

      return Map.of();
    }

    Map<String, Object> flattened = new LinkedHashMap<>();

    Map<String, Object> stringMap = new LinkedHashMap<>();

    map.forEach((key, value) -> stringMap.put(String.valueOf(key), value));

    flatten("", stringMap, flattened);

    return flattened;
  }

  private static void flatten(String prefix, Map<?, ?> map, Map<String, Object> output) {

    for (Map.Entry<?, ?> entry : map.entrySet()) {

      String key =
          prefix.isEmpty() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();

      Object value = entry.getValue();

      if (value instanceof Map<?, ?> nested) {

        flatten(key, nested, output);

      } else if (value != null) {

        output.put(key, value);
      }
    }
  }
}
