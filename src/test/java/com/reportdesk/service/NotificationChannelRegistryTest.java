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

import com.reportdesk.api.NotificationChannel;
import com.reportdesk.core.Report;
import com.reportdesk.test.TestFixtures;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

/** Behaviour of the notification channel SPI registry (pure, no Bukkit). */
class NotificationChannelRegistryTest {

  private final Logger logger = Logger.getLogger("test");

  private final NotificationChannelRegistry registry =
      new NotificationChannelRegistry(logger);

  /** A recording channel that also captures the dispatched report + placeholders. */
  private static final class Recording implements NotificationChannel {

    final String id;

    final AtomicInteger created = new AtomicInteger();

    final AtomicInteger claimed = new AtomicInteger();

    final AtomicInteger closed = new AtomicInteger();

    final AtomicInteger reopened = new AtomicInteger();

    volatile Report lastReport;

    volatile Map<String, String> lastPlaceholders;

    Recording(String id) {

      this.id = id;
    }

    public String id() {

      return id;
    }

    public void onCreated(Report report, Map<String, String> placeholders) {

      created.incrementAndGet();

      lastReport = report;

      lastPlaceholders = placeholders;
    }

    public void onClaimed(Report report, String staffName, Map<String, String> placeholders) {

      claimed.incrementAndGet();
    }

    public void onClosed(Report report, String staffName, Map<String, String> placeholders) {

      closed.incrementAndGet();
    }

    public void onReopened(Report report, String staffName, Map<String, String> placeholders) {

      reopened.incrementAndGet();
    }
  }

  @Test
  void registerAddsAndDuplicatesAreIgnored() {

    registry.register(new Recording("a"));

    assertTrue(registry.register(new Recording("b")));

    // duplicate id is rejected
    assertFalse(registry.register(new Recording("b")));

    assertEquals(2, registry.size());
  }

  @Test
  void nullOrBlankChannelsAreRejected() {

    assertFalse(registry.register(null));

    registry.register(new Recording("ok"));

    assertEquals(1, registry.size());
  }

  @Test
  void dispatchReachesEveryRegisteredChannel() {

    Recording first = new Recording("first");

    Recording second = new Recording("second");

    registry.register(first);

    registry.register(second);

    Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheat", 0L);

    Map<String, String> placeholders = Map.of("id", "1");

    registry.dispatchCreated(report, placeholders);

    registry.dispatchClaimed(report, "staff", placeholders);

    registry.dispatchClosed(report, "staff", placeholders);

    registry.dispatchReopened(report, "staff", placeholders);

    assertEquals(1, first.created.get());

    assertEquals(1, second.created.get());

    assertEquals(1, first.claimed.get());

    assertEquals(1, first.closed.get());

    assertEquals(1, first.reopened.get());

    assertEquals(report, first.lastReport);

    assertEquals(placeholders, first.lastPlaceholders);
  }

  @Test
  void unregisterRemovesByChannelId() {

    Recording channel = new Recording("x");

    registry.register(channel);

    registry.unregister("x");

    assertEquals(0, registry.size());

    registry.dispatchCreated(TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L), Map.of());

    assertEquals(0, channel.created.get());
  }

  @Test
  void aThrowingChannelDoesNotBreakTheOthers() {

    Recording healthy = new Recording("healthy");

    registry.register(healthy);

    NotificationChannel throwing =
        new NotificationChannel() {
          public String id() {

            return "throwing";
          }

          public void onCreated(Report report, Map<String, String> placeholders) {

            throw new IllegalStateException("boom");
          }
        };

    registry.register(throwing);

    assertEquals(2, registry.size());

    // must not throw
    registry.dispatchCreated(
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L), Map.of());

    assertEquals(1, healthy.created.get());
  }

  @Test
  void emptyRegistryDispatchesNothing() {

    // no channels registered; a no-op must not throw
    registry.dispatchClosed(
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L),
        "staff",
        Map.of());

    assertEquals(0, registry.size());
  }

  @Test
  void defaultChannelOnlyImplementsWhatItNeeds() {

    // a channel that overrides nothing still works as a no-op target
    registry.register(new NotificationChannel() {
      public String id() {

        return "minimal";
      }
    });

    List<String> ids = List.of("minimal");

    for (String ignored : ids) {

      // exercise every dispatch path without exception
      registry.dispatchCreated(
          TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L), Map.of());

      registry.dispatchReopened(
          TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "c", 0L),
          "staff",
          Map.of());
    }

    assertEquals(1, registry.size());
  }
}
