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

package com.reportdesk.service;
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.core.Report; import com.reportdesk.core.ReportCategory; import com.reportdesk.i18n.MessageKeys; import org.bukkit.Bukkit; import org.bukkit.entity.Player;
import java.util.LinkedHashMap; import java.util.Map; import java.util.Optional; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
/**
* Optional auto-detection. Disabled by default and never punishes: it either
* creates a system report or alerts staff.
*/ public final class AutoDetectService {


private final ReportDeskPlugin plugin;

 private final EvidenceService evidence;

 private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();


public AutoDetectService(ReportDeskPlugin plugin, EvidenceService evidence) {



 this.plugin = plugin;



 this.evidence = evidence;

 }


public void check() {



 if (!plugin.config().evidence().autoDetect().enabled()) {





 return;



 }



 var settings = plugin.config().evidence().autoDetect();



 if (!plugin.config().evidence().enabled() || !plugin.config().evidence().blockBreak().enabled()) {





 return;



 }



 long now = System.currentTimeMillis();



 for (EvidenceService.BreakStats stats : evidence.recentBreakStats(





 settings.windowSeconds(), settings.blockBreakThreshold())) {





 long cooldown = settings.cooldownSeconds() * 1000L;





 Long until = cooldownUntil.get(stats.playerId());





 if (until != null && until > now) {







 continue;





 }





 cooldownUntil.put(stats.playerId(), now + cooldown);





 Bukkit.getScheduler().runTask(plugin, () -> handle(stats, settings.windowSeconds()));



 }

 }


private void handle(EvidenceService.BreakStats stats, int windowSeconds) {



 Player target = Bukkit.getPlayer(stats.playerId());



 String targetName = target != null ? target.getName() : stats.playerName();



 if (targetName == null || targetName.isBlank()) {





 return;



 }



 var settings = plugin.config().evidence().autoDetect();



 ReportCategory category = plugin.config().categoryRegistry().find("cheating")





 .orElseGet(() -> plugin.config().categoryRegistry()







 .find(plugin.config().defaultCategory()).orElseThrow());



 String detail = plugin.messages().plain(MessageKeys.EVIDENCE_AUTO_REPORT_DETAIL, Map.of(





 "count", String.valueOf(stats.count()),





 "seconds", String.valueOf(windowSeconds)



 ));




if (settings.createReport()) {





 Optional<Report> created = plugin.reportService().submitSystem(targetName, category.id(), detail);





 created.ifPresent(report -> plugin.notification().onAutoDetected(report, stats.count(), windowSeconds));



 } else if (settings.notifyStaff()) {





 Map<String, String> placeholders = new LinkedHashMap<>();





 placeholders.put("target", targetName);





 placeholders.put("count", String.valueOf(stats.count()));





 placeholders.put("seconds", String.valueOf(windowSeconds));





 placeholders.put("id", "-");





 plugin.notification().staffAlert(plugin.messages()







 .prefixed(MessageKeys.EVIDENCE_AUTO_REPORT_STAFF, placeholders));



 }

 }


public void clearCooldowns() {



 cooldownUntil.clear();

 } }
