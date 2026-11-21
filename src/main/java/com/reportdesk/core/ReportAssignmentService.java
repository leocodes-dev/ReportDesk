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

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure assignment/transfer rules for multi-admin collaboration (roadmap P1#4).
 *
 * <p>{@link #assign} hands an unclaimed report to a specific online admin and {@link #transfer}
 * moves a claim between admins, keeping the original claim timestamp so the "when was this
 * picked up" timeline survives the hand-off. The Bukkit service layer delegates the state
 * transitions here and adds persistence, auditing and notification; everything in this class is
 * deterministic and unit-testable without a server.
 *
 * <p>Both operations are idempotent: assigning or transferring a report to the admin that already
 * holds the claim changes nothing and reports {@link Outcome#ALREADY_ASSIGNED}, so repeated
 * calls never duplicate notifications.
 */
public final class ReportAssignmentService {

  /** Result of an assign/transfer attempt. */
  public enum Outcome {

    /** The report now carries the target admin's claim. */
    ASSIGNED,

    /** The claim already sits with the target admin; nothing changed. */
    ALREADY_ASSIGNED,

    /** The claim moved to the target admin (original claim time preserved). */
    TRANSFERRED,

    /** The report is claimed by a different admin; use a transfer instead. */
    CLAIMED_BY_OTHER,

    /** The report is not claimed, so there is nothing to transfer. */
    NOT_CLAIMED,

    /** The actor does not hold the claim and no override rights were granted. */
    NOT_OWNER,

    /** The report is terminal; it can no longer be assigned or transferred. */
    TERMINAL
  }

  /** Outcome plus the resulting report state and stable placeholders for messaging. */
  public record Result(Outcome outcome, Report report, Map<String, String> placeholders) {

    public Result {

      placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }

    public boolean isChange() {

      return outcome == Outcome.ASSIGNED || outcome == Outcome.TRANSFERRED;
    }

    /** Convenience factory for results that carry no placeholders. */
    public static Result of(Outcome outcome, Report report) {

      return new Result(outcome, report, Map.of());
    }
  }

  private final ReportRepository repository;

  private final Clock clock;

  public ReportAssignmentService(ReportRepository repository, Clock clock) {

    if (repository == null) {

      throw new NullPointerException("repository");
    }

    this.repository = repository;

    this.clock = clock == null ? Clock.systemUTC() : clock;
  }

  /**
   * Assigns an unclaimed (open) report to the target admin.
   *
   * @param report the report to assign
   * @param targetId the unique id of the admin receiving the claim
   * @param targetName the admin's player name
   * @return the outcome; the repository is updated when the assignment takes effect
   */
  public Result assign(Report report, UUID targetId, String targetName) {

    if (report == null || report.isTerminal()) {

      return Result.of(Outcome.TERMINAL, report);
    }

    if (report.isClaimed()) {

      if (targetId != null && targetId.equals(report.claimedBy())) {

        return Result.of(Outcome.ALREADY_ASSIGNED, report);
      }

      return new Result(Outcome.CLAIMED_BY_OTHER, report, placeholders(report, report.claimedByName()));
    }

    Report updated = report.withClaim(targetId, targetName, clock.millis());

    repository.put(updated);

    return new Result(Outcome.ASSIGNED, updated, placeholders(updated, targetName));
  }

  /**
   * Moves the claim of a claimed report to the target admin, keeping the original claim time.
   *
   * @param report the claimed report to hand over
   * @param actorId who runs the transfer; a player that does not hold the claim needs
   *     {@code override} to succeed
   * @param override true when the actor may hand the claim over despite not holding it (for
   *     example the console or a higher-privileged admin)
   * @param targetId the unique id of the admin receiving the claim
   * @param targetName the admin's player name
   * @return the outcome; the repository is updated when the transfer takes effect
   */
  public Result transfer(
      Report report, UUID actorId, boolean override, UUID targetId, String targetName) {

    if (report == null || report.isTerminal()) {

      return Result.of(Outcome.TERMINAL, report);
    }

    if (!report.isClaimed()) {

      return Result.of(Outcome.NOT_CLAIMED, report);
    }

    if (targetId != null && targetId.equals(report.claimedBy())) {

      return Result.of(Outcome.ALREADY_ASSIGNED, report);
    }

    UUID claimer = report.claimedBy();

    boolean owner =
        claimer != null
            && actorId != null
            && !actorId.equals(new UUID(0L, 0L))
            && actorId.equals(claimer);

    if (!owner && !override) {

      return new Result(Outcome.NOT_OWNER, report, placeholders(report, report.claimedByName()));
    }

    Report updated = report.withTransfer(targetId, targetName, clock.millis());

    repository.put(updated);

    return new Result(Outcome.TRANSFERRED, updated, placeholders(updated, targetName));
  }

  private static Map<String, String> placeholders(Report report, String staff) {

    Map<String, String> values = new LinkedHashMap<>();

    values.put("id", String.valueOf(report == null ? 0L : report.id()));

    values.put("staff", staff == null ? "" : staff);

    return values;
  }
}
