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

package com.reportdesk.gui;

import com.reportdesk.config.ConfigKey;
import com.reportdesk.config.PluginConfig;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure presentation helpers for the in-game config editor.
 *
 * <p>Keeping these rules outside {@link AdminGuiService} makes them easy to test without a running
 * server. They never write anything: the actual edit is still validated and persisted by the
 * asynchronous config service.
 */
final class ConfigGuiSupport {

  private ConfigGuiSupport() {}

  /** True when the live value is still the bundled default. */
  static boolean isDefault(ConfigKey key, PluginConfig live, PluginConfig defaults) {

    if (key == null || live == null || defaults == null) {

      return false;
    }

    Object current = key.current(live);

    Object fallback = key.current(defaults);

    if (current == null || fallback == null) {

      return false;
    }

    try {

      return key.matches(fallback, current);

    } catch (RuntimeException exception) {

      return fallback.equals(current);
    }
  }

  static int modifiedCount(List<ConfigKey> keys, PluginConfig live, PluginConfig defaults) {

    int modified = 0;

    for (ConfigKey key : keys) {

      if (!isDefault(key, live, defaults)) {

        modified++;
      }
    }

    return modified;
  }

  /**
   * Applies the two optional list filters. Both are independent and combine with AND, so an
   * administrator can ask for "changed settings that also need a restart". A disabled filter
   * never touches the list, which keeps the default view allocation-free.
   */
  static List<ConfigKey> filter(
      List<ConfigKey> keys,
      PluginConfig live,
      PluginConfig defaults,
      boolean onlyModified,
      boolean onlyRestart) {

    if (keys == null || keys.isEmpty()) {

      return List.of();
    }

    if (!onlyModified && !onlyRestart) {

      return keys;
    }

    return keys.stream()
        .filter(key -> !onlyModified || !isDefault(key, live, defaults))
        .filter(key -> !onlyRestart || key.restartRequired())
        .toList();
  }

  /**
   * The value a left click should write for keys that can be changed without
   *
   * <p>typing. Booleans toggle; choices advance to the next suggestion. Numbers
   *
   * <p>and free-form strings return {@link Optional#empty()} so the caller can
   *
   * <p>open the chat prompt instead.
   */
  static Optional<String> quickChange(ConfigKey key, PluginConfig live) {

    if (key == null || live == null) {

      return Optional.empty();
    }

    Object current = key.current(live);

    return switch (key.kind()) {
      case BOOLEAN -> Optional.of(String.valueOf(!Boolean.TRUE.equals(current)));

      case CHOICE -> nextChoice(key, current);

      default -> Optional.empty();
    };
  }

  static Optional<String> nextChoice(ConfigKey key, Object current) {

    if (key == null || key.suggestions().isEmpty()) {

      return Optional.empty();
    }

    String value = current == null ? "" : String.valueOf(current).toLowerCase(Locale.ROOT);

    int index = key.suggestions().indexOf(value);

    return Optional.of(key.suggestions().get((index + 1) % key.suggestions().size()));
  }

  /** Numeric adjustment used by the detail page; empty for non-numeric keys. */
  static Optional<String> adjustedValue(ConfigKey key, PluginConfig live, long delta) {

    if (key == null
        || live == null
        || (key.kind() != ConfigKey.Kind.INTEGER && key.kind() != ConfigKey.Kind.LONG)) {

      return Optional.empty();
    }

    Object current = key.current(live);

    if (!(current instanceof Number number)) {

      return Optional.empty();
    }

    long value;

    try {

      value = Math.addExact(number.longValue(), delta);

    } catch (ArithmeticException exception) {

      return Optional.empty();
    }

    if (key.kind() == ConfigKey.Kind.INTEGER
        && (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)) {

      return Optional.empty();
    }

    return Optional.of(Long.toString(value));
  }

  /** Steps shown as -100/-10/-1 and +1/+10/+100 on the detail page. */
  static long[] adjustSteps(ConfigKey key) {

    return new long[] {1L, 10L, 100L};
  }

  /** Case-insensitive match against the config path and the current value. */
  static boolean matchesQuery(ConfigKey key, PluginConfig live, String query) {

    return matchesQuery(key, live, query, new String[0]);
  }

  /**
   * Case-insensitive match against the config path, the current value and
   *
   * <p>any localized text supplied by the caller (label and description). This
   *
   * <p>lets administrators search with the words they actually see in the GUI
   *
   * <p>instead of having to know the English config key.
   */
  static boolean matchesQuery(
      ConfigKey key, PluginConfig live, String query, String... localizedText) {

    if (key == null) {

      return false;
    }

    if (query == null || query.isBlank()) {

      return true;
    }

    String needle = query.trim().toLowerCase(Locale.ROOT);

    if (key.path().toLowerCase(Locale.ROOT).contains(needle)) {

      return true;
    }

    if (localizedText != null) {

      for (String text : localizedText) {

        if (text != null && text.toLowerCase(Locale.ROOT).contains(needle)) {

          return true;
        }
      }
    }

    String value = key.display(key.current(live));

    return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
  }
}
