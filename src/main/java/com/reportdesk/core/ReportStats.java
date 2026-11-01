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
import java.util.Collection; import java.util.LinkedHashMap; import java.util.List; import java.util.Map;
/**
* Pure statistics calculation used by /reports stats and the GUI.
*/ public record ReportStats(

 int total,

 int open,

 int claimed,

 int resolved,

 int rejected,

 int duplicate,

 int autoClosed,

 long averageHandlingMillis,

 Map<String, Integer> byCategory,

 Map<String, Integer> byTarget,

 Map<String, Integer> byStaff ) {


public ReportStats {



 byCategory = byCategory == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(byCategory));



 byTarget = byTarget == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(byTarget));



 byStaff = byStaff == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(byStaff));

 }


/** Backwards-compatible constructor without the staff map. */

 public ReportStats(int total, int open, int claimed, int resolved, int rejected, int duplicate,











int autoClosed, long averageHandlingMillis, Map<String, Integer> byCategory,











Map<String, Integer> byTarget) {



 this(total, open, claimed, resolved, rejected, duplicate, autoClosed,





 averageHandlingMillis, byCategory, byTarget, Map.of());

 }


public static ReportStats compute(Collection<Report> reports) {



 int open = 0;



 int claimed = 0;



 int resolved = 0;



 int rejected = 0;



 int duplicate = 0;



 int autoClosed = 0;



 long handledTotal = 0L;



 int handledCount = 0;



 Map<String, Integer> categories = new LinkedHashMap<>();



 Map<String, Integer> targets = new LinkedHashMap<>();



 Map<String, Integer> staff = new LinkedHashMap<>();




if (reports != null) {





 for (Report report : reports) {







 if (report == null) {









 continue;







 }







 switch (report.status()) {









 case OPEN -> open++;









 case CLAIMED -> claimed++;









 case RESOLVED -> resolved++;









 case REJECTED -> rejected++;









 case DUPLICATE -> duplicate++;









 case AUTO_CLOSED -> autoClosed++;







 }







 if (report.status().isTerminal() && report.closedAt() > 0 && report.closedAt() >= report.createdAt()) {









 handledTotal += report.closedAt() - report.createdAt();









 handledCount++;







 }







 categories.merge(report.category(), 1, Integer::sum);







 if (report.targetName() != null && !report.targetName().isBlank()) {









 targets.merge(report.targetName(), 1, Integer::sum);







 }







 if (report.status().isTerminal() && report.closedByName() != null









 && !report.closedByName().isBlank()) {









 staff.merge(report.closedByName(), 1, Integer::sum);







 }





 }



 }




Map<String, Integer> sortedCategories = sortByCount(categories);



 Map<String, Integer> sortedTargets = sortByCount(targets);



 Map<String, Integer> sortedStaff = sortByCount(staff);



 long average = handledCount == 0 ? 0L : handledTotal / handledCount;



 int total = open + claimed + resolved + rejected + duplicate + autoClosed;



 return new ReportStats(total, open, claimed, resolved, rejected, duplicate, autoClosed,





 average, sortedCategories, sortedTargets, sortedStaff);

 }


public int closed() {



 return resolved + rejected + duplicate + autoClosed;

 }


public List<Map.Entry<String, Integer>> topTargets(int limit) {



 return byTarget.entrySet().stream().limit(Math.max(0, limit)).toList();

 }


public List<Map.Entry<String, Integer>> topStaff(int limit) {



 return byStaff.entrySet().stream().limit(Math.max(0, limit)).toList();

 }


public static ReportStats empty() {



 return new ReportStats(0, 0, 0, 0, 0, 0, 0, 0L, Map.of(), Map.of());

 }


private static Map<String, Integer> sortByCount(Map<String, Integer> source) {



 return source.entrySet().stream()





 .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()







 .thenComparing(Map.Entry.comparingByKey()))





 .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);

 } }
