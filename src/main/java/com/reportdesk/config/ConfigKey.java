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

package com.reportdesk.config;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * One scalar setting that can be changed in game through {@code /reports config set}.
 *
 * <p>Every key carries its own parser and a getter that reads the effective value from a validated
 * {@link PluginConfig}. Editing therefore reuses the exact same validation and clamping rules as
 * config.yml loading instead of duplicating ranges in the command layer.
 */
public record ConfigKey(
    String path,
    Kind kind,
    List<String> suggestions,
    Function<PluginConfig, Object> getter,
    boolean restartRequired) {

  public enum Kind {
    BOOLEAN,

    INTEGER,

    LONG,

    STRING,

    CHOICE
  }

  public ConfigKey {

    path = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);

    kind = kind == null ? Kind.STRING : kind;

    suggestions =
        suggestions == null
            ? List.of()
            : suggestions.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .toList();

    Objects.requireNonNull(getter, "getter");
  }

  public Object current(PluginConfig config) {

    return config == null ? null : getter.apply(config);
  }

  /**
   * Parses a raw command argument. The returned object is already normalized
   *
   * <p>(lower-case choices, trimmed strings) so it can be written to YAML.
   *
   * @throws IllegalArgumentException with a short, user-facing reason
   */
  public Object parse(String raw) {

    String value = raw == null ? "" : raw.trim();

    return switch (kind) {
      case BOOLEAN -> parseBoolean(value);

      case INTEGER -> parseInteger(value);

      case LONG -> parseLong(value);

      case STRING -> parseString(value);

      case CHOICE -> parseChoice(value);
    };
  }

  /**
   * True when the validated config kept exactly the requested value. A value
   *
   * <p>that was clamped, ignored or replaced is rejected instead of silently
   *
   * <p>written to disk.
   */
  public boolean matches(Object parsed, Object effective) {

    if (parsed == null || effective == null) {

      return false;
    }

    return switch (kind) {
      case BOOLEAN -> Objects.equals(parsed, effective);

      case INTEGER, LONG -> ((Number) parsed).longValue() == ((Number) effective).longValue();

      case STRING, CHOICE -> String.valueOf(parsed).equals(String.valueOf(effective));
    };
  }

  public String display(Object value) {

    return value == null ? "" : String.valueOf(value);
  }

  private boolean parseBoolean(String value) {

    String normalized = value.toLowerCase(Locale.ROOT);

    return switch (normalized) {
      case "true", "on", "yes", "1" -> true;

      case "false", "off", "no", "0" -> false;

      default ->
          throw new IllegalArgumentException("expected true/false (also on/off, yes/no, 1/0)");
    };
  }

  private int parseInteger(String value) {

    try {

      long parsed = Long.parseLong(value);

      if (parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {

        throw new IllegalArgumentException("number is outside the integer range");
      }

      return (int) parsed;

    } catch (NumberFormatException exception) {

      throw new IllegalArgumentException("expected a whole number");
    }
  }

  private long parseLong(String value) {

    try {

      return Long.parseLong(value);

    } catch (NumberFormatException exception) {

      throw new IllegalArgumentException("expected a whole number");
    }
  }

  private String parseString(String value) {

    String result = value;

    if (result.length() >= 2) {

      char first = result.charAt(0);

      char last = result.charAt(result.length() - 1);

      if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {

        result = result.substring(1, result.length() - 1).trim();
      }
    }

    for (int index = 0; index < result.length(); index++) {

      char character = result.charAt(index);

      if (character == '\n' || character == '\r' || Character.isISOControl(character)) {

        throw new IllegalArgumentException("value must stay on a single line");
      }
    }

    return result;
  }

  private String parseChoice(String value) {

    String normalized = value.toLowerCase(Locale.ROOT);

    if (!suggestions.contains(normalized)) {

      throw new IllegalArgumentException("expected one of: " + String.join(", ", suggestions));
    }

    return normalized;
  }
}
