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
import org.junit.jupiter.api.Test;
import java.util.EnumMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
class PriorityPolicyTest {


private static Map<ReportPriority, Integer> hours(int urgent, int high, int normal, int low) {



 Map<ReportPriority, Integer> map = new EnumMap<>(ReportPriority.class);



 map.put(ReportPriority.URGENT, urgent);



 map.put(ReportPriority.HIGH, high);



 map.put(ReportPriority.NORMAL, normal);



 map.put(ReportPriority.LOW, low);



 return map;

 }


@Test

 void disabledPolicyKeepsCategorySeverityAndSkipsSla() {



 PriorityPolicy policy = new PriorityPolicy(false, 1, ReportPriority.URGENT,





 hours(2, 6, 24, 72));




assertEquals(ReportPriority.HIGH, policy.resolve(ReportPriority.HIGH, 100));



 assertEquals(0L, policy.dueAt(ReportPriority.HIGH, 1_000L));

 }


@Test

 void repeatOffendersAreEscalatedButNeverDowngraded() {



 PriorityPolicy policy = new PriorityPolicy(true, 3, ReportPriority.HIGH,





 hours(2, 6, 24, 72));




assertEquals(ReportPriority.NORMAL, policy.resolve(ReportPriority.NORMAL, 2));



 assertEquals(ReportPriority.HIGH, policy.resolve(ReportPriority.NORMAL, 3));



 assertEquals(ReportPriority.URGENT, policy.resolve(ReportPriority.URGENT, 5));

 }


@Test

 void repeatOffenderEscalationCanBeDisabled() {



 PriorityPolicy policy = new PriorityPolicy(true, 0, ReportPriority.URGENT,





 hours(2, 6, 24, 72));




assertEquals(ReportPriority.LOW, policy.resolve(ReportPriority.LOW, 50));

 }


@Test

 void dueAtUsesPerPriorityHours() {



 PriorityPolicy policy = new PriorityPolicy(true, 0, ReportPriority.HIGH,





 hours(2, 6, 24, 72));




long created = 1_000_000L;



 assertEquals(created + 2L * 3_600_000L, policy.dueAt(ReportPriority.URGENT, created));



 assertEquals(created + 24L * 3_600_000L, policy.dueAt(ReportPriority.NORMAL, created));



 assertEquals(created + 72L * 3_600_000L, policy.dueAt(ReportPriority.LOW, created));



 assertEquals(0L, policy.dueAt(ReportPriority.HIGH, 0L));

 }


@Test

 void zeroHoursMeanNoDeadline() {



 PriorityPolicy policy = new PriorityPolicy(true, 0, ReportPriority.HIGH,





 hours(0, 0, 0, 0));




assertEquals(0L, policy.dueAt(ReportPriority.URGENT, 5_000L));



 assertEquals(0, policy.slaHours(ReportPriority.URGENT));

 }


@Test

 void nullSeverityFallsBackToNormal() {



 PriorityPolicy policy = new PriorityPolicy(true, 0, ReportPriority.HIGH,





 hours(2, 6, 24, 72));




assertEquals(ReportPriority.NORMAL, policy.resolve(null, 0));



 assertEquals(24, policy.slaHours(null));

 } }
