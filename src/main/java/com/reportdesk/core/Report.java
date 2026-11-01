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
    List<ReportChatSelection> chatSelections) {

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
        List.of());
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        chatSelections);
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
        selections == null ? List.of() : selections);
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
      List<ReportChatSelection> newChatSelections) {

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
        newChatSelections);
  }
}
