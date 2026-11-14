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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistent report model. Instances are immutable; service classes create updated copies with the
 * {@code with*} helpers.
 */
public record Report(
    long id,
    UUID reporterId,
    String reporterName,
    UUID targetId,
    String targetName,
    String category,
    String detail,
    ReportStatus status,
    long createdAt,
    long updatedAt,
    UUID claimedBy,
    String claimedByName,
    long claimedAt,
    UUID closedBy,
    String closedByName,
    long closedAt,
    String verdict,
    String closeNote,
    List<ReportNote> notes,
    EvidenceMeta evidence,
    boolean systemGenerated,
    String server,
    ReportPriority priority,
    long slaDueAt,
    long slaAlertedAt,
    List<ReportReply> replies,
    long reporterReadAt,
    List<ReportChatSelection> chatSelections,
    int smartScore,
    String reportedFrom,
    String ipHash) {

  public Report {

    reporterId = reporterId == null ? new UUID(0L, 0L) : reporterId;

    reporterName = reporterName == null ? "" : reporterName;

    targetName = targetName == null ? "" : targetName;

    category = category == null ? "" : category;

    detail = detail == null ? "" : detail;

    status = status == null ? ReportStatus.OPEN : status;

    claimedByName = claimedByName == null ? "" : claimedByName;

    closedByName = closedByName == null ? "" : closedByName;

    verdict = verdict == null ? "" : verdict;

    closeNote = closeNote == null ? "" : closeNote;

    notes = notes == null ? List.of() : List.copyOf(notes);

    evidence = evidence == null ? EvidenceMeta.none() : evidence;

    server = server == null ? "" : server;

    priority = priority == null ? ReportPriority.NORMAL : priority;

    slaDueAt = Math.max(0L, slaDueAt);

    slaAlertedAt = Math.max(0L, slaAlertedAt);

    replies = replies == null ? List.of() : List.copyOf(replies);

    reporterReadAt = Math.max(0L, reporterReadAt);

    chatSelections = chatSelections == null ? List.of() : List.copyOf(chatSelections);

    smartScore = Math.max(0, smartScore);

    // Legacy files predate both fields; Gson loads them as null and they stay empty.
    reportedFrom = reportedFrom == null ? "" : reportedFrom;

    ipHash = ipHash == null ? "" : ipHash;
  }

  /**
   * Convenience constructor used by tests and integrations that do not need
   *
   * <p>the triage/conversation fields yet.
   */
  public Report(
      long id,
      UUID reporterId,
      String reporterName,
      UUID targetId,
      String targetName,
      String category,
      String detail,
      ReportStatus status,
      long createdAt,
      long updatedAt,
      UUID claimedBy,
      String claimedByName,
      long claimedAt,
      UUID closedBy,
      String closedByName,
      long closedAt,
      String verdict,
      String closeNote,
      List<ReportNote> notes,
      EvidenceMeta evidence,
      boolean systemGenerated,
      String server) {

    this(
        id,
        reporterId,
        reporterName,
        targetId,
        targetName,
        category,
        detail,
        status,
        createdAt,
        updatedAt,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        systemGenerated,
        server,
        ReportPriority.NORMAL,
        0L,
        0L,
        List.of(),
        0L,
        List.of(),
        0,
        "",
        "");
  }

  public boolean isOpen() {

    return status.isOpen();
  }

  public boolean isTerminal() {

    return status.isTerminal();
  }

  public boolean isClaimed() {

    return status == ReportStatus.CLAIMED;
  }

  public boolean isOverdue(long now) {

    return !isTerminal() && slaDueAt > 0L && now > slaDueAt;
  }

  /**
   * Case-insensitive text search over the report's stable fields. The lower-casing is paid on every
   * call; {@link ReportRepository#filter} passes a memoised field array (see {@link
   * #matchesQuery(String, String[])}) so the cost is spread over renders instead of repeated on
   * every page navigation.
   *
   * @param query an already lower-cased query (as produced by {@link ReportFilter}).
   */
  public boolean matchesQuery(String query) {

    return matchesQuery(query, null);
  }

  /**
   * Same as {@link #matchesQuery(String)}, but the lower-cased search fields may be supplied by
   * the caller so repeated filter passes over the same repository state do not re-pay the
   * {@code toLowerCase} work.
   *
   * <p>A non-null {@code cachedFields} is trusted to have been built for a report instance with
   * the same id and {@code updatedAt}: every {@code with*} method that changes one of the
   * searched fields also changes {@code updatedAt}, so an unchanged {@code updatedAt} means the
   * fields are still valid.
   *
   * @param cachedFields memoised lower-cased fields, or {@code null} to build them here.
   */
  public boolean matchesQuery(String query, String[] cachedFields) {

    if (query == null || query.isEmpty()) {

      return true;
    }

    String[] fields = cachedFields != null ? cachedFields : searchFields(this);

    for (String field : fields) {

      if (field.contains(query)) {

        return true;
      }
    }

    return false;
  }

  /** The lower-cased copies of the fields a text query is matched against. */
  public static String[] searchFields(Report report) {

    return new String[] {
      lower(report.targetName()),
      lower(report.reporterName()),
      lower(report.category()),
      lower(report.detail()),
      lower(report.claimedByName()),
      lower(report.closedByName()),
      lower(report.verdict()),
      lower(report.priority().id()),
      String.valueOf(report.id()),
      "#" + report.id()
    };
  }

  private static String lower(String value) {

    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  /** Staff replies the reporter has not read yet. */
  public int unreadReplies() {

    int unread = 0;

    for (ReportReply reply : replies) {

      if (reply.staff() && reply.createdAt() > reporterReadAt) {

        unread++;
      }
    }

    return unread;
  }

  public Optional<ReportReply> lastReply() {

    return replies.isEmpty() ? Optional.empty() : Optional.of(replies.get(replies.size() - 1));
  }

  public Report withStatus(ReportStatus newStatus, long now) {

    return copy(
        newStatus,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withClaim(UUID staffId, String staffName, long now) {

    return copy(
        ReportStatus.CLAIMED,
        now,
        staffId,
        staffName,
        now,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withClaimReleased(long now) {

    return copy(
        ReportStatus.OPEN,
        now,
        null,
        "",
        0L,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withClosed(Verdict result, UUID staffId, String staffName, String note, long now) {

    return copy(
        result.status(),
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        staffId,
        staffName,
        now,
        result.name(),
        note == null ? "" : note,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withReopened(long now) {

    return copy(
        ReportStatus.OPEN,
        now,
        null,
        "",
        0L,
        null,
        "",
        0L,
        "",
        "",
        notes,
        evidence,
        priority,
        slaDueAt,
        0L,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withNote(ReportNote note, long now) {

    List<ReportNote> updated = new java.util.ArrayList<>(notes);

    updated.add(note);

    return copy(
        status,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        List.copyOf(updated),
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withEvidence(EvidenceMeta meta, long now) {

    return copy(
        status,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        meta,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withPriority(ReportPriority newPriority, long dueAt, long now) {

    return copy(
        status,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        newPriority == null ? ReportPriority.NORMAL : newPriority,
        Math.max(0L, dueAt),
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withSlaAlerted(long now) {

    return copy(
        status,
        updatedAt,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        Math.max(0L, now),
        replies,
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withReply(ReportReply reply, long now) {

    if (reply == null) {

      return this;
    }

    List<ReportReply> updated = new java.util.ArrayList<>(replies);

    updated.add(reply);

    return copy(
        status,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        List.copyOf(updated),
        reporterReadAt,
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withReporterReadAt(long now) {

    return copy(
        status,
        updatedAt,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        Math.max(0L, now),
        chatSelections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  public Report withChatSelections(List<ReportChatSelection> selections, long now) {

    return copy(
        status,
        now,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        selections == null ? List.of() : selections,
        smartScore,
        reportedFrom,
        ipHash);
  }

  /**
   * Records the smart-priority score without touching {@code updatedAt}; the score is written at
   * submission time and is not one of the text-searched fields.
   */
  public Report withSmartScore(int score) {

    return copy(
        status,
        updatedAt,
        claimedBy,
        claimedByName,
        claimedAt,
        closedBy,
        closedByName,
        closedAt,
        verdict,
        closeNote,
        notes,
        evidence,
        priority,
        slaDueAt,
        slaAlertedAt,
        replies,
        reporterReadAt,
        chatSelections,
        Math.max(0, score),
        reportedFrom,
        ipHash);
  }

  private Report copy(
      ReportStatus newStatus,
      long newUpdatedAt,
      UUID newClaimedBy,
      String newClaimedByName,
      long newClaimedAt,
      UUID newClosedBy,
      String newClosedByName,
      long newClosedAt,
      String newVerdict,
      String newCloseNote,
      List<ReportNote> newNotes,
      EvidenceMeta newEvidence,
      ReportPriority newPriority,
      long newSlaDueAt,
      long newSlaAlertedAt,
      List<ReportReply> newReplies,
      long newReporterReadAt,
      List<ReportChatSelection> newChatSelections,
      int newSmartScore,
      String newReportedFrom,
      String newIpHash) {

    return new Report(
        id,
        reporterId,
        reporterName,
        targetId,
        targetName,
        category,
        detail,
        newStatus,
        createdAt,
        newUpdatedAt,
        newClaimedBy,
        newClaimedByName,
        newClaimedAt,
        newClosedBy,
        newClosedByName,
        newClosedAt,
        newVerdict,
        newCloseNote,
        newNotes,
        newEvidence,
        systemGenerated,
        server,
        newPriority,
        newSlaDueAt,
        newSlaAlertedAt,
        newReplies,
        newReporterReadAt,
        newChatSelections,
        newSmartScore,
        newReportedFrom,
        newIpHash);
  }
}
