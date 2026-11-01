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
import java.util.ArrayList; import java.util.Collection; import java.util.Comparator; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Set;
/**
* Aggregated view of one player for the admin player directory. Pure data so
* the sorting and counting rules can be unit tested without a server.
*
* <p>Names are matched case-insensitively because reports store the name that
* was typed at submission time, which may differ in case from the current
* account name.</p>
*/ public record PlayerSummary(

 String name,

 int reportsAsTarget,

 int reportsAsReporter,

 int openAsTarget,

 int resolvedAsTarget,

 long lastActivityAt,

 boolean online ) {


private static final class Mutable {



 private String name;



 private int asTarget;



 private int asReporter;



 private int open;



 private int resolved;



 private long lastActivity;



 private boolean online;

 }


/**


* Builds one summary per known name from report history plus the currently


* online players, so staff can look up a player who has no reports yet.


*/

 public static List<PlayerSummary> build(Collection<Report> reports,





















 Collection<String> onlineNames) {



 Map<String, Mutable> index = new LinkedHashMap<>();



 if (reports != null) {





 for (Report report : reports) {







 if (report == null) {









 continue;







 }







 accumulate(index, report.targetName(), report, true);







 accumulate(index, report.reporterName(), report, false);





 }



 }



 Set<String> online = normaliseOnline(onlineNames);



 for (String name : online) {





 Mutable entry = index.computeIfAbsent(key(name), ignored -> new Mutable());





 if (entry.name == null) {







 entry.name = name;





 }





 entry.online = true;



 }



 List<PlayerSummary> result = new ArrayList<>(index.size());



 for (Mutable entry : index.values()) {





 if (entry.name == null || entry.name.isBlank()) {







 continue;





 }





 result.add(new PlayerSummary(entry.name, entry.asTarget, entry.asReporter,







 entry.open, entry.resolved, entry.lastActivity, entry.online));



 }



 result.sort(Comparator





 .comparingInt(PlayerSummary::openAsTarget).reversed()





 .thenComparing(Comparator.comparingInt(PlayerSummary::reportsAsTarget).reversed())





 .thenComparing(PlayerSummary::name, String.CASE_INSENSITIVE_ORDER));



 return List.copyOf(result);

 }


public static List<PlayerSummary> build(Collection<Report> reports,





















 Collection<String> onlineNames, String query) {



 String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);



 List<PlayerSummary> all = build(reports, onlineNames);



 if (needle.isEmpty()) {





 return all;



 }



 List<PlayerSummary> matches = new ArrayList<>();



 for (PlayerSummary summary : all) {





 if (summary.name().toLowerCase(Locale.ROOT).contains(needle)) {







 matches.add(summary);





 }



 }



 return List.copyOf(matches);

 }


public int totalReports() {



 return reportsAsTarget + reportsAsReporter;

 }


private static void accumulate(Map<String, Mutable> index, String rawName, Report report,

















boolean asTarget) {



 if (rawName == null || rawName.isBlank()) {





 return;



 }



 Mutable entry = index.computeIfAbsent(key(rawName), ignored -> new Mutable());



 if (entry.name == null) {





 entry.name = rawName.trim();



 }



 entry.lastActivity = Math.max(entry.lastActivity, report.createdAt());



 if (asTarget) {





 entry.asTarget++;





 if (report.isOpen()) {







 entry.open++;





 }





 if (report.status() == ReportStatus.RESOLVED) {







 entry.resolved++;





 }



 } else {





 entry.asReporter++;



 }

 }


private static Set<String> normaliseOnline(Collection<String> onlineNames) {



 if (onlineNames == null || onlineNames.isEmpty()) {





 return Set.of();



 }



 Set<String> names = new java.util.LinkedHashSet<>();



 for (String name : onlineNames) {





 if (name != null && !name.isBlank()) {







 names.add(name.trim());





 }



 }



 return names;

 }


private static String key(String name) {



 return name.trim().toLowerCase(Locale.ROOT);

 } }
