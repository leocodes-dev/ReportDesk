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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.test.TestFixtures;
import com.reportdesk.core.ReportSubmissionService.SubmitResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportSubmissionServiceTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

  private final InputValidator validator =
      new InputValidator(
          new InputValidator.Settings(
              2, 20, 40, false, false, List.of("badword"), "^[A-Za-z0-9_]{3,16}$"));

  private final CategoryRegistry categories =
      CategoryRegistry.of(
          List.of(
              new ReportCategory(
                  "cheating",
                  "Cheating",
                  "DIAMOND_SWORD",
                  List.of("cheat", "hack"),
                  true,
                  100,
                  List.of()),
              new ReportCategory("chat", "Chat", "PAPER", List.of(), true, 90, List.of()),
              new ReportCategory(
                  "disabled", "Disabled", "BARRIER", List.of("off"), false, 10, List.of())));

  private ReportSubmissionService service(
      ReportRepository repository,
      RateLimiter limiter,
      boolean allowOffline,
      boolean requireOnline,
      int minAccountAgeMinutes) {

    return new ReportSubmissionService(
        repository,
        limiter,
        validator,
        categories,
        new ReportSubmissionService.Settings(
            "test-server", true, allowOffline, requireOnline, minAccountAgeMinutes),
        Clock.fixed(START, ZoneOffset.UTC));
  }

  private ReportSubmissionService service(ReportRepository repository, RateLimiter limiter) {

    return service(repository, limiter, true, false, 0);
  }

  private static ReportRepository readyRepository() {

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    return repository;
  }

  private static RateLimiter unlimited() {

    return new RateLimiter(
        new RateLimitPolicy(0, 0, 0, 0, 0), Clock.fixed(START, ZoneOffset.UTC), ZoneOffset.UTC);
  }

  private static ReportSubmissionService.SubmitRequest request(
      String target, String category, String detail) {

    return new ReportSubmissionService.SubmitRequest(
        TestFixtures.ALICE, "Alice", null, target, true, 0L, category, detail, false, false);
  }

  private static ReportSubmissionService.SubmitRequest request(
      String target, String category, String detail, List<ReportChatSelection> selections) {

    return new ReportSubmissionService.SubmitRequest(
        TestFixtures.ALICE,
        "Alice",
        null,
        target,
        true,
        0L,
        category,
        detail,
        false,
        false,
        selections);
  }

  @Test
  void successfulSubmissionCreatesOpenReport() {

    ReportRepository repository = readyRepository();

    ReportSubmissionService service = service(repository, unlimited());

    var result = service.submit(request("Bob", "hack", "using kill aura"));

    assertTrue(result.success());

    assertEquals(ReportSubmissionService.SubmitReason.SUCCESS, result.reason());

    assertEquals("1", result.placeholders().get("id"));

    assertEquals(1, repository.size());

    Report report = result.report();

    assertEquals(1L, report.id());

    assertEquals(TestFixtures.ALICE, report.reporterId());

    assertEquals("Alice", report.reporterName());

    assertEquals("Bob", report.targetName());

    assertEquals("cheating", report.category());

    assertEquals("using kill aura", report.detail());

    assertEquals(ReportStatus.OPEN, report.status());

    assertEquals("test-server", report.server());

    assertTrue(report.isOpen());

    assertFalse(report.systemGenerated());
  }

  @Test
  void invalidTargetNamesAreRejected() {

    ReportRepository repository = readyRepository();

    ReportSubmissionService service = service(repository, unlimited());

    assertEquals(
        ReportSubmissionService.SubmitReason.INVALID_TARGET,
        service.submit(request("", "chat", "hello")).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.INVALID_TARGET,
        service.submit(request("x", "chat", "hello")).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.INVALID_TARGET,
        service.submit(request("bad name", "chat", "hello")).reason());

    assertEquals(0, repository.size());
  }

  @Test
  void unknownAndDisabledCategoriesAreRejected() {

    ReportSubmissionService service = service(readyRepository(), unlimited());

    assertEquals(
        ReportSubmissionService.SubmitReason.CATEGORY_UNKNOWN,
        service.submit(request("Bob", "nope", "hello")).reason());

    SubmitResult disabled =
        service.submit(request("Bob", "off", "hello"));

    assertEquals(
        ReportSubmissionService.SubmitReason.CATEGORY_DISABLED, disabled.reason());

    // The pure core reports the config display value; the Bukkit layer
    // rewrites the "category" placeholder through categoryDisplay().

    assertEquals("Disabled", disabled.placeholders().get("category"));
  }

  @Test
  void selfReportsAreBlockedByNameAndId() {

    ReportRepository repository = readyRepository();

    ReportSubmissionService service = service(repository, unlimited());

    var byName =
        new ReportSubmissionService.SubmitRequest(
            TestFixtures.ALICE, "Alice", null, "alice", true, 0L, "chat", "hello", false, false);

    assertEquals(ReportSubmissionService.SubmitReason.SELF_REPORT, service.submit(byName).reason());

    var byId =
        new ReportSubmissionService.SubmitRequest(
            TestFixtures.ALICE,
            "Alice",
            TestFixtures.ALICE,
            "Bob",
            true,
            0L,
            "chat",
            "hello",
            false,
            false);

    assertEquals(ReportSubmissionService.SubmitReason.SELF_REPORT, service.submit(byId).reason());
  }

  @Test
  void offlineTargetSettingsAreEnforced() {

    ReportSubmissionService requireOnline = service(readyRepository(), unlimited(), true, true, 0);

    var offline =
        new ReportSubmissionService.SubmitRequest(
            TestFixtures.ALICE, "Alice", null, "Bob", false, 0L, "chat", "hello", false, false);

    assertEquals(
        ReportSubmissionService.SubmitReason.REQUIRES_ONLINE,
        requireOnline.submit(offline).reason());

    ReportSubmissionService noOffline = service(readyRepository(), unlimited(), false, false, 0);

    assertEquals(
        ReportSubmissionService.SubmitReason.OFFLINE_DISABLED, noOffline.submit(offline).reason());
  }

  @Test
  void accountAgeRequirementUsesReporterFirstPlayed() {

    ReportSubmissionService service = service(readyRepository(), unlimited(), true, false, 60);

    var tooYoung =
        new ReportSubmissionService.SubmitRequest(
            TestFixtures.ALICE,
            "Alice",
            null,
            "Bob",
            true,
            START.minusSeconds(30 * 60).toEpochMilli(),
            "chat",
            "hello",
            false,
            false);

    var result = service.submit(tooYoung);

    assertEquals(ReportSubmissionService.SubmitReason.ACCOUNT_TOO_YOUNG, result.reason());

    assertEquals("60", result.placeholders().get("minutes"));
  }

  @Test
  void detailValidationMapsToSpecificReasons() {

    ReportSubmissionService service = service(readyRepository(), unlimited());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_TOO_SHORT,
        service.submit(request("Bob", "chat", "a")).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_TOO_LONG,
        service.submit(request("Bob", "chat", "x".repeat(21))).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_BLOCKED,
        service.submit(request("Bob", "chat", "this is BADWORD")).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_ILLEGAL,
        service.submit(request("Bob", "chat", "hello\u0007world")).reason());
  }

  @Test
  void chatSelectionsAllowBlankDetailAndArePersisted() {

    ReportRepository repository = readyRepository();

    ReportSubmissionService service = service(repository, unlimited());

    List<ReportChatSelection> selections =
        List.of(
            new ReportChatSelection(1_000L, "Bob", "you are bad"),
            new ReportChatSelection(2_000L, "Bob", "stop playing"));

    var result = service.submit(request("Bob", "chat", "", selections));

    assertTrue(result.success());

    assertEquals("", result.report().detail());

    assertEquals(selections, result.report().chatSelections());

    assertEquals(selections, repository.get(result.report().id()).orElseThrow().chatSelections());
  }

  @Test
  void blankDetailIsRejectedWithoutSelectionsWhenRequired() {

    InputValidator required =
        new InputValidator(
            new InputValidator.Settings(2, 20, 40, true, false, List.of(), "^[A-Za-z0-9_]{3,16}$"));

    ReportSubmissionService service =
        new ReportSubmissionService(
            readyRepository(),
            unlimited(),
            required,
            categories,
            new ReportSubmissionService.Settings("test-server", true, true, false, 0),
            Clock.fixed(START, ZoneOffset.UTC));

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_EMPTY,
        service.submit(request("Bob", "chat", "", List.of())).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_EMPTY,
        service.submit(request("Bob", "chat", " ", List.of())).reason());

    assertTrue(
        service
            .submit(
                request(
                    "Bob",
                    "chat",
                    "",
                    List.of(new ReportChatSelection(1_000L, "Bob", "you are bad"))))
            .success());
  }

  @Test
  void chatSelectionsDoNotBypassOtherDetailValidation() {

    ReportSubmissionService service = service(readyRepository(), unlimited());

    List<ReportChatSelection> selections =
        List.of(new ReportChatSelection(1_000L, "Bob", "you are bad"));

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_TOO_SHORT,
        service.submit(request("Bob", "chat", "a", selections)).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_TOO_LONG,
        service.submit(request("Bob", "chat", "x".repeat(21), selections)).reason());

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_BLOCKED,
        service.submit(request("Bob", "chat", "this is BADWORD", selections)).reason());
  }

  @Test
  void requiredDetailProducesEmptyReason() {

    InputValidator required =
        new InputValidator(
            new InputValidator.Settings(2, 20, 40, true, false, List.of(), "^[A-Za-z0-9_]{3,16}$"));

    ReportSubmissionService service =
        new ReportSubmissionService(
            readyRepository(),
            unlimited(),
            required,
            categories,
            new ReportSubmissionService.Settings("test-server", true, true, false, 0),
            Clock.fixed(START, ZoneOffset.UTC));

    assertEquals(
        ReportSubmissionService.SubmitReason.DETAIL_EMPTY,
        service.submit(request("Bob", "chat", "")).reason());
  }

  @Test
  void repositoryMustBeReadyUnlessSubmissionIsSystemGenerated() {

    ReportRepository notReady = new ReportRepository();

    ReportSubmissionService service = service(notReady, unlimited());

    assertEquals(
        ReportSubmissionService.SubmitReason.NOT_READY,
        service.submit(request("Bob", "chat", "hello")).reason());

    var system =
        new ReportSubmissionService.SubmitRequest(
            new UUID(0L, 0L), "SYSTEM", null, "Bob", true, 0L, "chat", "auto", true, true);

    var result = service.submit(system);

    assertTrue(result.success());

    assertTrue(result.report().systemGenerated());

    assertEquals(1, notReady.size());
  }

  @Test
  void cooldownRejectsASecondReportAndBypassSkipsIt() {

    ReportRepository repository = readyRepository();

    RateLimiter limiter =
        new RateLimiter(
            new RateLimitPolicy(60, 0, 0, 0, 0),
            Clock.fixed(START, ZoneOffset.UTC),
            ZoneOffset.UTC);

    ReportSubmissionService service = service(repository, limiter);

    assertTrue(service.submit(request("Bob", "chat", "first")).success());

    var blocked = service.submit(request("Charlie", "chat", "second"));

    assertEquals(ReportSubmissionService.SubmitReason.COOLDOWN, blocked.reason());

    assertEquals("60", blocked.placeholders().get("seconds"));

    var bypass =
        new ReportSubmissionService.SubmitRequest(
            TestFixtures.ALICE, "Alice", null, "Dave", true, 0L, "chat", "third", true, false);

    assertTrue(service.submit(bypass).success());
  }

  @Test
  void dailyLimitAndDuplicateAndMaxOpenDecisionsSurface() {

    RateLimiter dailyLimiter =
        new RateLimiter(
            new RateLimitPolicy(0, 1, 0, 0, 0), Clock.fixed(START, ZoneOffset.UTC), ZoneOffset.UTC);

    ReportSubmissionService dailyService = service(readyRepository(), dailyLimiter);

    assertTrue(dailyService.submit(request("Bob", "chat", "first")).success());

    assertEquals(
        ReportSubmissionService.SubmitReason.DAILY_LIMIT,
        dailyService.submit(request("Charlie", "chat", "second")).reason());

    ReportRepository withDuplicate = readyRepository();

    withDuplicate.put(
        TestFixtures.open(40L, TestFixtures.ALICE, "Alice", "Bob", "chat", START.toEpochMilli()));

    RateLimiter duplicateLimiter =
        new RateLimiter(
            new RateLimitPolicy(0, 0, 0, 600, 0),
            Clock.fixed(START, ZoneOffset.UTC),
            ZoneOffset.UTC);

    ReportSubmissionService duplicateService = service(withDuplicate, duplicateLimiter);

    var duplicate = duplicateService.submit(request("Bob", "chat", "again"));

    assertEquals(ReportSubmissionService.SubmitReason.DUPLICATE, duplicate.reason());

    assertEquals("40", duplicate.placeholders().get("id"));

    ReportRepository withOpen = new ReportRepository();

    withOpen.seed(
        List.of(
            TestFixtures.open(1L, TestFixtures.BOB, "Bob", "Dave", "chat", START.toEpochMilli()),
            TestFixtures.open(
                2L, TestFixtures.STAFF, "Staff", "Dave", "chat", START.toEpochMilli())));

    RateLimiter maxOpenLimiter =
        new RateLimiter(
            new RateLimitPolicy(0, 0, 0, 0, 2), Clock.fixed(START, ZoneOffset.UTC), ZoneOffset.UTC);

    ReportSubmissionService maxOpenService = service(withOpen, maxOpenLimiter);

    assertEquals(
        ReportSubmissionService.SubmitReason.MAX_OPEN,
        maxOpenService.submit(request("Dave", "chat", "third")).reason());
  }

  @Test
  void nullRequestIsRejectedSafely() {

    ReportSubmissionService service = service(readyRepository(), unlimited());

    assertEquals(
        ReportSubmissionService.SubmitReason.INVALID_TARGET, service.submit(null).reason());
  }
}
