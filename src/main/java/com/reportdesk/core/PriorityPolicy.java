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
import java.util.Map;
/**
* Pure triage policy: turns a category severity plus the target's current open
* report count into an effective priority and an SLA deadline.
*/ public record PriorityPolicy(

 boolean enabled,

 int repeatOffenderThreshold,

 ReportPriority repeatOffenderLevel,

 Map<ReportPriority, Integer> slaHours ) {


public PriorityPolicy {



 repeatOffenderLevel = repeatOffenderLevel == null





 ? ReportPriority.HIGH : repeatOffenderLevel;



 slaHours = slaHours == null ? Map.of() : Map.copyOf(slaHours);

 }


public ReportPriority resolve(ReportPriority categorySeverity, int openForTarget) {



 ReportPriority base = categorySeverity == null ? ReportPriority.NORMAL : categorySeverity;



 if (!enabled) {





 return base;



 }



 if (repeatOffenderThreshold > 0 && openForTarget >= repeatOffenderThreshold





 && repeatOffenderLevel.weight() > base.weight()) {





 return repeatOffenderLevel;



 }



 return base;

 }


public long dueAt(ReportPriority priority, long createdAt) {



 if (!enabled || createdAt <= 0L) {





 return 0L;



 }



 int hours = slaHours.getOrDefault(priority == null ? ReportPriority.NORMAL : priority, 0);



 return hours <= 0 ? 0L : createdAt + hours * 3_600_000L;

 }


public int slaHours(ReportPriority priority) {



 return slaHours.getOrDefault(priority == null ? ReportPriority.NORMAL : priority, 0);

 } }
