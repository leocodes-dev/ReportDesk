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

package com.reportdesk.service;

import com.google.gson.Gson;
import com.reportdesk.ReportDeskPlugin;
import com.reportdesk.config.PluginConfig;
import com.reportdesk.core.Report;
import com.reportdesk.i18n.MessageKeys;
import com.reportdesk.i18n.Messages;
import com.reportdesk.webhook.WebhookPayload;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Asynchronous Discord webhook integration. Disabled by default; no thread is blocked.
 *
 * <p>Failures are retried with exponential backoff (bounded by
 * {@code notifications.webhook.retry-count}) and, once exhausted, alert online staff in game
 * (bounded, in-memory only: at most {@code retry-count} pending retries per in-flight event).
 */
public final class WebhookService {

  private final ReportDeskPlugin plugin;

  private final Supplier<PluginConfig> config;

  private final Supplier<Messages> messages;

  private final Function<String, String> categoryDisplay;

  private final Consumer<String> warning;

  private final Consumer<String> failureAlert;

  private final Gson gson = new Gson();

  private final HttpClient client;

  public WebhookService(ReportDeskPlugin plugin) {

    this(
        plugin,
        plugin::config,
        plugin::messages,
        plugin::categoryDisplay,
        message -> plugin.getLogger().warning("[webhook] " + message),
        defaultFailureAlert(plugin),
        defaultClient());
  }

  WebhookService(
      ReportDeskPlugin plugin,
      Supplier<PluginConfig> config,
      Supplier<Messages> messages,
      Function<String, String> categoryDisplay,
      Consumer<String> warning,
      Consumer<String> failureAlert,
      HttpClient client) {

    this.plugin = plugin;

    this.config = config;

    this.messages = messages;

    this.categoryDisplay = categoryDisplay;

    this.warning = warning;

    this.failureAlert = failureAlert;

    this.client = client;
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

  /**
   * Pushes an SLA breach summary to the webhook channel. When a secondary URL is configured it is
   * routed there (traffic split: SLA/alerts to a second group); otherwise it goes to the primary
   * channel. Gated by the live {@code notifications.webhook.sla-events-enabled} switch so the
   * {@code /reports sla} command and the optional weekly task share one toggle. No-op when the
   * report has nothing overdue or the webhook is disabled.
   */
  public void slaBreach(com.reportdesk.core.SlaStats.SlaReport report) {

    PluginConfig.Webhook settings = config.get().notifications().webhook();

    if (!settings.enabled() || report == null || report.isEmpty() || !settings.slaEventsEnabled()) {

      return;
    }

    String targetUrl =
        settings.secondaryUrl() != null && !settings.secondaryUrl().isBlank()
            ? settings.secondaryUrl().trim()
            : settings.url().trim();

    if (targetUrl == null || targetUrl.isBlank()) {

      return;
    }

    Messages text = messages.get();

    String breakdown = report.breakdownSummary();

    WebhookPayload payload =
        new WebhookPayload(
            settings.username(),
            settings.avatarUrl(),
            text.plain(
                MessageKeys.WEBHOOK_CONTENT_SLA_BREACH,
                Map.of(
                    "overdue", String.valueOf(report.totalOverdue()),
                    "avg",
                        com.reportdesk.util.TimeText.duration(
                            report.averageOverdueMillis(), text),
                    "breakdown", breakdown.isBlank() ? "-" : breakdown)),
            text.plain(MessageKeys.WEBHOOK_TITLE_SLA_BREACH, Map.of()),
            15158720,
            List.of());

    post(gson.toJson(payload.toMap()), 0, "sla", targetUrl, null);
  }

  /**
   * Sends a small test payload to the configured channel so an administrator can verify the
   * webhook works. Runs the same retry chain as event sends; completes with {@code true} when
   * the channel finally accepts a message. No staff alert is raised for a failed test — the
   * clicker sees the result in chat.
   */
  public CompletableFuture<Boolean> sendTest() {

    PluginConfig.Webhook settings = config.get().notifications().webhook();

    if (!settings.enabled() || settings.url() == null || settings.url().isBlank()) {

      return CompletableFuture.completedFuture(false);
    }

    Messages text = messages.get();

    WebhookPayload payload =
        new WebhookPayload(
            settings.username(),
            settings.avatarUrl(),
            text.plain(MessageKeys.WEBHOOK_TEST_CONTENT),
            text.plain(MessageKeys.WEBHOOK_TEST_TITLE),
            65280,
            List.of(
                new WebhookPayload.Field(
                    text.plain(MessageKeys.WEBHOOK_FIELD_SERVER),
                    config.get().serverName(),
                    true)));

    CompletableFuture<Boolean> outcome = new CompletableFuture<>();

    post(gson.toJson(payload.toMap()), 0, "test", settings.url().trim(), outcome::complete);

    return outcome;
  }

  private void send(String event, Report report, String staffName) {

    PluginConfig.Webhook settings = config.get().notifications().webhook();

    if (!settings.enabled() || report == null) {

      return;
    }

    boolean enabledForEvent =
        switch (event) {
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

    Messages text = messages.get();

    String titleKey =
        switch (event) {
          case "created" -> MessageKeys.WEBHOOK_TITLE_CREATED;

          case "claimed" -> MessageKeys.WEBHOOK_TITLE_CLAIMED;

          default -> MessageKeys.WEBHOOK_TITLE_CLOSED;
        };

    String contentKey =
        switch (event) {
          case "created" -> MessageKeys.WEBHOOK_CONTENT_CREATED;

          case "claimed" -> MessageKeys.WEBHOOK_CONTENT_CLAIMED;

          default -> MessageKeys.WEBHOOK_CONTENT_CLOSED;
        };

    Map<String, String> placeholders =
        Map.of(
            "id", String.valueOf(report.id()),
            "target", report.targetName(),
            "reporter", report.reporterName(),
            "category", categoryDisplay.apply(report.category()),
            "status", text.plain(statusKey(report.status())),
            "staff", staffName == null ? "" : staffName);

    List<WebhookPayload.Field> fields = new ArrayList<>();

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_ID), String.valueOf(report.id()), true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_TARGET), report.targetName(), true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_REPORTER), report.reporterName(), true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_CATEGORY),
            categoryDisplay.apply(report.category()),
            true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_STATUS),
            text.plain(statusKey(report.status())),
            true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_STAFF), staffName == null ? "" : staffName, true));

    fields.add(
        new WebhookPayload.Field(
            text.plain(MessageKeys.WEBHOOK_FIELD_SERVER), report.server(), true));

    if (!report.detail().isBlank()) {

      fields.add(
          new WebhookPayload.Field(
              text.plain(MessageKeys.WEBHOOK_FIELD_DETAIL), report.detail(), false));
    }

    WebhookPayload payload =
        new WebhookPayload(
            settings.username(),
            settings.avatarUrl(),
            text.plain(contentKey, placeholders),
            text.plain(titleKey, placeholders),
            3447003,
            fields);

    post(gson.toJson(payload.toMap()), 0, event, settings.url().trim(), null);
  }

  /**
   * One attempt of the bounded retry chain against {@code targetUrl}. {@code finalOutcome} is
   * completed exactly once: {@code true} when a channel accepts the payload, {@code false} when the
   * retries exhaust.
   */
  private void post(
      String body, int attempt, String label, String targetUrl, Consumer<Boolean> finalOutcome) {

    PluginConfig.Webhook settings = config.get().notifications().webhook();

    try {

      HttpRequest request =
          HttpRequest.newBuilder(URI.create(targetUrl))
              .timeout(Duration.ofSeconds(settings.timeoutSeconds()))
              .header("Content-Type", "application/json; charset=utf-8")
              .header("User-Agent", "ReportDesk/1.0")
              .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
              .build();

      client
          .sendAsync(request, HttpResponse.BodyHandlers.discarding())
          .orTimeout(settings.timeoutSeconds() + 2L, TimeUnit.SECONDS)
          .whenComplete(
              (response, throwable) -> {
                if (throwable == null && response != null && response.statusCode() < 400) {

                  if (finalOutcome != null) {

                    finalOutcome.accept(true);
                  }

                  return;
                }

                warning.accept(
                    label
                        + " attempt "
                        + (attempt + 1)
                        + " failed: "
                        + (throwable != null
                            ? String.valueOf(throwable.getMessage())
                            : "HTTP " + response.statusCode()));

                scheduleNext(body, attempt + 1, label, targetUrl, settings, finalOutcome);
              });

    } catch (RuntimeException exception) {

      warning.accept(label + " attempt " + (attempt + 1) + " invalid request: " + exception);

      scheduleNext(body, attempt + 1, label, targetUrl, settings, finalOutcome);
    }
  }

  /** Bounded by {@code retry-count}; each pending retry is a single scheduled task. */
  private void scheduleNext(
      String body, int nextAttempt, String label, String targetUrl,
      PluginConfig.Webhook settings,
      Consumer<Boolean> finalOutcome) {

    if (nextAttempt > settings.retryCount()) {

      warning.accept(label + " giving up after " + (settings.retryCount() + 1) + " attempts");

      if ("test".equals(label)) {

        if (finalOutcome != null) {

          finalOutcome.accept(false);
        }

      } else {

        failureAlert.accept(label);
      }

      return;
    }

    CompletableFuture.delayedExecutor(delayMillis(settings, nextAttempt), TimeUnit.MILLISECONDS)
        .execute(() -> post(body, nextAttempt, label, targetUrl, finalOutcome));
  }

  /** Exponential backoff: base × 2^(attempt-1), capped so the shift cannot overflow. */
  static long delayMillis(PluginConfig.Webhook settings, int attempt) {

    int base = Math.max(1, Math.min(settings.retryBaseSeconds(), 300));

    return base * (1L << Math.min(attempt - 1, 10)) * 1000L;
  }

  private static Consumer<String> defaultFailureAlert(ReportDeskPlugin plugin) {

    return label ->
        Bukkit.getScheduler()
            .runTask(
                plugin,
                () -> {
                  if (!plugin.config().notifications().webhook().failureAlertEnabled()) {

                    return;
                  }

                  String permission = plugin.config().notifications().staffPermission();

                  net.kyori.adventure.text.Component text =
                      plugin
                          .messages()
                          .prefixed(MessageKeys.WEBHOOK_FAILURE_ALERT, Map.of("event", label));

                  for (Player online : Bukkit.getOnlinePlayers()) {

                    if (online.hasPermission(permission)) {

                      online.sendMessage(text);
                    }
                  }
                });
  }

  private static HttpClient defaultClient() {

    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
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
  }
}
