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

package com.reportdesk.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.core.ReportStatus;
import com.reportdesk.test.TestFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Behaviour of the lifecycle events without a live Bukkit server. */
class ReportEventsTest {

  @Test
  void preSubmitDefaultsNotCancelledAndExposesPayload() {

    ReportPreSubmitEvent event =
        new ReportPreSubmitEvent(TestFixtures.ALICE, "Alice", "Bob", "cheat", "flying");

    assertFalse(event.isCancelled());

    assertEquals(TestFixtures.ALICE, event.getReporterId());

    assertEquals("Alice", event.getReporterName());

    assertEquals("Bob", event.getTargetName());

    assertEquals("cheat", event.getCategory());

    assertEquals("flying", event.getDetail());

    event.setCancelled(true);

    assertTrue(event.isCancelled());
  }

  @Test
  void preSubmitNormalisesNullPayloads() {

    ReportPreSubmitEvent event = new ReportPreSubmitEvent(null, null, null, null, null);

    assertEquals(new UUID(0L, 0L), event.getReporterId());

    assertEquals("", event.getReporterName());

    assertEquals("", event.getTargetName());

    assertEquals("", event.getCategory());

    assertEquals("", event.getDetail());
  }

  @Test
  void submittedExposesReportAndSource() {

    var report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    ReportSubmittedEvent event = new ReportSubmittedEvent(report, TestFixtures.ALICE, "gui");

    assertEquals(report, event.getReport());

    assertEquals(TestFixtures.ALICE, event.getReporterId());

    assertEquals("gui", event.getSource());
  }

  @Test
  void submittedNormalisesNullSourceAndReporter() {

    var report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    ReportSubmittedEvent event = new ReportSubmittedEvent(report, null, null);

    assertEquals(new UUID(0L, 0L), event.getReporterId());

    assertEquals("", event.getSource());

    assertNotNull(event.getReport());
  }

  @Test
  void statusChangedExposesTransition() {

    var open = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    ReportStatusChangedEvent event =
        new ReportStatusChangedEvent(open, ReportStatus.OPEN, ReportStatus.RESOLVED);

    assertEquals(ReportStatus.OPEN, event.getFrom());

    assertEquals(ReportStatus.RESOLVED, event.getTo());

    assertEquals(open, event.getReport());
  }

  @Test
  void statusChangedNormalisesNullTransitions() {

    var open = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    ReportStatusChangedEvent event = new ReportStatusChangedEvent(open, null, null);

    assertEquals(ReportStatus.OPEN, event.getFrom());

    assertEquals(ReportStatus.OPEN, event.getTo());
  }
}
