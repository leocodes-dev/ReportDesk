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

package com.reportdesk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpHasherTest {

  @Test
  void hashIsDeterministic() {

    assertEquals(IpHasher.hash("salt", "1.2.3.4"), IpHasher.hash("salt", "1.2.3.4"));
  }

  @Test
  void differentSaltChangesHash() {

    assertNotEquals(IpHasher.hash("a", "1.2.3.4"), IpHasher.hash("b", "1.2.3.4"));
  }

  @Test
  void blankIpHashesToEmpty() {

    assertEquals("", IpHasher.hash("salt", ""));

    assertEquals("", IpHasher.hash("salt", "   "));

    assertEquals("", IpHasher.hash("salt", null));
  }

  @Test
  void nullSaltIsAllowed() {

    assertEquals(IpHasher.hash(null, "1.2.3.4"), IpHasher.hash("", "1.2.3.4"));
  }

  @Test
  void hashLooksLikeLowerHexSha256() {

    String hash = IpHasher.hash("s", "10.0.0.1");

    assertEquals(64, hash.length());

    assertTrue(hash.matches("[0-9a-f]{64}"));
  }
}
