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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

/** Regression: partial or legacy evidence JSON must load with safe defaults, never NPE. */
class EvidenceTest {

  @Test
  void partialJsonLoadsSafeDefaults() {

    Evidence loaded = new Gson().fromJson("{\"reportId\": 7}", Evidence.class);

    assertEquals(7L, loaded.reportId());

    assertEquals("", loaded.targetName());

    assertEquals("", loaded.world());

    assertEquals("", loaded.gameMode());

    assertTrue(loaded.inventory().isEmpty());

    assertTrue(loaded.armor().isEmpty());

    assertTrue(loaded.chat().isEmpty());

    assertTrue(loaded.blockBreaks().isEmpty());

    assertTrue(loaded.movement().isEmpty());

    assertEquals("", loaded.server());
  }

  @Test
  void emptyJsonLoadsAllSafeDefaults() {

    Evidence loaded = new Gson().fromJson("{}", Evidence.class);

    assertEquals(0L, loaded.reportId());

    assertEquals("", loaded.world());

    assertTrue(loaded.inventory().isEmpty());
  }
}
