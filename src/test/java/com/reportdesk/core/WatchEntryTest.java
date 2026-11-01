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
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class WatchEntryTest {


@Test

 void normalizesNameAndTimestamp() {



 WatchEntry entry = new WatchEntry(null, " Bob ", -1L);




assertEquals(new java.util.UUID(0L, 0L), entry.staffId());



 assertEquals("Bob", entry.targetName());



 assertEquals(0L, entry.createdAt());



 assertEquals("bob", entry.targetKey());



 assertFalse(entry.valid());

 }


@Test

 void validEntryExposesLowerCaseKey() {



 WatchEntry entry = new WatchEntry(TestFixtures.STAFF, "Charlie", 1_000L);




assertTrue(entry.valid());



 assertEquals("charlie", entry.targetKey());

 } }
