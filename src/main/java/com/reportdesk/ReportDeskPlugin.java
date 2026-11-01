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

package com.reportdesk;
import com.reportdesk.command.ReportCommand; import com.reportdesk.command.ReportsCommand; import com.reportdesk.config.BukkitConfigSource; import com.reportdesk.config.ConfigLoader; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.AuditEntry; import com.reportdesk.core.CategoryRegistry; import com.reportdesk.core.InputValidator; import com.reportdesk.core.RateLimiter; import com.reportdesk.core.Report; import com.reportdesk.core.ReportRepository; import com.reportdesk.core.ReportSubmissionService; import com.reportdesk.gui.GuiClickListener; import com.reportdesk.gui.GuiService; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.i18n.Messages; import com.reportdesk.listener.BlockBreakEvidenceListener; import com.reportdesk.listener.ChatEvidenceListener; import com.reportdesk.listener.ChatInputListener; import com.reportdesk.listener.MovementEvidenceListener; import com.reportdesk.listener.PlayerSessionListener; import com.reportdesk.service.AutoDetectService; import com.reportdesk.service.ChatInputService; import com.reportdesk.service.ConfigEditService; import com.reportdesk.service.EvidenceService; import com.reportdesk.service.MaintenanceService; import com.reportdesk.service.NotificationService; import com.reportdesk.service.PendingNoticeService; import com.reportdesk.service.PunishmentService; import com.reportdesk.service.ReportService; import com.reportdesk.service.ReportViewService; import com.reportdesk.service.WatchlistService; import com.reportdesk.service.WebhookService; import com.reportdesk.storage.AuditStorage; import com.reportdesk.storage.FileStorage; import org.bukkit.Bukkit; import org.bukkit.command.PluginCommand; import org.bukkit.entity.Player; import org.bukkit.event.HandlerList; import org.bukkit.event.Listener; import org.bukkit.permissions.Permission; import org.bukkit.permissions.PermissionDefault; import org.bukkit.plugin.java.JavaPlugin; import org.bukkit.scheduler.BukkitTask;
import java.nio.file.Files; import java.nio.file.Path; import java.time.Clock; import java.time.ZoneId; import java.util.ArrayList; import java.util.LinkedHashMap; import java.util.List; import java.util.Map; import java.util.UUID; import java.util.concurrent.CompletionException;
/**
* ReportDesk entry point.
*
* <p>Only the base reporting workflow, GUI and command listeners are always
* registered. The optional evidence layer (block-break, movement, chat capture)
* is registered only when its own switch is enabled in config.yml.</p>
*/ public final class ReportDeskPlugin extends JavaPlugin {


private static final long TASK_TICK_MILLIS = 50L;


private volatile PluginConfig config;

 private volatile Messages messages;

 private volatile InputValidator validator;

 private volatile RateLimiter rateLimiter;

 private volatile ReportSubmissionService submissionService;


private ReportRepository repository;

 private FileStorage storage;

 private AuditStorage auditStorage;


private EvidenceService evidence;

 private NotificationService notification;

 private PendingNoticeService offlineNotices;

 private WatchlistService watchlist;

 private ConfigEditService configEditor;

 private WebhookService webhook;

 private PunishmentService punishment;

 private ChatInputService chatInput;

 private ReportService reportService;

 private AutoDetectService autoDetect;

 private MaintenanceService maintenance;

 private GuiService gui;

 private ReportViewService view;


private final List<Listener> evidenceListeners = new ArrayList<>();

 private BukkitTask chatExpiryTask;

 private BukkitTask maintenanceTask;

 private BukkitTask autoDetectTask;

 private BukkitTask overdueTask;


@Override

 public void onEnable() {



 saveDefaultResources();



 loadConfiguration();




repository = new ReportRepository();



 storage = new FileStorage(getDataFolder().toPath(), config.storage(), this::warning);



 // The storage object is always enabled; plugin.audit() checks the live



 // config switch so /reports reload can toggle auditing without recreating



 // the executor.



 auditStorage = new AuditStorage(getDataFolder().toPath(), true,





 config.storage().shutdownTimeoutSeconds(), this::warning);




rebuildCoreServices();




evidence = new EvidenceService(this);



 webhook = new WebhookService(this);



 offlineNotices = new PendingNoticeService(this);



 watchlist = new WatchlistService(this);



 configEditor = new ConfigEditService(this);



 notification = new NotificationService(this);



 punishment = new PunishmentService(this);



 chatInput = new ChatInputService(this);



 autoDetect = new AutoDetectService(this, evidence);



 maintenance = new MaintenanceService(this);



 reportService = new ReportService(this);



 gui = new GuiService(this);



 view = new ReportViewService(this);




registerPermissions();



 registerCommands();



 registerBaseListeners();



 applyEvidenceListeners();



 scheduleTasks();



 loadData();



 offlineNotices.load();



 watchlist.load();



 configEditor.load();




getLogger().info("ReportDesk enabled with " + config.categories().size()





 + " categories; evidence layer " + (config.evidence().enabled() ? "enabled" : "disabled"));

 }


@Override

 public void onDisable() {



 cancelTasks();



 if (chatInput != null) {





 chatInput.clear();



 }



 if (evidence != null) {





 evidence.shutdown();



 }



 if (reportService != null && repository != null) {





 // Persist the pending write-ahead log into reports.json before the





 // storage executor is torn down, so the on-disk state is clean.





 reportService.flush();



 }



 if (offlineNotices != null) {





 offlineNotices.save();



 }



 if (watchlist != null) {





 watchlist.save();



 }



 if (auditStorage != null) {





 auditStorage.shutdown();



 }



 if (storage != null) {





 storage.shutdown();



 }

 }


// ---------------------------------------------------------------------

 // Lifecycle helpers

 // ---------------------------------------------------------------------


private void saveDefaultResources() {



 saveDefaultConfig();



 saveResourceIfMissing("lang/zh_CN.yml");



 saveResourceIfMissing("lang/en_US.yml");

 }


private void saveResourceIfMissing(String resourcePath) {



 Path target = getDataFolder().toPath().resolve(resourcePath);



 if (Files.isRegularFile(target)) {





 return;



 }



 try {





 saveResource(resourcePath, false);



 } catch (IllegalArgumentException exception) {





 getLogger().warning("Bundled resource " + resourcePath + " is missing: "







 + exception.getMessage());



 }

 }


/**


* Loads config.yml and the selected language. Called during startup and by


* /reports reload. Never touches report storage.


*/

 private void loadConfiguration() {



 reloadConfig();



 PluginConfig loaded = new ConfigLoader(this::warning).load(new BukkitConfigSource(getConfig()));



 this.config = loaded;



 loadMessages(loaded);

 }


/**


* Loads the language file for a validated config. Called on startup, on


* /reports reload and when an in-game edit switches the language.


*/

 private void loadMessages(PluginConfig loaded) {



 Path primary = getDataFolder().toPath().resolve("lang").resolve(loaded.language() + ".yml");



 Path fallback = getDataFolder().toPath().resolve("lang").resolve("en_US.yml");



 if (!Files.isRegularFile(primary)) {





 warning("Language file " + primary.getFileName() + " not found; using en_US.yml");





 primary = fallback;



 }



 this.messages = Messages.load(primary, fallback, this::warning);

 }


private void rebuildCoreServices() {



 PluginConfig current = config;



 this.validator = new InputValidator(current.validatorSettings());



 this.rateLimiter = new RateLimiter(current.rateLimitPolicy(), Clock.systemDefaultZone(),





 ZoneId.systemDefault());



 if (repository != null) {





 this.rateLimiter.seed(repository.all());



 }



 this.submissionService = new ReportSubmissionService(





 repository,





 rateLimiter,





 validator,





 current.categoryRegistry(),





 current.submissionSettings(),





 Clock.systemDefaultZone()



 );

 }


private void loadData() {



 storage.loadReports().whenComplete((loaded, throwable) -> {





 if (throwable != null) {







 warning("Failed to load reports: " + rootMessage(throwable));







 repository.seed(List.of());







 return;





 }





 repository.seed(loaded);





 rateLimiter.seed(loaded);





 getLogger().info("Loaded " + loaded.size() + " reports");



 });

 }


public void reloadAll() {



 PluginConfig previous = config;



 loadConfiguration();



 PluginConfig updated = config;



 if (!previous.storage().directory().equals(updated.storage().directory())) {





 warning("storage.directory changed; a server restart is required to move existing data");



 }



 rebuildCoreServices();



 applyEvidenceListeners();



 scheduleTasks();



 if (configEditor != null) {





 configEditor.load();



 }

 }


/**


* Applies an already validated config. Used by the in-game editor after it


* has confirmed the value; no file is read here, so it is safe on the main


* thread.


*/

 public void applyConfig(PluginConfig updated) {



 if (updated == null) {





 return;



 }



 PluginConfig previous = config;



 this.config = updated;



 if (previous == null || !previous.language().equals(updated.language())) {





 loadMessages(updated);



 }



 rebuildCoreServices();



 applyEvidenceListeners();



 scheduleTasks();

 }


private void registerPermissions() {



 permission("reportdesk.report", "Create player reports", PermissionDefault.TRUE);



 permission("reportdesk.admin", "Access the ReportDesk admin workflow", PermissionDefault.OP);



 permission("reportdesk.admin.close", "Close and reopen reports", PermissionDefault.OP);



 permission("reportdesk.admin.delete", "Delete and purge reports", PermissionDefault.OP);



 permission("reportdesk.notify", "Receive new report notifications", PermissionDefault.OP);



 permission("reportdesk.watch", "Follow players with the report watchlist", PermissionDefault.OP);



 permission("reportdesk.evidence", "Read captured evidence", PermissionDefault.OP);



 permission("reportdesk.reload", "Reload ReportDesk", PermissionDefault.OP);



 permission("reportdesk.config", "View and edit ReportDesk settings in game", PermissionDefault.OP);



 permission("reportdesk.bypass.limits", "Bypass report limits", PermissionDefault.FALSE);



 permission("reportdesk.bypass.evidence", "Excluded from evidence capture", PermissionDefault.FALSE);

 }


private void permission(String name, String description, PermissionDefault defaultValue) {



 if (Bukkit.getPluginManager().getPermission(name) != null) {





 return;



 }



 Bukkit.getPluginManager().addPermission(new Permission(name, description, defaultValue));

 }


private void registerCommands() {



 ReportCommand reportCommand = new ReportCommand(this);



 ReportsCommand reportsCommand = new ReportsCommand(this);



 com.reportdesk.command.CommandRegistrar.register(this, "report", reportCommand, reportCommand,





 List.of("rdreport", "jb"), "Open the report desk or report a player.",





 "/report [player] [category] [details...]");



 com.reportdesk.command.CommandRegistrar.register(this, "reports", reportsCommand, reportsCommand,





 List.of("reportdesk", "rd"), "Manage player reports.", "/reports help");




// Keep plugin.yml-declared aliases working even when they were not



 // installed by the descriptor, and set executors when they were.



 for (String alias : List.of("rdreport", "jb")) {





 PluginCommand command = com.reportdesk.command.CommandRegistrar.findCommand(this, alias);





 if (command != null && command.getExecutor() != reportCommand) {







 command.setExecutor(reportCommand);







 command.setTabCompleter(reportCommand);





 }



 }



 for (String alias : List.of("reportdesk", "rd")) {





 PluginCommand command = com.reportdesk.command.CommandRegistrar.findCommand(this, alias);





 if (command != null && command.getExecutor() != reportsCommand) {







 command.setExecutor(reportsCommand);







 command.setTabCompleter(reportsCommand);





 }



 }

 }


private void registerBaseListeners() {



 getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);



 getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);



 getServer().getPluginManager().registerEvents(new GuiClickListener(gui), this);

 }


/**


* Rebuilds the optional listeners from the current config. When an


* enhancement is disabled, no listener for it is registered at all.


*/

 private void applyEvidenceListeners() {



 for (Listener listener : new ArrayList<>(evidenceListeners)) {





 HandlerList.unregisterAll(listener);



 }



 evidenceListeners.clear();




if (!config.evidence().enabled()) {





 return;



 }



 if (config.evidence().blockBreak().enabled()) {





 registerEvidenceListener(new BlockBreakEvidenceListener(this));



 }



 if (config.evidence().movement().enabled()) {





 registerEvidenceListener(new MovementEvidenceListener(this));



 }



 if (config.evidence().captureChatLog()) {





 registerEvidenceListener(new ChatEvidenceListener(this));



 }

 }


private void registerEvidenceListener(Listener listener) {



 getServer().getPluginManager().registerEvents(listener, this);



 evidenceListeners.add(listener);

 }


private void scheduleTasks() {



 cancelTasks();




chatExpiryTask = Bukkit.getScheduler().runTaskTimer(this,





 () -> chatInput.expire(), 20L, 20L);




int maintenanceMinutes = config.reports().maintenanceIntervalMinutes();



 long maintenanceTicks = Math.max(20L * 60L, maintenanceMinutes * 60L * 20L);



 maintenanceTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {





 try {







 maintenance.runOnce().whenComplete((result, throwable) -> {









 if (throwable != null) {











 warning("Maintenance task failed: " + rootMessage(throwable));









 } else if (result != null && result.changed()) {











 getLogger().info("Maintenance: auto-closed " + result.autoClosed()













 + ", purged reports " + result.purgedReports()













 + ", evidence " + result.purgedEvidence()













 + ", audit files " + result.purgedAudit());









 }







 });





 } catch (RuntimeException exception) {







 warning("Maintenance task failed: " + exception.getMessage());





 }



 }, maintenanceTicks, maintenanceTicks);




PluginConfig.Priority priority = config.reports().priority();



 if (priority.enabled() && priority.overdueAlertEnabled()) {





 long overdueTicks = Math.max(20L * 60L, priority.overdueCheckMinutes() * 60L * 20L);





 overdueTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {







 try {









 maintenance.alertOverdue().whenComplete((count, throwable) -> {











 if (throwable != null) {













 warning("Overdue check failed: " + rootMessage(throwable));











 } else if (count != null && count > 0) {













 getLogger().info("Sent overdue alerts for " + count + " report(s)");











 }









 });







 } catch (RuntimeException exception) {









 warning("Overdue check failed: " + exception.getMessage());







 }





 }, overdueTicks, overdueTicks);



 }




PluginConfig.AutoDetect auto = config.evidence().autoDetect();



 if (config.evidence().enabled() && auto.enabled() && config.evidence().blockBreak().enabled()) {





 long autoTicks = Math.max(20L, auto.checkIntervalSeconds() * 20L);





 autoDetectTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {







 try {









 autoDetect.check();







 } catch (RuntimeException exception) {









 warning("Auto-detection task failed: " + exception.getMessage());







 }





 }, autoTicks, autoTicks);



 }

 }


private void cancelTasks() {



 cancelTask(chatExpiryTask);



 cancelTask(maintenanceTask);



 cancelTask(autoDetectTask);



 cancelTask(overdueTask);



 chatExpiryTask = null;



 maintenanceTask = null;



 autoDetectTask = null;



 overdueTask = null;

 }


private void cancelTask(BukkitTask task) {



 if (task != null) {





 task.cancel();



 }

 }


// ---------------------------------------------------------------------

 // Shared services / helpers used by commands, GUI and services

 // ---------------------------------------------------------------------


public PluginConfig config() {



 return config;

 }


public Messages messages() {



 return messages;

 }


public InputValidator validator() {



 return validator;

 }


public RateLimiter rateLimiter() {



 return rateLimiter;

 }


public ReportRepository repository() {



 return repository;

 }


public ReportSubmissionService submissionService() {



 return submissionService;

 }


public FileStorage storage() {



 return storage;

 }


public AuditStorage auditStorage() {



 return auditStorage;

 }


public EvidenceService evidence() {



 return evidence;

 }


public NotificationService notification() {



 return notification;

 }


public PendingNoticeService offlineNotices() {



 return offlineNotices;

 }


public WatchlistService watchlist() {



 return watchlist;

 }


public ConfigEditService configEditor() {



 return configEditor;

 }


public WebhookService webhook() {



 return webhook;

 }


public PunishmentService punishment() {



 return punishment;

 }


public ChatInputService chatInput() {



 return chatInput;

 }


public ReportService reportService() {



 return reportService;

 }


public AutoDetectService autoDetect() {



 return autoDetect;

 }


public MaintenanceService maintenance() {



 return maintenance;

 }


public GuiService gui() {



 return gui;

 }


public ReportViewService view() {



 return view;

 }


public CategoryRegistry categories() {



 return config.categoryRegistry();

 }


public String categoryDisplay(String id) {



 return config.category(id)





 .map(category -> messages.plain(messages.render(category.display(), Map.of())))





 .filter(value -> !value.isBlank())





 .orElse(id == null ? "" : id);

 }


/**


* Appends an audit entry asynchronously. Never performs file IO on the


* calling thread.


*/

 public void audit(String action, Player staff, long reportId, String detail) {



 if (config == null || !config.audit().enabled() || auditStorage == null) {





 return;



 }



 UUID actorId = staff == null ? new UUID(0L, 0L) : staff.getUniqueId();



 String actorName = staff == null





 ? messages.plain(MessageKeys.ADMIN_CONSOLE_ACTOR, Map.of())





 : staff.getName();



 AuditEntry entry = new AuditEntry(System.currentTimeMillis(), actorId, actorName,





 action, reportId, detail);



 auditStorage.append(entry).whenComplete((ignored, throwable) -> {





 if (throwable != null) {







 warning("Failed to write audit entry: " + rootMessage(throwable));





 }



 });

 }


public void audit(String action, Player staff, Report report, String detail) {



 audit(action, staff, report == null ? 0L : report.id(), detail);

 }


public void warning(String message) {



 getLogger().warning(message);

 }


private static String rootMessage(Throwable throwable) {



 Throwable current = throwable;



 while (current instanceof CompletionException && current.getCause() != null) {





 current = current.getCause();



 }



 return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();

 }


/**


* Utility for command/GUI code that needs a stable order of placeholders.


*/

 public static Map<String, String> placeholders(String... keyValues) {



 Map<String, String> map = new LinkedHashMap<>();



 for (int index = 0; index + 1 < keyValues.length; index += 2) {





 map.put(keyValues[index], keyValues[index + 1] == null ? "" : keyValues[index + 1]);



 }



 return map;

 } }
