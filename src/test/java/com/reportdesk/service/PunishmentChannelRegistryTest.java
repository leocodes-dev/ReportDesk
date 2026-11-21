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

package com.reportdesk.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.api.PunishmentChannel;
import com.reportdesk.core.Report;
import com.reportdesk.core.Verdict;
import com.reportdesk.test.TestFixtures;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/** Behaviour of the punishment channel SPI registry (pure, no Bukkit). */
class PunishmentChannelRegistryTest {

  private final Logger logger = Logger.getLogger("test");

  private final PunishmentChannelRegistry registry =
      new PunishmentChannelRegistry(logger);

  private static final class Recording implements PunishmentChannel {

    final String id;

    final AtomicInteger punished = new AtomicInteger();

    Recording(String id) {

      this.id = id;
    }

    public String id() {

      return id;
    }

    public void onPunished(Report report, Verdict verdict, String staffName) {

      punished.incrementAndGet();
    }
  }

  @Test
  void registerAddsAndDuplicatesAreIgnored() {

    registry.register(new Recording("a"));

    assertTrue(registry.register(new Recording("b")));

    assertFalse(registry.register(new Recording("b")));

    assertEquals(2, registry.size());
  }

  @Test
  void nullChannelIsRejected() {

    assertFalse(registry.register(null));

    assertEquals(0, registry.size());
  }

  @Test
  void dispatchReachesEveryChannel() {

    Recording first = new Recording("first");

    Recording second = new Recording("second");

    registry.register(first);

    registry.register(second);

    Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    registry.dispatch(report, Verdict.RESOLVED, "staff");

    assertEquals(1, first.punished.get());

    assertEquals(1, second.punished.get());
  }

  @Test
  void unregisterRemovesByChannelId() {

    Recording channel = new Recording("x");

    registry.register(channel);

    registry.unregister("x");

    assertEquals(0, registry.size());

    registry.dispatch(
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L),
        Verdict.RESOLVED,
        "staff");

    assertEquals(0, channel.punished.get());
  }

  @Test
  void aThrowingChannelDoesNotBreakTheOthers() {

    Recording healthy = new Recording("healthy");

    registry.register(healthy);

    PunishmentChannel throwing =
        new PunishmentChannel() {
          public String id() {

            return "throwing";
          }

          public void onPunished(Report report, Verdict verdict, String staffName) {

            throw new IllegalStateException("boom");
          }
        };

    registry.register(throwing);

    assertEquals(2, registry.size());

    // must not throw
    registry.dispatch(
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L),
        Verdict.RESOLVED,
        "staff");

    assertEquals(1, healthy.punished.get());
  }

  @Test
  void emptyRegistryDispatchesNothing() {

    registry.dispatch(
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L),
        Verdict.RESOLVED,
        "staff");

    assertEquals(0, registry.size());
  }
}
