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
import com.google.gson.Gson; import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.Report; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.i18n.Messages; import com.reportdesk.webhook.WebhookPayload;
import java.net.URI; import java.net.http.HttpClient; import java.net.http.HttpRequest; import java.net.http.HttpResponse; import java.nio.charset.StandardCharsets; import java.time.Duration; import java.util.ArrayList; import java.util.List; import java.util.Map; import java.util.function.Consumer;
/**
* Asynchronous Discord webhook integration. Disabled by default; no thread is
* blocked and failures are only logged.
*/ public final class WebhookService {


private final ReportDeskPlugin plugin;

 private final Consumer<String> warning;

 private final Gson gson = new Gson();

 private final HttpClient client;


public WebhookService(ReportDeskPlugin plugin) {



 this.plugin = plugin;



 this.warning = message -> plugin.getLogger().warning("[webhook] " + message);



 this.client = HttpClient.newBuilder()





 .connectTimeout(Duration.ofSeconds(10))





 .followRedirects(HttpClient.Redirect.NORMAL)





 .build();

 }


public void created(Report report) {



 send("created", report, "");

 }


public void claimed(Report report, String staffName) {



 send("claimed", report, staffName);

 }


public void closed(Report report, String staffName) {



 send("closed", report, staffName);

 }


private void send(String event, Report report, String staffName) {



 PluginConfig.Webhook settings = plugin.config().notifications().webhook();



 if (!settings.enabled() || report == null) {





 return;



 }



 boolean enabledForEvent = switch (event) {





 case "created" -> settings.eventCreated();





 case "claimed" -> settings.eventClaimed();





 case "closed" -> settings.eventClosed();





 default -> false;



 };



 if (!enabledForEvent) {





 return;



 }



 if (settings.url() == null || settings.url().isBlank()) {





 return;



 }




Messages messages = plugin.messages();



 String titleKey = switch (event) {





 case "created" -> MessageKeys.WEBHOOK_TITLE_CREATED;





 case "claimed" -> MessageKeys.WEBHOOK_TITLE_CLAIMED;





 default -> MessageKeys.WEBHOOK_TITLE_CLOSED;



 };



 String contentKey = switch (event) {





 case "created" -> MessageKeys.WEBHOOK_CONTENT_CREATED;





 case "claimed" -> MessageKeys.WEBHOOK_CONTENT_CLAIMED;





 default -> MessageKeys.WEBHOOK_CONTENT_CLOSED;



 };



 Map<String, String> placeholders = Map.of(





 "id", String.valueOf(report.id()),





 "target", report.targetName(),





 "reporter", report.reporterName(),





 "category", plugin.categoryDisplay(report.category()),





 "status", messages.plain(statusKey(report.status())),





 "staff", staffName == null ? "" : staffName



 );




List<WebhookPayload.Field> fields = new ArrayList<>();



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_ID), String.valueOf(report.id()), true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_TARGET), report.targetName(), true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_REPORTER), report.reporterName(), true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_CATEGORY),





 plugin.categoryDisplay(report.category()), true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_STATUS),





 messages.plain(statusKey(report.status())), true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_STAFF),





 staffName == null ? "" : staffName, true));



 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_SERVER), report.server(), true));



 if (!report.detail().isBlank()) {





 fields.add(new WebhookPayload.Field(messages.plain(MessageKeys.WEBHOOK_FIELD_DETAIL), report.detail(), false));



 }




WebhookPayload payload = new WebhookPayload(





 settings.username(),





 settings.avatarUrl(),





 messages.plain(contentKey, placeholders),





 messages.plain(titleKey, placeholders),





 3447003,





 fields



 );



 String body = gson.toJson(payload.toMap());



 try {





 HttpRequest request = HttpRequest.newBuilder(URI.create(settings.url().trim()))







 .timeout(Duration.ofSeconds(settings.timeoutSeconds()))







 .header("Content-Type", "application/json; charset=utf-8")







 .header("User-Agent", "ReportDesk/1.0")







 .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))







 .build();





 client.sendAsync(request, HttpResponse.BodyHandlers.discarding())







 .orTimeout(settings.timeoutSeconds() + 2L, java.util.concurrent.TimeUnit.SECONDS)







 .whenComplete((response, throwable) -> {









 if (throwable != null) {











 warning.accept("send failed: " + throwable.getMessage());









 } else if (response.statusCode() >= 400) {











 warning.accept("Discord returned HTTP " + response.statusCode());









 }







 });



 } catch (RuntimeException exception) {





 warning.accept("invalid request: " + exception.getMessage());



 }

 }


private String statusKey(com.reportdesk.core.ReportStatus status) {



 return switch (status) {





 case OPEN -> MessageKeys.STATUS_OPEN;





 case CLAIMED -> MessageKeys.STATUS_CLAIMED;





 case RESOLVED -> MessageKeys.STATUS_RESOLVED;





 case REJECTED -> MessageKeys.STATUS_REJECTED;





 case DUPLICATE -> MessageKeys.STATUS_DUPLICATE;





 case AUTO_CLOSED -> MessageKeys.STATUS_AUTO_CLOSED;



 };

 } }
