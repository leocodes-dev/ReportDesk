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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pure-function tests for the client-language to bundled-language mapping that
 * {@link PlayerLanguageResolver} exposes for per-player UI language selection.
 */
class PlayerLanguageResolverTest {

  @Test
  void mapsEachSupportedLanguageFromCommonTags() {

    assertEquals("en_US", PlayerLanguageResolver.mapToSupportedLanguage("en"));

    assertEquals("en_US", PlayerLanguageResolver.mapToSupportedLanguage("en-US"));

    assertEquals("en_US", PlayerLanguageResolver.mapToSupportedLanguage("en_GB"));

    assertEquals("zh_CN", PlayerLanguageResolver.mapToSupportedLanguage("zh"));

    assertEquals("zh_CN", PlayerLanguageResolver.mapToSupportedLanguage("zh-CN"));

    assertEquals("de_DE", PlayerLanguageResolver.mapToSupportedLanguage("de-DE"));

    assertEquals("fr_FR", PlayerLanguageResolver.mapToSupportedLanguage("fr"));

    assertEquals("ru_RU", PlayerLanguageResolver.mapToSupportedLanguage("ru-RU"));

    assertEquals("pt_BR", PlayerLanguageResolver.mapToSupportedLanguage("pt-BR"));

    assertEquals("es_ES", PlayerLanguageResolver.mapToSupportedLanguage("es"));

    assertEquals("tr_TR", PlayerLanguageResolver.mapToSupportedLanguage("tr"));

    assertEquals("pl_PL", PlayerLanguageResolver.mapToSupportedLanguage("pl"));

    assertEquals("ja_JP", PlayerLanguageResolver.mapToSupportedLanguage("ja"));

    assertEquals("ko_KR", PlayerLanguageResolver.mapToSupportedLanguage("ko-KR"));
  }

  @Test
  void traditionalChineseRegionsMapToZhTw() {

    assertEquals("zh_TW", PlayerLanguageResolver.mapToSupportedLanguage("zh-TW"));

    assertEquals("zh_TW", PlayerLanguageResolver.mapToSupportedLanguage("zh_TW"));

    assertEquals("zh_TW", PlayerLanguageResolver.mapToSupportedLanguage("zh-HK"));

    assertEquals("zh_CN", PlayerLanguageResolver.mapToSupportedLanguage("zh-SG"));
  }

  @Test
  void unsupportedAndBlankTagsReturnNull() {

    assertNull(PlayerLanguageResolver.mapToSupportedLanguage(""));

    assertNull(PlayerLanguageResolver.mapToSupportedLanguage("   "));

    assertNull(PlayerLanguageResolver.mapToSupportedLanguage(null));

    // A language the plugin does not ship a file for.
    assertNull(PlayerLanguageResolver.mapToSupportedLanguage("it"));

    assertNull(PlayerLanguageResolver.mapToSupportedLanguage("th-TH"));

    assertNull(PlayerLanguageResolver.mapToSupportedLanguage("xx"));
  }

  @Test
  void supportedSetCoversAllBundledLanguages() {

    assertTrue(
        PlayerLanguageResolver.SUPPORTED.containsAll(
            java.util.Set.of(
                "zh_CN", "zh_TW", "en_US", "pt_BR", "es_ES", "ru_RU", "de_DE", "fr_FR", "tr_TR",
                "pl_PL", "ja_JP", "ko_KR")));

    assertEquals(12, PlayerLanguageResolver.SUPPORTED.size());
  }
}
