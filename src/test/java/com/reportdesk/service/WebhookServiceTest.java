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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.config.ConfigLoader;
import com.reportdesk.config.MapConfigSource;
import com.reportdesk.config.PluginConfig;
import com.reportdesk.i18n.Messages;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class WebhookServiceTest {

  @Test
  void retryChainRecoversAfterFailure() throws Exception {

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

    AtomicInteger hits = new AtomicInteger();

    server.createContext(
        "/",
        exchange -> {
          int status = hits.incrementAndGet() < 2 ? 500 : 200;

          exchange.sendResponseHeaders(status, -1);

          exchange.close();
        });

    server.start();

    try {

      WebhookService service =
          serviceAt(server, 2, new ArrayList<>(), new ArrayList<>());

      CompletableFuture<Boolean> result = service.sendTest();

      assertTrue(
          result.get(15, TimeUnit.SECONDS), "test payload should succeed after one 500");

      assertEquals(2, hits.get(), "exactly one retry");

    } finally {

      server.stop(0);
    }
  }

  @Test
  void exhaustedTestRetriesDoNotAlertStaff() throws Exception {

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

    AtomicInteger hits = new AtomicInteger();

    server.createContext(
        "/",
        exchange -> {
          hits.incrementAndGet();

          exchange.sendResponseHeaders(500, -1);

          exchange.close();
        });

    server.start();

    try {

      List<String> alerts = new ArrayList<>();

      WebhookService service = serviceAt(server, 1, new ArrayList<>(), alerts);

      CompletableFuture<Boolean> result = service.sendTest();

      assertEquals(false, result.get(15, TimeUnit.SECONDS));

      assertEquals(2, hits.get(), "initial attempt plus one retry");

      assertTrue(alerts.isEmpty(), "a failed test reports in chat, never as a staff alert");
    } finally {

      server.stop(0);
    }
  }

  @Test
  void disabledWebhookTestCompletesFalseWithoutNetwork() throws Exception {

    List<String> warnings = new ArrayList<>();

    List<String> alerts = new ArrayList<>();

    PluginConfig config = configWithWebhookEnabled(false, "http://127.0.0.1:1", 2, 1);

    WebhookService service =
        new WebhookService(
            null,
            () -> config,
            messages(),
            Function.identity(),
            warnings::add,
            alerts::add,
            testClient());

    CompletableFuture<Boolean> result = service.sendTest();

    assertEquals(false, result.get(1, TimeUnit.SECONDS));

    assertTrue(warnings.isEmpty(), "disabled webhook must not touch the network stack");
  }

  @Test
  void retryDelayDoublesExponentially() {

    PluginConfig config = configWithWebhookEnabled(true, "http://127.0.0.1:1", 3, 1);

    PluginConfig.Webhook webhook = config.notifications().webhook();

    assertEquals(1000L, WebhookService.delayMillis(webhook, 1));

    assertEquals(2000L, WebhookService.delayMillis(webhook, 2));

    assertEquals(4000L, WebhookService.delayMillis(webhook, 3));
  }

  private static WebhookService serviceAt(
      HttpServer server, int retryCount, List<String> warnings, List<String> alerts) {

    PluginConfig config =
        configWithWebhookEnabled(
            true, "http://127.0.0.1:" + server.getAddress().getPort(), retryCount, 1);

    return new WebhookService(
        null, () -> config, messages(), Function.identity(), warnings::add, alerts::add, testClient());
  }

  private static PluginConfig configWithWebhookEnabled(
      boolean enabled, String url, int retryCount, int baseSeconds) {

    Map<String, Object> webhook = new LinkedHashMap<>();

    webhook.put("enabled", enabled);

    webhook.put("url", url);

    webhook.put("timeout-seconds", 5);

    webhook.put("retry-count", retryCount);

    webhook.put("retry-base-seconds", baseSeconds);

    Map<String, Object> notifications = Map.of("webhook", webhook);

    Map<String, Object> source = Map.of("notifications", notifications);

    List<String> warnings = new ArrayList<>();

    return new ConfigLoader(warnings::add).load(new MapConfigSource(source));
  }

  private static Supplier<Messages> messages() {

    return () -> {
      try (InputStream input =
          WebhookServiceTest.class.getResourceAsStream("/lang/en_US.yml")) {

        return Messages.fromStream(input, "en_US");
      } catch (IOException exception) {

        throw new UncheckedIOException(exception);
      }
    };
  }

  private static HttpClient testClient() {

    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
  }
}
