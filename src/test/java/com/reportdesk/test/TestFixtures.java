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

package com.reportdesk.test;
import com.reportdesk.core.EvidenceMeta; import com.reportdesk.core.Report; import com.reportdesk.core.ReportStatus;
import java.util.List; import java.util.UUID;
/**
* Shared immutable report fixtures for the pure unit tests.
*/ public final class TestFixtures {


public static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");

 public static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

 public static final UUID STAFF = UUID.fromString("33333333-3333-3333-3333-333333333333");


private TestFixtures() {

 }


public static Report open(long id, UUID reporter, String reporterName, String target,














 String category, long createdAt) {



 return report(id, reporter, reporterName, target, category, ReportStatus.OPEN,





 createdAt, createdAt, 0L);

 }


public static Report report(long id, UUID reporter, String reporterName, String target,















 String category, ReportStatus status, long createdAt, long updatedAt,















 long closedAt) {



 return new Report(





 id,





 reporter,





 reporterName,





 null,





 target,





 category,





 "detail " + id,





 status,





 createdAt,





 updatedAt,





 null,





 "",





 0L,





 null,





 "",





 closedAt,





 "",





 "",





 List.of(),





 EvidenceMeta.none(),





 false,





 "test"



 );

 } }
