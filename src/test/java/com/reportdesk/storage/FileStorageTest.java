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

package com.reportdesk.storage;
import com.reportdesk.config.PluginConfig; import com.reportdesk.core.BreakSample; import com.reportdesk.core.ChatSample; import com.reportdesk.core.Evidence; import com.reportdesk.core.EvidenceMeta; import com.reportdesk.core.MoveSample; import com.reportdesk.core.PendingNotice; import com.reportdesk.core.Report; import com.reportdesk.core.ReportChatSelection; import com.reportdesk.core.ReportStatus; import com.reportdesk.core.WatchEntry; import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.AfterEach; import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.io.IOException; import java.nio.charset.StandardCharsets; import java.nio.file.Files; import java.nio.file.Path; import java.nio.file.attribute.FileTime; import java.time.Instant; import java.util.ArrayList; import java.util.List; import java.util.Optional; import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class FileStorageTest {


@TempDir

 Path temp;


private final List<String> warnings = new ArrayList<>();

 private FileStorage storage;


@BeforeEach

 void setUp() {



 // High compaction threshold so per-save tests stay append-only; the



 // dedicated compaction tests use a low threshold via their own instance.



 PluginConfig.Storage settings =





 new PluginConfig.Storage("data", false, 0, 60, 5, 1_000_000, 0L);



 storage = new FileStorage(temp, settings, warnings::add);

 }


@AfterEach

 void tearDown() {



 if (storage != null) {





 storage.shutdown();



 }

 }


@Test

 void saveReportAppendsWithoutRewritingSnapshot() {



 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);



 Report updated = TestFixtures.report(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating",





 ReportStatus.RESOLVED, 1_000L, 2_000L, 2_000L);




storage.saveReport(first).join();



 // The write-ahead log holds the entry; reports.json is not created yet.



 assertFalse(Files.exists(temp.resolve("data").resolve("reports.json")));



 assertTrue(Files.isRegularFile(temp.resolve("data").resolve("reports.log")));




storage.saveReport(updated).join();



 List<Report> loaded = storage.loadReports().join();




assertEquals(1, loaded.size());



 assertEquals(updated, loaded.get(0));



 assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);

 }


@Test

 void compactionFoldsLogIntoSnapshotAndSurvivesReload() {



 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);



 Report second = TestFixtures.report(2L, TestFixtures.BOB, "Bob", "Charlie", "chat",





 ReportStatus.RESOLVED, 2_000L, 3_000L, 3_000L);




storage.saveReport(first).join();



 storage.saveReport(second).join();



 storage.compactReports().join();




assertTrue(Files.isRegularFile(temp.resolve("data").resolve("reports.json")));



 assertFalse(Files.exists(temp.resolve("data").resolve("reports.log")));




// A fresh storage instance (simulating a restart) must see the same data.



 FileStorage reloaded = new FileStorage(temp, new PluginConfig.Storage("data", false, 0, 60, 5,





 1_000_000, 0L), warnings::add);



 try {





 List<Report> loaded = reloaded.loadReports().join();





 assertEquals(2, loaded.size());





 assertEquals(first, loaded.get(0));





 assertEquals(second, loaded.get(1));



 } finally {





 reloaded.shutdown();



 }

 }


@Test

 void deleteReportRemovesEntryFromReplay() {



 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);



 Report second = TestFixtures.report(2L, TestFixtures.BOB, "Bob", "Charlie", "chat",





 ReportStatus.RESOLVED, 2_000L, 3_000L, 3_000L);




storage.saveReport(first).join();



 storage.saveReport(second).join();



 storage.deleteReport(1L).join();




List<Report> loaded = storage.loadReports().join();



 assertEquals(1, loaded.size());



 assertEquals(2L, loaded.get(0).id());




// Compaction must drop the deleted entry from the snapshot permanently.



 storage.compactReports().join();



 FileStorage reloaded = new FileStorage(temp, new PluginConfig.Storage("data", false, 0, 60, 5,





 1_000_000, 0L), warnings::add);



 try {





 List<Report> snapshot = reloaded.loadReports().join();





 assertEquals(1, snapshot.size());





 assertEquals(2L, snapshot.get(0).id());



 } finally {





 reloaded.shutdown();



 }

 }


@Test

 void thresholdTriggersAutomaticCompaction() {



 // Compact after just 1 append.



 PluginConfig.Storage settings =





 new PluginConfig.Storage("data", false, 0, 60, 5, 1, 0L);



 FileStorage low = new FileStorage(temp, settings, warnings::add);



 try {





 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);





 Report second = TestFixtures.report(2L, TestFixtures.BOB, "Bob", "Charlie", "chat",







 ReportStatus.RESOLVED, 2_000L, 3_000L, 3_000L);






// Threshold = 1: the very first append compacts (snapshot holds #1).





 low.saveReport(first).join();





 // Second append: log re-created, threshold fires again (snapshot holds #1+#2).





 low.saveReport(second).join();






assertTrue(Files.isRegularFile(temp.resolve("data").resolve("reports.json")));





 assertFalse(Files.exists(temp.resolve("data").resolve("reports.log")));





 List<Report> loaded = low.loadReports().join();





 assertEquals(2, loaded.size());





 assertEquals(first, loaded.get(0));





 assertEquals(second, loaded.get(1));





 assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);



 } finally {





 low.shutdown();



 }

 }


@Test

 void deleteBeforeThresholdLeavesSurvivingSnapshot() {



 // Threshold = 1; a delete marker that arrives before compaction must



 // not resurrect an entry that was already removed.



 PluginConfig.Storage settings =





 new PluginConfig.Storage("data", false, 0, 60, 5, 1, 0L);



 FileStorage low = new FileStorage(temp, settings, warnings::add);



 try {





 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);





 Report second = TestFixtures.open(2L, TestFixtures.BOB, "Bob", "Charlie", "chat", 2_000L);






low.saveReport(first).join();
 // compacts: snapshot = {1}





 low.saveReport(second).join();
// compacts: snapshot = {1, 2}





 low.deleteReport(1L).join();

// compacts: snapshot = {2}






List<Report> loaded = low.loadReports().join();





 assertEquals(1, loaded.size());





 assertEquals(2L, loaded.get(0).id());






// Simulated restart must keep the deletion.





 FileStorage reloaded = new FileStorage(temp, settings, warnings::add);





 try {







 List<Report> snapshot = reloaded.loadReports().join();







 assertEquals(1, snapshot.size());







 assertEquals(2L, snapshot.get(0).id());





 } finally {







 reloaded.shutdown();





 }





 assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);



 } finally {





 low.shutdown();



 }

 }


@Test

 void reportsRoundTripThroughSaveReports() {



 Report first = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L);



 Report second = TestFixtures.report(2L, TestFixtures.BOB, "Bob", "Charlie", "chat",





 ReportStatus.RESOLVED, 2_000L, 3_000L, 3_000L);




storage.saveReports(List.of(first, second)).join();



 List<Report> loaded = storage.loadReports().join();




assertEquals(2, loaded.size());



 assertEquals(first, loaded.get(0));



 assertEquals(second, loaded.get(1));



 assertTrue(Files.isRegularFile(temp.resolve("data").resolve("reports.json")));



 assertFalse(Files.exists(temp.resolve("data").resolve("reports.json.tmp")));



 assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);

 }


@Test

 void chatSelectionsRoundTripThroughJson() {



 List<ReportChatSelection> selections = List.of(





 new ReportChatSelection(1_000L, "Bob", "you are bad"),





 new ReportChatSelection(2_000L, "Bob", "stop playing")



 );



 Report report = TestFixtures.open(4L, TestFixtures.ALICE, "Alice", "Bob", "chat", 3_000L)





 .withChatSelections(selections, 3_000L);




storage.saveReports(List.of(report)).join();



 List<Report> loaded = storage.loadReports().join();




assertEquals(1, loaded.size());



 assertEquals(report, loaded.get(0));



 assertEquals(selections, loaded.get(0).chatSelections());



 assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);

 }


@Test

 void missingFileLoadsAsEmpty() {



 assertTrue(storage.loadReports().join().isEmpty());

 }


@Test

 void corruptFileLoadsAsEmptyAndWarns() throws IOException {



 Path file = temp.resolve("data").resolve("reports.json");



 Files.createDirectories(file.getParent());



 Files.writeString(file, "{not json", StandardCharsets.UTF_8);




assertTrue(storage.loadReports().join().isEmpty());



 assertFalse(warnings.isEmpty());

 }


@Test

 void invalidReportEntriesAreSkipped() throws IOException {



 Path file = temp.resolve("data").resolve("reports.json");



 Files.createDirectories(file.getParent());



 Files.writeString(file, """
{"schema":1,"reports":[
{"id":0,"targetName":"nobody"},
{"id":5,"targetName":"","category":"chat"},
{"id":6,"targetName":"Bob","category":"chat","status":"OPEN","reporterId":"11111111-1111-1111-1111-111111111111"}
]}
""", StandardCharsets.UTF_8);




List<Report> loaded = storage.loadReports().join();



 assertEquals(1, loaded.size());



 assertEquals(6L, loaded.get(0).id());



 assertFalse(warnings.isEmpty());

 }


@Test

 void evidenceRoundTripAndDeletion() {



 Evidence evidence = sampleEvidence(9L);




storage.saveEvidence(evidence).join();



 Optional<Evidence> loaded = storage.loadEvidence(9L).join();




assertTrue(loaded.isPresent());



 assertEquals(evidence, loaded.get());



 assertEquals(1, loaded.get().chat().size());



 assertTrue(storage.deleteEvidence(9L).join());



 assertTrue(storage.loadEvidence(9L).join().isEmpty());



 assertFalse(storage.deleteEvidence(9L).join());

 }


@Test

 void invalidEvidenceIsIgnored() {



 storage.saveEvidence(sampleEvidence(0L)).join();



 storage.saveEvidence(null).join();



 assertTrue(storage.loadEvidence(0L).join().isEmpty());

 }


@Test

 void purgeEvidenceOnlyRemovesOldFiles() throws IOException {



 storage.saveEvidence(sampleEvidence(1L)).join();



 storage.saveEvidence(sampleEvidence(2L)).join();



 Path oldFile = storage.evidenceDirectory().resolve("evidence-1.json");



 Files.setLastModifiedTime(oldFile, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));




int deleted = storage.purgeEvidenceBefore(Instant.parse("2021-01-01T00:00:00Z").toEpochMilli())





 .join();




assertEquals(1, deleted);



 assertTrue(storage.loadEvidence(1L).join().isEmpty());



 assertTrue(storage.loadEvidence(2L).join().isPresent());

 }


@Test

 void csvExportWritesUtf8BomAndHeader() throws IOException {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L);




Path exported = storage.exportCsv(List.of(report)).join();



 String content = Files.readString(exported, StandardCharsets.UTF_8);




assertTrue(Files.isRegularFile(exported));



 assertEquals(temp.resolve("data").resolve("export").normalize(), exported.getParent().normalize());



 assertTrue(content.startsWith("\ufeff"));



 assertTrue(content.contains("id,created_at,status,category,target,reporter"));

 }


@Test

 void exportHandlesNullReportCollection() throws IOException {



 Path exported = storage.exportCsv(null).join();



 assertTrue(Files.readString(exported, StandardCharsets.UTF_8).contains("id,created_at"));

 }


@Test

 void pendingNoticesRoundTripAndSkipInvalidEntries() throws IOException {



 PendingNotice notice = new PendingNotice(TestFixtures.ALICE,





 "notify.reporter-reply",





 java.util.Map.of("id", "7", "text", "We are checking this."), 1_500L);



 storage.savePendingNotices(List.of(notice)).join();




List<PendingNotice> loaded = storage.loadPendingNotices().join();



 assertEquals(List.of(notice), loaded);



 assertTrue(Files.isRegularFile(temp.resolve("data").resolve("pending-notices.json")));




Path file = temp.resolve("data").resolve("pending-notices.json");



 Files.writeString(file, """
{"schema":1,"notices":[
{"playerId":"00000000-0000-0000-0000-000000000000","messageKey":"x","placeholders":{},"createdAt":1},
{"playerId":"11111111-1111-1111-1111-111111111111","messageKey":"","placeholders":{},"createdAt":1},
{"playerId":"11111111-1111-1111-1111-111111111111","messageKey":"notify.reporter-claimed","placeholders":{"id":"9"},"createdAt":2}
]}
""", StandardCharsets.UTF_8);




List<PendingNotice> filtered = storage.loadPendingNotices().join();



 assertEquals(1, filtered.size());



 assertEquals("notify.reporter-claimed", filtered.get(0).messageKey());

 }


@Test

 void watchlistRoundTripsAndHandlesNull() {



 WatchEntry entry = new WatchEntry(TestFixtures.STAFF, "Bob", 2_000L);



 storage.saveWatchlist(List.of(entry)).join();




assertEquals(List.of(entry), storage.loadWatchlist().join());



 assertTrue(Files.isRegularFile(temp.resolve("data").resolve("watchlist.json")));




storage.saveWatchlist(null).join();



 assertTrue(storage.loadWatchlist().join().isEmpty());

 }


private static Evidence sampleEvidence(long reportId) {



 return new Evidence(





 1,





 reportId,





 1_500L,





 UUID.fromString("22222222-2222-2222-2222-222222222222"),





 "Bob",





 true,





 "world",





 10.5,





 64,





 -7.25,





 90.0f,





 0.0f,





 "SURVIVAL",





 20.0,





 18,





 5,





 42,





 List.of("DIAMOND_SWORD", "STONE x3"),





 List.of("IRON_HELMET"),





 List.of(new ChatSample(1_400L, "Bob", "hello")),





 List.of(new BreakSample(1_300L, "world", 1, 64, 2, "STONE")),





 List.of(new MoveSample(1_350L, "world", 1.5, 64, 2.5, true)),





 "test"



 );

 } }
