/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.core;
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertThrows; import static org.junit.jupiter.api.Assertions.assertTrue;
class PendingNoticeTest {


@Test

 void normalizesNullsAndCopiesPlaceholders() {



 Map<String, String> source = new HashMap<>();



 source.put("id", null);



 source.put("", "ignored");



 source.put(null, "ignored");




PendingNotice notice = new PendingNotice(null, " notify.reporter-reply ", source, -5L);




assertEquals(new java.util.UUID(0L, 0L), notice.playerId());



 assertEquals("notify.reporter-reply", notice.messageKey());



 assertEquals("", notice.placeholders().get("id"));



 assertEquals(1, notice.placeholders().size());



 assertEquals(0L, notice.createdAt());



 assertFalse(notice.valid());

 }


@Test

 void placeholdersAreImmutableAndValidNoticePasses() {



 PendingNotice notice = new PendingNotice(TestFixtures.ALICE, "notify.reporter-closed",





 Map.of("id", "3"), 10L);




assertTrue(notice.valid());



 assertThrows(UnsupportedOperationException.class,





 () -> notice.placeholders().put("id", "4"));

 } }
