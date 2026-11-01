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

import com.reportdesk.ReportDeskPlugin;
import com.reportdesk.config.ConfigKey;
import com.reportdesk.config.ConfigLoader;
import com.reportdesk.config.MapConfigSource;
import com.reportdesk.config.PluginConfig;
import com.reportdesk.config.YamlDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;

/**
 * In-game config editor.
 *
 * <p>The current config.yml text is kept in memory, so validation happens on the main thread
 * without any file IO. A successful edit is applied to the running server immediately and then
 * written atomically on the storage executor. If the write fails the previous config is restored.
 *
 * <p>Validation reuses {@link ConfigLoader}: a value that would be clamped, replaced or accompanied
 * by a new warning is rejected instead of being written to disk silently.
 */
public final class ConfigEditService {

  public enum Status {
    OK,

    DISABLED,

    LOADING,

    BUSY,

    UNKNOWN,

    INVALID,

    REJECTED,

    WRITE_FAILED
  }

  public record Result(
      Status status,
      String key,
      String oldValue,
      String newValue,
      String reason,
      boolean reset,
      boolean restartRequired) {

    public boolean ok() {

      return status == Status.OK;
    }

    static Result changed(
        String key, String oldValue, String newValue, boolean reset, boolean restartRequired) {

      return new Result(Status.OK, key, oldValue, newValue, "", reset, restartRequired);
    }

    static Result failed(Status status, String key, String reason) {

      return new Result(status, key, "", "", reason == null ? "" : reason, false, false);
    }
  }

  private final ReportDeskPlugin plugin;

  private final PluginConfig defaults;

  private final AtomicBoolean ready = new AtomicBoolean(false);

  private final AtomicBoolean saving = new AtomicBoolean(false);

  private volatile String document = "";

  private volatile Map<String, Object> values = Map.of();

  public ConfigEditService(ReportDeskPlugin plugin) {

    this.plugin = plugin;

    this.defaults = new ConfigLoader(ignored -> {}).load(new MapConfigSource(Map.of()));
  }

  /**
   * Loads config.yml for editing. Called at startup and after /reports reload
   *
   * <p>so external file edits are visible to the editor as well.
   */
  public void load() {

    ready.set(false);

    plugin
        .storage()
        .readConfigText()
        .whenComplete(
            (text, error) -> {
              if (error != null) {

                plugin.warning(
                    "Failed to read config.yml for the online editor: " + rootMessage(error));

                return;
              }

              if (text == null || text.isBlank()) {

                plugin.warning("config.yml is empty; online editing is disabled until it exists");

                return;
              }

              try {

                Map<String, Object> parsed = YamlDocument.parse(text);

                document = text;

                values = parsed;

                ready.set(true);

              } catch (RuntimeException exception) {

                plugin.warning("config.yml cannot be edited in game: " + exception.getMessage());
              }
            });
  }

  public boolean ready() {

    return ready.get();
  }

  public PluginConfig defaults() {

    return defaults;
  }

  public CompletableFuture<Result> set(ConfigKey key, String raw) {

    if (key == null) {

      return CompletableFuture.completedFuture(Result.failed(Status.UNKNOWN, "", ""));
    }

    if (!ready.get()) {

      return CompletableFuture.completedFuture(Result.failed(Status.LOADING, key.path(), ""));
    }

    if (!allowed(key)) {

      return CompletableFuture.completedFuture(Result.failed(Status.DISABLED, key.path(), ""));
    }

    int maxLength = plugin.config().configEditor().maxValueLength();

    if (raw != null && raw.length() > maxLength) {

      return CompletableFuture.completedFuture(
          Result.failed(
              Status.INVALID, key.path(), "value is longer than " + maxLength + " characters"));
    }

    Object parsed;

    try {

      parsed = key.parse(raw);

    } catch (IllegalArgumentException exception) {

      return CompletableFuture.completedFuture(
          Result.failed(Status.INVALID, key.path(), exception.getMessage()));
    }

    return apply(key, parsed, false);
  }

  public CompletableFuture<Result> reset(ConfigKey key) {

    if (key == null) {

      return CompletableFuture.completedFuture(Result.failed(Status.UNKNOWN, "", ""));
    }

    if (!ready.get()) {

      return CompletableFuture.completedFuture(Result.failed(Status.LOADING, key.path(), ""));
    }

    if (!allowed(key)) {

      return CompletableFuture.completedFuture(Result.failed(Status.DISABLED, key.path(), ""));
    }

    Object fallback = key.current(defaults);

    if (fallback == null) {

      return CompletableFuture.completedFuture(
          Result.failed(Status.INVALID, key.path(), "no default value"));
    }

    return apply(key, fallback, true);
  }

  private CompletableFuture<Result> apply(ConfigKey key, Object parsed, boolean reset) {

    if (!saving.compareAndSet(false, true)) {

      return CompletableFuture.completedFuture(Result.failed(Status.BUSY, key.path(), ""));
    }

    PluginConfig previous = plugin.config();

    String oldText = document;

    try {

      Map<String, Object> candidate = YamlDocument.deepCopy(values);

      YamlDocument.setValue(candidate, key.path(), parsed);

      List<String> baselineWarnings = new ArrayList<>();

      new ConfigLoader(baselineWarnings::add).load(new MapConfigSource(values));

      List<String> warnings = new ArrayList<>();

      PluginConfig updated = new ConfigLoader(warnings::add).load(new MapConfigSource(candidate));

      List<String> introduced =
          warnings.stream().filter(warning -> !baselineWarnings.contains(warning)).toList();

      Object effective = key.current(updated);

      if (!key.matches(parsed, effective) || !introduced.isEmpty()) {

        saving.set(false);

        return CompletableFuture.completedFuture(
            Result.failed(
                Status.REJECTED, key.path(), rejectionReason(warnings, introduced, key.path())));
      }

      String newText = YamlDocument.set(oldText, key.path(), effective);

      Map<String, Object> normalized = YamlDocument.parse(newText);

      String oldDisplay = key.display(key.current(previous));

      String newDisplay = key.display(effective);

      // Apply before the async write so commands see the new value at once.

      plugin.applyConfig(updated);

      PluginConfig.ConfigEditor editor = updated.configEditor();

      CompletableFuture<Void> write =
          plugin.storage().writeConfigText(newText, editor.backupOnSave(), editor.maxBackups());

      return write.handle(
          (ignored, error) -> {
            saving.set(false);

            if (error != null) {

              String reason = rootMessage(error);

              revert(previous);

              return Result.failed(Status.WRITE_FAILED, key.path(), reason);
            }

            document = newText;

            values = normalized;

            return Result.changed(key.path(), oldDisplay, newDisplay, reset, key.restartRequired());
          });

    } catch (RuntimeException exception) {

      saving.set(false);

      return CompletableFuture.completedFuture(
          Result.failed(Status.INVALID, key.path(), rootMessage(exception)));
    }
  }

  /**
   * Only config-editor.* may be changed while the editor is disabled, so an
   *
   * <p>admin can always switch it back on without touching the file.
   */
  private boolean allowed(ConfigKey key) {

    return plugin.config().configEditor().enabled() || key.path().startsWith("config-editor.");
  }

  private void revert(PluginConfig previous) {

    if (previous == null || !plugin.isEnabled()) {

      return;
    }

    try {

      Bukkit.getScheduler().runTask(plugin, () -> plugin.applyConfig(previous));

    } catch (RuntimeException exception) {

      plugin.warning("Could not restore the previous config: " + exception.getMessage());
    }
  }

  private static String rejectionReason(
      List<String> warnings, List<String> introduced, String path) {

    for (String warning : introduced) {

      if (warning.contains(path)) {

        return clean(warning);
      }
    }

    if (!introduced.isEmpty()) {

      return clean(introduced.get(0));
    }

    for (String warning : warnings) {

      if (warning.contains(path)) {

        return clean(warning);
      }
    }

    return "value was clamped or replaced by validation";
  }

  private static String clean(String warning) {

    return warning == null ? "" : warning.replace("[config] ", "").trim();
  }

  private static String rootMessage(Throwable throwable) {

    Throwable current = throwable;

    while (current instanceof CompletionException && current.getCause() != null) {

      current = current.getCause();
    }

    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
