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
import java.time.Clock; import java.time.Instant; import java.time.LocalDate; import java.time.ZoneId; import java.util.Collection; import java.util.HashMap; import java.util.Locale; import java.util.Map; import java.util.UUID;
/**
* In-memory rate limiter with a {@link Clock} so behaviour is deterministic in
* tests. It never touches storage.
*/ public final class RateLimiter {


public enum Reason {



 ALLOWED,



 COOLDOWN,



 DAILY_LIMIT,



 SAME_TARGET,



 DUPLICATE,



 MAX_OPEN

 }


public record Decision(Reason reason, long seconds, int limit, long duplicateId) {




public static Decision allowed() {





 return new Decision(Reason.ALLOWED, 0L, 0, 0L);



 }




public boolean isAllowed() {





 return reason == Reason.ALLOWED;



 }

 }


private final RateLimitPolicy policy;

 private final Clock clock;

 private final ZoneId zone;

 private final Map<UUID, Long> lastSubmit = new HashMap<>();

 private final Map<UUID, Map<String, Long>> lastTarget = new HashMap<>();

 private final Map<UUID, Map<LocalDate, Integer>> dailyCounts = new HashMap<>();


public RateLimiter(RateLimitPolicy policy, Clock clock, ZoneId zone) {



 this.policy = policy == null ? new RateLimitPolicy(0, 0, 0, 0, 0) : policy;



 this.clock = clock == null ? Clock.systemDefaultZone() : clock;



 this.zone = zone == null ? this.clock.getZone() : zone;

 }


public RateLimitPolicy policy() {



 return policy;

 }


public synchronized Decision check(ReportRepository repository, UUID reporterId, String targetName) {



 long now = clock.millis();



 if (policy.cooldownSeconds() > 0) {





 Long last = lastSubmit.get(reporterId);





 if (last != null) {







 long elapsed = now - last;







 long required = policy.cooldownSeconds() * 1000L;







 if (elapsed >= 0 && elapsed < required) {









 return new Decision(Reason.COOLDOWN, remainingSeconds(required - elapsed), 0, 0L);







 }





 }



 }




if (policy.dailyLimit() > 0) {





 int today = dailyCount(reporterId, dateOf(now));





 if (today >= policy.dailyLimit()) {







 return new Decision(Reason.DAILY_LIMIT, 0L, policy.dailyLimit(), 0L);





 }



 }




if (policy.sameTargetIntervalSeconds() > 0 && targetName != null) {





 Map<String, Long> targets = lastTarget.get(reporterId);





 Long last = targets == null ? null : targets.get(normalize(targetName));





 if (last != null) {







 long elapsed = now - last;







 long required = policy.sameTargetIntervalSeconds() * 1000L;







 if (elapsed >= 0 && elapsed < required) {









 return new Decision(Reason.SAME_TARGET, remainingSeconds(required - elapsed), 0, 0L);







 }





 }



 }




if (policy.duplicateWindowSeconds() > 0 && targetName != null && repository != null) {





 long since = now - policy.duplicateWindowSeconds() * 1000L;





 var duplicate = repository.findOpenDuplicate(reporterId, targetName, since);





 if (duplicate.isPresent()) {







 return new Decision(Reason.DUPLICATE, 0L, 0, duplicate.get().id());





 }



 }




if (policy.maxOpenPerTarget() > 0 && targetName != null && repository != null) {





 int open = repository.countOpenForTarget(targetName);





 if (open >= policy.maxOpenPerTarget()) {







 return new Decision(Reason.MAX_OPEN, 0L, policy.maxOpenPerTarget(), 0L);





 }



 }




return Decision.allowed();

 }


public synchronized void record(UUID reporterId, String targetName) {



 long now = clock.millis();



 lastSubmit.put(reporterId, now);



 if (targetName != null) {





 lastTarget.computeIfAbsent(reporterId, ignored -> new HashMap<>())







 .put(normalize(targetName), now);



 }



 LocalDate date = dateOf(now);



 dailyCounts.computeIfAbsent(reporterId, ignored -> new HashMap<>())





 .merge(date, 1, Integer::sum);



 prune(date);

 }


/**


* Rebuilds cooldown/daily state from reports loaded at startup.


*/

 public synchronized void seed(Collection<Report> reports) {



 if (reports == null) {





 return;



 }



 for (Report report : reports) {





 if (report == null || report.reporterId() == null || report.createdAt() <= 0) {







 continue;





 }





 UUID reporter = report.reporterId();





 lastSubmit.merge(reporter, report.createdAt(), Math::max);





 if (report.targetName() != null && !report.targetName().isBlank()) {







 lastTarget.computeIfAbsent(reporter, ignored -> new HashMap<>())









 .merge(normalize(report.targetName()), report.createdAt(), Math::max);





 }





 LocalDate date = dateOf(report.createdAt());





 dailyCounts.computeIfAbsent(reporter, ignored -> new HashMap<>())







 .merge(date, 1, Integer::sum);



 }



 prune(dateOf(clock.millis()));

 }


public synchronized void reset() {



 lastSubmit.clear();



 lastTarget.clear();



 dailyCounts.clear();

 }


private int dailyCount(UUID reporterId, LocalDate date) {



 Map<LocalDate, Integer> counts = dailyCounts.get(reporterId);



 if (counts == null) {





 return 0;



 }



 return counts.getOrDefault(date, 0);

 }


private void prune(LocalDate today) {



 if (dailyCounts.size() <= 64) {





 return;



 }



 dailyCounts.entrySet().removeIf(entry -> entry.getValue().keySet().stream()





 .allMatch(date -> date.isBefore(today.minusDays(2))));



 lastTarget.entrySet().removeIf(entry -> entry.getValue().isEmpty());

 }


private LocalDate dateOf(long epochMillis) {



 return Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate();

 }


private static long remainingSeconds(long millis) {



 if (millis <= 0) {





 return 0L;



 }



 return Math.max(1L, (millis + 999L) / 1000L);

 }


private static String normalize(String value) {



 return value.trim().toLowerCase(Locale.ROOT);

 } }
