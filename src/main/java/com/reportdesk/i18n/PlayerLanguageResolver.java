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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Resolves a player's preferred UI language from their client, so the plugin can render the
 * player-facing parts of the workflow in that player's own language and fall back to the
 * server's default language when the client language is not supported.
 *
 * <p>Detection is best-effort and safe: the Java client's locale is read reflectively (it changes
 * with the Paper build and is not a stable API), and every reflective step is wrapped so a
 * failure simply means "unknown" — which degrades to the default language and never throws.
 * Bedrock clients joining through Floodgate expose a locale too; that path is probed first and
 * falls back to the Java path.
 *
 * <p>The mapping from a raw client tag to one of the bundled languages is a pure function
 * ({@link #mapToSupportedLanguage(String)}) that is unit-testable without a Bukkit server.
 */
public final class PlayerLanguageResolver {

  /** The 12 bundled language files this plugin can render. */
  public static final Set<String> SUPPORTED =
      new LinkedHashSet<>(
          Set.of(
              "zh_CN", "zh_TW", "en_US", "pt_BR", "es_ES", "ru_RU", "de_DE", "fr_FR", "tr_TR",
              "pl_PL", "ja_JP", "ko_KR"));

  /** Maps a raw client language tag to a supported bundled language, or null when unsupported. */
  private static final Map<String, String> TAG_TO_LANGUAGE =
      Map.ofEntries(
          Map.entry("zh", "zh_CN"),
          Map.entry("en", "en_US"),
          Map.entry("pt", "pt_BR"),
          Map.entry("es", "es_ES"),
          Map.entry("ru", "ru_RU"),
          Map.entry("de", "de_DE"),
          Map.entry("fr", "fr_FR"),
          Map.entry("tr", "tr_TR"),
          Map.entry("pl", "pl_PL"),
          Map.entry("ja", "ja_JP"),
          Map.entry("ko", "ko_KR"));

  private final ConcurrentHashMap<UUID, String> cache = new ConcurrentHashMap<>();

  /**
   * Pure BCP47 / Java-locale tag to supported-language mapping.
   *
   * <p>A tag like {@code en-US}, {@code en_US}, {@code zh_TW} or plain {@code de} is normalised:
   * the first subtag is the language, the second (if present) the region. Simplified Chinese
   * resolves to {@code zh_CN} while the Traditional variants ({@code zh_TW}/{@code zh_HK}/
   * {@code zh_MO}) resolve to {@code zh_TW}. A tag whose language is not among the bundled set
   * returns {@code null} so the caller can fall back to the server default.
   *
   * @param tag a client language tag, or {@code null}
   * @return a bundled language code, or {@code null} when the tag is unsupported/blank
   */
  public static String mapToSupportedLanguage(String tag) {

    if (tag == null || tag.isBlank()) {

      return null;
    }

    // Java's Locale.toString() yields "en_US" / "zh_TW"; BCP47 yields "en-US".
    String normalised = tag.trim().replace('_', '-');

    String[] parts = normalised.split("-");

    String language = parts[0].toLowerCase(Locale.ROOT);

    if (language.equals("zh") && parts.length >= 2) {

      String region = parts[1].toUpperCase(Locale.ROOT);

      if (region.equals("TW") || region.equals("HK") || region.equals("MO")) {

        return "zh_TW";
      }

      return "zh_CN";
    }

    return TAG_TO_LANGUAGE.get(language);
  }

  /** Best-effort detection of a player's client language tag; returns null when unknown. */
  public String detect(Player player) {

    return detect(player, true);
  }

  /**
   * Same as {@link #detect(Player)} but the Java-client reflective probe is only attempted when
   * {@code allowJavaDetect} is true. The Bedrock (Floodgate) probe is always safe to run.
   */
  public String detect(Player player, boolean allowJavaDetect) {

    if (player == null) {

      return null;
    }

    String bedrock = detectBedrockLocale(player);

    if (bedrock != null) {

      return bedrock;
    }

    if (!allowJavaDetect) {

      return null;
    }

    return detectJavaLocale(player);
  }

  /**
   * Detects and caches the player's supported language. An unsupported or undetectable client
   * language stores nothing, so lookups fall back to the server default.
   *
   * @return the resolved bundled language, or {@code null} when unknown/unsupported
   */
  public String resolve(Player player, boolean allowJavaDetect) {

    if (player == null) {

      return null;
    }

    String language = mapToSupportedLanguage(detect(player, allowJavaDetect));

    if (language != null) {

      cache(player.getUniqueId(), language);
    }

    return language;
  }

  /** Stores a resolved language for a player so later lookups are pure in-memory. */
  public void cache(UUID playerId, String language) {

    if (playerId != null && language != null && !language.isBlank()) {

      cache.put(playerId, language);
    }
  }

  public void clear(UUID playerId) {

    cache.remove(playerId);
  }

  public Optional<String> cached(UUID playerId) {

    String value = cache.get(playerId);

    return value == null ? Optional.empty() : Optional.of(value);
  }

  public int size() {

    return cache.size();
  }

  /**
   * Floodgate (Bedrock) clients carry their locale on the proxied player object. Probed through
   * the Floodgate API ({@code getInstance().getPlayer(uuid).getLocale()}) so a server without
   * Floodgate simply skips this path.
   */
  private String detectBedrockLocale(Player player) {

    try {

      Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");

      Method getInstance = apiClass.getMethod("getInstance");

      Object api = getInstance.invoke(null);

      if (api == null) {

        return null;
      }

      Method getPlayer = apiClass.getMethod("getPlayer", UUID.class);

      Object floodgatePlayer = getPlayer.invoke(api, player.getUniqueId());

      if (floodgatePlayer == null) {

        return null;
      }

      Method getLocale = floodgatePlayer.getClass().getMethod("getLocale");

      Object locale = getLocale.invoke(floodgatePlayer);

      if (locale instanceof java.util.Locale javaLocale) {

        return javaLocale.toString();
      }

      return locale == null ? null : String.valueOf(locale);

    } catch (Exception exception) {

      return null;
    }
  }

  /**
   * Java client locale, read reflectively through the craft handle's packet listener. Paper uses
   * Mojang mappings where the field is {@code locale} on the connection; older Spigot builds use
   * a different name, so several candidates are tried.
   */
  private String detectJavaLocale(Player player) {

    try {

      Method handleMethod = findMethod(player.getClass(), "getHandle");

      if (handleMethod == null) {

        return null;
      }

      Object handle = handleMethod.invoke(player);

      if (handle == null) {

        return null;
      }

      Object connection = findField(handle, "connection");

      if (connection == null) {

        return null;
      }

      Object locale = findFirstField(connection, new String[] {"locale", "language"});

      if (locale instanceof java.util.Locale javaLocale) {

        return javaLocale.toString();
      }

      if (locale instanceof String string) {

        return string;
      }

      return null;

    } catch (RuntimeException | ReflectiveOperationException exception) {

      return null;
    }
  }

  private static Method findMethod(Class<?> type, String name) {

    for (Class<?> current = type; current != null && current != Object.class;
        current = current.getSuperclass()) {

      try {

        Method method = current.getDeclaredMethod(name);

        method.setAccessible(true);

        return method;

      } catch (Exception ignored) {

        // getDeclaredMethod throws NoSuchMethodException (checked); treat any reflective
        // miss as "not found" and keep walking the superclass chain.
      }
    }

    return null;
  }

  private static Object findField(Object target, String name) {

    for (Class<?> current = target.getClass(); current != null && current != Object.class;
        current = current.getSuperclass()) {

      try {

        Field field = current.getDeclaredField(name);

        field.setAccessible(true);

        return field.get(target);

      } catch (Exception ignored) {

        // getDeclaredField (NoSuchFieldException) and get (IllegalAccessException) are checked;
        // treat any reflective miss as "not found" and keep walking the superclass chain.
      }
    }

    return null;
  }

  private static Object findFirstField(Object target, String[] candidateNames) {

    for (String name : candidateNames) {

      Object value = findField(target, name);

      if (value != null) {

        return value;
      }
    }

    return null;
  }
}
