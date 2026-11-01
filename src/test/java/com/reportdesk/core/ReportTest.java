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

package com.reportdesk.core;
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import java.util.List; import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertNull; import static org.junit.jupiter.api.Assertions.assertTrue;
class ReportTest {


@Test

 void constructorNormalizesNullValues() {



 Report report = new Report(1L, null, null, null, null, null, null, null,





 10L, 10L, null, null, 0L, null, null, 0L, null, null, null, null, false, null);




assertEquals(new UUID(0L, 0L), report.reporterId());



 assertEquals("", report.reporterName());



 assertEquals("", report.targetName());



 assertEquals(ReportStatus.OPEN, report.status());



 assertEquals(List.of(), report.notes());



 assertFalse(report.evidence().present());



 assertTrue(report.isOpen());

 }


@Test

 void claimAndReleaseTransitionStatus() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);




Report claimed = report.withClaim(TestFixtures.STAFF, "Staff", 200L);



 assertEquals(ReportStatus.CLAIMED, claimed.status());



 assertEquals(TestFixtures.STAFF, claimed.claimedBy());



 assertEquals("Staff", claimed.claimedByName());



 assertEquals(200L, claimed.claimedAt());



 assertEquals(200L, claimed.updatedAt());



 assertTrue(claimed.isClaimed());




Report released = claimed.withClaimReleased(300L);



 assertEquals(ReportStatus.OPEN, released.status());



 assertNull(released.claimedBy());



 assertEquals("", released.claimedByName());



 assertEquals(0L, released.claimedAt());



 assertEquals(300L, released.updatedAt());

 }


@Test

 void closingStoresVerdictAndClosingStaff() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);




Report closed = report.withClosed(Verdict.INSUFFICIENT, TestFixtures.STAFF, "Staff",





 "not enough proof", 500L);




assertEquals(ReportStatus.REJECTED, closed.status());



 assertEquals(Verdict.INSUFFICIENT.name(), closed.verdict());



 assertEquals(TestFixtures.STAFF, closed.closedBy());



 assertEquals("Staff", closed.closedByName());



 assertEquals(500L, closed.closedAt());



 assertEquals("not enough proof", closed.closeNote());



 assertTrue(closed.isTerminal());

 }


@Test

 void reopeningClearsClaimAndClosure() {



 Report closed = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L)





 .withClosed(Verdict.RESOLVED, TestFixtures.STAFF, "Staff", "handled", 500L);




Report reopened = closed.withReopened(600L);




assertEquals(ReportStatus.OPEN, reopened.status());



 assertNull(reopened.claimedBy());



 assertNull(reopened.closedBy());



 assertEquals("", reopened.verdict());



 assertEquals("", reopened.closeNote());



 assertEquals(0L, reopened.closedAt());



 assertEquals(600L, reopened.updatedAt());

 }


@Test

 void notesAreAppendedImmutably() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);



 ReportNote note = new ReportNote(TestFixtures.STAFF, "Staff", 700L, "checked logs");




Report updated = report.withNote(note, 700L);




assertEquals(List.of(note), updated.notes());



 assertTrue(report.notes().isEmpty());



 assertEquals(700L, updated.updatedAt());

 }


@Test

 void evidenceMetaCanBeAttached() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);



 EvidenceMeta meta = new EvidenceMeta(true, 800L, "world", 1, 2, 3, 4, 5, 6, 7);




Report updated = report.withEvidence(meta, 800L);




assertEquals(meta, updated.evidence());



 assertEquals(800L, updated.updatedAt());



 assertTrue(updated.evidence().present());



 assertTrue(updated.evidence().shortSummary().contains("breaks=6"));

 }


@Test

 void statusParsingIsLenient() {



 assertEquals(ReportStatus.OPEN, ReportStatus.parse(" open ", ReportStatus.REJECTED));



 assertEquals(ReportStatus.AUTO_CLOSED, ReportStatus.parse("auto-closed", ReportStatus.OPEN));



 assertEquals(ReportStatus.REJECTED, ReportStatus.parse("unknown", ReportStatus.REJECTED));



 assertEquals(ReportStatus.OPEN, ReportStatus.parse(null, ReportStatus.OPEN));

 }


@Test

 void verdictParsingSupportsAliases() {



 assertEquals(Verdict.RESOLVED, Verdict.parse("accept"));



 assertEquals(Verdict.REJECTED, Verdict.parse("reject"));



 assertEquals(Verdict.DUPLICATE, Verdict.parse("dup"));



 assertEquals(Verdict.INSUFFICIENT, Verdict.parse("evidence-insufficient"));



 assertEquals(Verdict.AUTO_CLOSED, Verdict.parse("auto_closed"));



 assertNull(Verdict.parse("nonsense"));



 assertNull(Verdict.parse(null));

 }


@Test

 void evidenceMetaSummaryHandlesMissingEvidence() {



 assertEquals("none", EvidenceMeta.none().shortSummary());



 assertFalse(EvidenceMeta.of(null).present());

 }


@Test

 void priorityDefaultsToNormalAndSlaIsNotTrackedByDefault() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);




assertEquals(ReportPriority.NORMAL, report.priority());



 assertEquals(0L, report.slaDueAt());



 assertFalse(report.isOverdue(1_000L));



 assertEquals(0, report.unreadReplies());



 assertTrue(report.lastReply().isEmpty());

 }


@Test

 void priorityAndSlaCanBeUpdated() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);




Report updated = report.withPriority(ReportPriority.URGENT, 5_000L, 200L);




assertEquals(ReportPriority.URGENT, updated.priority());



 assertEquals(5_000L, updated.slaDueAt());



 assertEquals(200L, updated.updatedAt());



 assertTrue(updated.isOverdue(5_001L));



 assertFalse(updated.isOverdue(5_000L));



 assertFalse(updated.withClosed(Verdict.RESOLVED, TestFixtures.STAFF, "Staff", "", 6_000L)





 .isOverdue(7_000L));

 }


@Test

 void repliesTrackUnreadStateUntilMarkedRead() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L);



 ReportReply staffReply = new ReportReply(TestFixtures.STAFF, "Staff", true, "checking", 200L);



 ReportReply playerReply = new ReportReply(TestFixtures.ALICE, "Alice", false, "thanks", 300L);




Report threaded = report.withReply(staffReply, 200L).withReply(playerReply, 300L);




assertEquals(2, threaded.replies().size());



 assertEquals(1, threaded.unreadReplies());



 assertEquals(staffReply, threaded.replies().get(0));



 assertEquals("thanks", threaded.lastReply().orElseThrow().text());




Report read = threaded.withReporterReadAt(250L);



 assertEquals(0, read.unreadReplies());



 assertEquals(250L, read.reporterReadAt());



 assertEquals(threaded.updatedAt(), read.updatedAt());

 }


@Test

 void slaAlertTimestampIsRecordedOnce() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L)





 .withPriority(ReportPriority.HIGH, 1_000L, 100L);




Report alerted = report.withSlaAlerted(2_000L);




assertEquals(2_000L, alerted.slaAlertedAt());



 assertEquals(100L, alerted.updatedAt());



 assertEquals(report.slaDueAt(), alerted.slaDueAt());

 }


@Test

 void chatSelectionsSurviveCopiesAndCanBeReplaced() {



 ReportChatSelection first = new ReportChatSelection(1_000L, "Bob", "line one");



 ReportChatSelection second = new ReportChatSelection(2_000L, "Bob", "line two");



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 100L)





 .withChatSelections(List.of(first, second), 150L);




assertEquals(List.of(first, second), report.chatSelections());



 assertEquals(150L, report.updatedAt());




Report claimed = report.withClaim(TestFixtures.STAFF, "Staff", 200L);



 assertEquals(List.of(first, second), claimed.chatSelections());




Report replied = claimed.withReply(





 new ReportReply(TestFixtures.STAFF, "Staff", true, "checking", 300L), 300L);



 assertEquals(List.of(first, second), replied.chatSelections());




Report closed = replied.withClosed(Verdict.RESOLVED, TestFixtures.STAFF, "Staff", "", 400L);



 assertEquals(List.of(first, second), closed.chatSelections());




Report cleared = closed.withChatSelections(null, 500L);



 assertTrue(cleared.chatSelections().isEmpty());



 assertEquals(500L, cleared.updatedAt());

 }


@Test

 void priorityParsingIsLenient() {



 assertEquals(ReportPriority.HIGH, ReportPriority.parse(" high ", null));



 assertEquals(ReportPriority.URGENT, ReportPriority.parse("URGENT", null));



 assertEquals(ReportPriority.LOW, ReportPriority.parse("unknown", ReportPriority.LOW));



 assertEquals(ReportPriority.NORMAL, ReportPriority.parse(null, ReportPriority.NORMAL));



 assertEquals("urgent", ReportPriority.URGENT.id());



 assertTrue(ReportPriority.URGENT.weight() > ReportPriority.LOW.weight());

 } }
