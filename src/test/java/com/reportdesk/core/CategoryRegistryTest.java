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

package com.reportdesk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CategoryRegistryTest {

  private final CategoryRegistry registry =
      CategoryRegistry.of(
          List.of(
              new ReportCategory(
                  "cheating",
                  "Cheating",
                  "DIAMOND_SWORD",
                  List.of("hack", "cheat"),
                  true,
                  100,
                  List.of()),
              new ReportCategory("chat", "Chat", "PAPER", List.of("talk"), true, 50, List.of()),
              new ReportCategory(
                  "disabled", "Disabled", "BARRIER", List.of("off"), false, 10, List.of())));

  @Test
  void lookupWorksByIdAndAliasCaseInsensitively() {

    assertEquals("cheating", registry.find("HACK").orElseThrow().id());

    assertEquals("cheating", registry.find("cheating").orElseThrow().id());

    assertTrue(registry.find("missing").isEmpty());

    assertTrue(registry.find(null).isEmpty());

    assertTrue(registry.find(" ").isEmpty());
  }

  @Test
  void enabledStateIsReported() {

    assertTrue(registry.isEnabled("chat"));

    assertFalse(registry.isEnabled("off"));

    assertFalse(registry.isEnabled("missing"));
  }

  @Test
  void sortedUsesPriorityThenIdAndFiltersDisabled() {

    assertEquals(
        List.of("cheating", "chat", "disabled"),
        registry.sorted().stream().map(ReportCategory::id).toList());

    assertEquals(
        List.of("cheating", "chat"),
        registry.enabledSorted().stream().map(ReportCategory::id).toList());
  }

  @Test
  void commandSuggestionsOnlyIncludeEnabledCategoriesAndAliases() {

    List<String> suggestions = registry.commandSuggestions();

    assertTrue(suggestions.contains("cheating"));

    assertTrue(suggestions.contains("hack"));

    assertTrue(suggestions.contains("chat"));

    assertFalse(suggestions.contains("off"));
  }

  @Test
  void emptyConfigurationFallsBackToOtherCategory() {

    CategoryRegistry fallback = CategoryRegistry.of(List.of());

    assertEquals(1, fallback.size());

    assertEquals("other", fallback.find("misc").orElseThrow().id());

    assertEquals("other", fallback.getOrDefault("missing", "also-missing").id());
  }

  @Test
  void categoryRecordNormalizesFields() {

    ReportCategory category = new ReportCategory(null, null, null, null, true, 0, null);

    assertEquals("other", category.id());

    assertEquals("other", category.display());

    assertEquals("PAPER", category.icon());

    assertEquals(List.of(), category.aliases());

    assertEquals(List.of(), category.description());
  }
}
