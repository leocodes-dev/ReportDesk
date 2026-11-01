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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Small deterministic clock for limiter tests. */
final class MutableClock extends Clock {

  private Instant instant;

  private final ZoneId zone;

  MutableClock(Instant instant) {

    this(instant, ZoneOffset.UTC);
  }

  private MutableClock(Instant instant, ZoneId zone) {

    this.instant = instant;

    this.zone = zone;
  }

  void advanceSeconds(long seconds) {

    instant = instant.plusSeconds(seconds);
  }

  @Override
  public ZoneId getZone() {

    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {

    return new MutableClock(instant, zone);
  }

  @Override
  public Instant instant() {

    return instant;
  }
}
