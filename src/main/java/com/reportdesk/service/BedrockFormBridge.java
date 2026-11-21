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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Reflective bridge to the GeyserMC Cumulus form stack.
 *
 * <p>ReportDesk is a pure Java Paper plugin that cannot declare a compile-time dependency on
 * Geyser, Floodgate, or Cumulus (servers without Geyser must still load it cleanly). All access
 * to the Bedrock form stack happens through reflection.
 *
 * <p><b>Classloader isolation</b>: Bukkit loads each plugin with its own isolated classloader, so
 * Cumulus classes are only visible through the Geyser/Floodgate plugin's own loader. This bridge
 * therefore discovers the backend plugin via {@link org.bukkit.plugin.PluginManager} and resolves
 * every Cumulus/Geyser type through that plugin's classloader rather than ReportDesk's own.
 *
 * <p><b>Backend detection order</b> (first match wins):
 * <ol>
 *   <li>Geyser-Spigot — the plugin instance itself implements {@code GeyserApi}</li>
 *   <li>Floodgate — accessed via {@code FloodgateApi.getInstance()}</li>
 * </ol>
 *
 * <p>When neither backend is present the bridge reports unavailable and all calls are no-ops.
 *
 * <p><b>Threading note</b>: Cumulus form callbacks are delivered on the Geyser/Floodgate worker
 * thread, not the Bukkit main thread. Callers schedule Bukkit work back via
 * {@code Bukkit.getScheduler()}.
 */
public final class BedrockFormBridge {

  /** Backend API instance: the Geyser plugin, or a {@code FloodgateApi} instance. */
  private final Object api;

  /** Classloader of the backend plugin; used to resolve Cumulus types. */
  private final ClassLoader apiLoader;

  private final Method sendFormMethod;

  private final Method isBedrockMethod;

  private final Method customFormBuilderMethod;

  private final Method simpleFormBuilderMethod;

  private final String backendName;

  public BedrockFormBridge() {

    Object apiInstance = null;
    ClassLoader loader = null;
    Method customForm = null;
    Method simpleForm = null;
    Method sendForm = null;
    Method isBedrock = null;
    String backend = "none";

    // --- Geyser backend: the Spigot plugin instance is itself a GeyserApi ---
    Plugin geyser = findPlugin("geyser");
    if (geyser != null) {

      try {
        loader = geyser.getClass().getClassLoader();
        apiInstance = geyser;
        customForm = resolveBuilder(loader, "org.geysermc.cumulus.form.CustomForm");
        simpleForm = resolveBuilder(loader, "org.geysermc.cumulus.form.SimpleForm");
        sendForm = findSendForm(apiInstance);
        isBedrock = findBooleanMethod(apiInstance, "isBedrockPlayer");
        backend = "geyser";
      } catch (Throwable exception) {
        // Geyser present but its API surface could not be fully loaded
        // (e.g. ViaVersion not installed). Degrade gracefully.
        backend = "geyser-incomplete";
      }
    } else {
      // --- Floodgate backend: accessed through FloodgateApi.getInstance() ---
      Plugin floodgate = findPlugin("floodgate");
      if (floodgate != null) {

        ClassLoader fl = floodgate.getClass().getClassLoader();
        Object floodgateApi = loadFloodgateApi(fl);
        if (floodgateApi != null) {
          loader = fl;
          apiInstance = floodgateApi;
          customForm = resolveBuilder(fl, "org.geysermc.cumulus.form.CustomForm");
          simpleForm = resolveBuilder(fl, "org.geysermc.cumulus.form.SimpleForm");
          sendForm = findSendForm(apiInstance);
          isBedrock = findBooleanMethod(apiInstance, "isFloodgatePlayer");
          backend = "floodgate";
        }
      }
    }

    this.api = apiInstance;
    this.apiLoader = loader;
    this.sendFormMethod = sendForm;
    this.isBedrockMethod = isBedrock;
    this.customFormBuilderMethod = customForm;
    this.simpleFormBuilderMethod = simpleForm;
    this.backendName = backend;
  }

  // -------------------------------------------------------------------
  // Public queries
  // -------------------------------------------------------------------

  /** Whether the Bedrock form stack is available at runtime (cached at construction). */
  public boolean available() {

    return api != null
        && apiLoader != null
        && sendFormMethod != null
        && isBedrockMethod != null
        && (customFormBuilderMethod != null || simpleFormBuilderMethod != null);
  }

  /**
   * Human-readable backend name: "floodgate", "geyser", "geyser-incomplete", or "none".
   * "geyser-incomplete" means the Geyser plugin is present but its API surface could not
   * be fully loaded (e.g. an optional dependency such as ViaVersion is missing).
   */
  public String backend() {

    return backendName;
  }

  /**
   * Whether the given player UUID is a Bedrock client.
   *
   * @return true when the bridge is available and the player is Bedrock; false otherwise
   */
  public boolean isBedrockPlayer(UUID uuid) {

    if (!available() || uuid == null) {

      return false;
    }

    try {

      return Boolean.TRUE.equals(isBedrockMethod.invoke(api, uuid));

    } catch (Throwable exception) {

      return false;
    }
  }

  // -------------------------------------------------------------------
  // Callbacks
  // -------------------------------------------------------------------

  /** Callback for a custom-form submission, delivered on the Cumulus worker thread. */
  public interface FormSubmitCallback {

    /**
     * @param target        text entered for the target input; empty when absent
     * @param categoryIndex 0-based dropdown index; {@code -1} when no dropdown was included
     * @param detail        text entered for the detail input; empty when absent
     */
    void onValidSubmit(String target, int categoryIndex, String detail);
  }

  /** Callback for a simple-form button click, delivered on the Cumulus worker thread. */
  public interface SimpleFormCallback {

    /**
     * @param buttonIndex 0-based index of the clicked button
     */
    void onButtonClicked(int buttonIndex);
  }

  // -------------------------------------------------------------------
  // Form building + sending
  // -------------------------------------------------------------------

  /**
   * Builds and sends a Cumulus {@code CustomForm} with a target input, an optional category
   * dropdown and a detail input to the given Bedrock player.
   *
   * <p>Field order: target input, category dropdown, detail input. The valid-result handler
   * reads the response through the shared JDK {@link Iterable} interface and invokes
   * {@code onValid} on the Cumulus worker thread; the caller schedules Bukkit work back to the
   * main thread.
   *
   * @return true when the form was sent; false when the bridge is unavailable or send failed
   */
  public boolean sendCustomForm(
      UUID targetUuid,
      String title,
      String contentLabel,
      String targetLabel,
      String targetPlaceholder,
      String targetDefault,
      String categoryLabel,
      List<String> categoryOptions,
      String detailLabel,
      String detailPlaceholder,
      FormSubmitCallback onValid,
      Runnable onClosedOrInvalid) {

    if (!available() || targetUuid == null || customFormBuilderMethod == null) {

      return false;
    }

    Object builder = invokeStatic(customFormBuilderMethod);

    if (builder == null) {

      return false;
    }

    invokeOn(builder, "title", new Class<?>[] {String.class}, new Object[] {title});

    if (contentLabel != null && !contentLabel.isBlank()) {

      invokeOn(builder, "label", new Class<?>[] {String.class}, new Object[] {contentLabel});
    }

    if (targetDefault != null && !targetDefault.isBlank()) {

      invokeOn(
          builder,
          "input",
          new Class<?>[] {String.class, String.class, String.class},
          new Object[] {
              targetLabel, targetPlaceholder == null ? "" : targetPlaceholder, targetDefault});
    } else {

      invokeOn(
          builder,
          "input",
          new Class<?>[] {String.class, String.class},
          new Object[] {
              targetLabel, targetPlaceholder == null ? "" : targetPlaceholder});
    }

    if (categoryOptions != null && !categoryOptions.isEmpty()) {

      invokeOn(
          builder,
          "dropdown",
          new Class<?>[] {String.class, List.class, int.class},
          new Object[] {categoryLabel, categoryOptions, 0});
    }

    invokeOn(
        builder,
        "input",
        new Class<?>[] {String.class, String.class},
        new Object[] {detailLabel, detailPlaceholder == null ? "" : detailPlaceholder});

    if (onValid != null) {

      invokeOn(
          builder,
          "validResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {
              createConsumerProxy(response -> readCustomResponse(response, onValid))});
    }

    if (onClosedOrInvalid != null) {

      invokeOn(
          builder,
          "closedOrInvalidResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {createConsumerProxy(ignored -> onClosedOrInvalid.run())});
    }

    return sendForm(targetUuid, builder);
  }

  /**
   * Builds and sends a Cumulus {@code SimpleForm} with a list of buttons to the given Bedrock
   * player.
   *
   * @return true when the form was sent; false when unavailable or send failed
   */
  public boolean sendSimpleForm(
      UUID targetUuid, String title, String content, List<String> buttons,
      SimpleFormCallback onValid) {

    if (!available() || targetUuid == null || simpleFormBuilderMethod == null) {

      return false;
    }

    Object builder = invokeStatic(simpleFormBuilderMethod);

    if (builder == null) {

      return false;
    }

    invokeOn(builder, "title", new Class<?>[] {String.class}, new Object[] {title});

    if (content != null && !content.isBlank()) {

      invokeOn(builder, "content", new Class<?>[] {String.class}, new Object[] {content});
    }

    if (buttons != null) {

      for (String button : buttons) {

        invokeOn(builder, "button", new Class<?>[] {String.class}, new Object[] {button});
      }
    }

    if (onValid != null) {

      invokeOn(
          builder,
          "validResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {
              createConsumerProxy(response -> {
                if (response instanceof Iterable<?> iterable) {
                  for (Object value : iterable) {
                    if (value instanceof Number n) {
                      onValid.onButtonClicked(n.intValue());
                      break;
                    }
                  }
                }
              })});
    }

    return sendForm(targetUuid, builder);
  }

  // -------------------------------------------------------------------
  // Backend discovery helpers
  // -------------------------------------------------------------------

  /** Returns the first installed Bukkit plugin whose name contains the hint. */
  private static Plugin findPlugin(String nameHint) {

    for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {

      if (plugin.getName().toLowerCase(Locale.ROOT).contains(nameHint)) {

        return plugin;
      }
    }

    return null;
  }

  /** Resolves {@code CustomForm.builder()} / {@code SimpleForm.builder()} through the backend loader. */
  private static Method resolveBuilder(ClassLoader loader, String fqcn) {

    if (loader == null) {

      return null;
    }

    try {

      return Class.forName(fqcn, true, loader).getMethod("builder");

    } catch (Throwable exception) {

      return null;
    }
  }

  /** Calls {@code FloodgateApi.getInstance()} reflectively through the Floodgate loader. */
  private static Object loadFloodgateApi(ClassLoader loader) {

    if (loader == null) {

      return null;
    }

    try {

      Method getInstance =
          Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader)
              .getMethod("getInstance");

      return getInstance.invoke(null);

    } catch (Throwable exception) {

      return null;
    }
  }

  /**
   * Finds the {@code sendForm(UUID, ...)} overload, preferring the {@code FormBuilder} variant
   * because the built builder is passed directly (Cumulus builds it internally).
   */
  private static Method findSendForm(Object apiInstance) {

    if (apiInstance == null) {

      return null;
    }

    try {

      Method fallback = null;

      for (Method method : apiInstance.getClass().getMethods()) {

        if (!method.getName().equals("sendForm") || method.getParameterCount() != 2) {

          continue;
        }

        if (method.getParameterTypes()[0] != UUID.class) {

          continue;
        }

        if (method.getParameterTypes()[1].getSimpleName().contains("FormBuilder")) {

          return method;
        }

        if (fallback == null) {

          fallback = method;
        }
      }

      return fallback;

    } catch (Throwable exception) {
      // getMethods() can throw NoClassDefFoundError when an optional
      // dependency (e.g. ViaVersion) is not on the classpath; degrade
      // to no-ops rather than crash the plugin.
      return null;
    }
  }

  /** Finds a {@code <name>(UUID): boolean} method on the backend API. */
  private static Method findBooleanMethod(Object apiInstance, String name) {

    if (apiInstance == null) {

      return null;
    }

    try {

      for (Method method : apiInstance.getClass().getMethods()) {

        if (method.getName().equals(name)
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == UUID.class
            && method.getReturnType() == boolean.class) {

          return method;
        }
      }

    } catch (Throwable exception) {
      // Same NoClassDefFoundError guard as findSendForm.
    }

    return null;
  }

  // -------------------------------------------------------------------
  // Reflection plumbing
  // -------------------------------------------------------------------

  private boolean sendForm(UUID uuid, Object builder) {

    if (api == null || sendFormMethod == null) {

      return false;
    }

    try {

      Object result = sendFormMethod.invoke(api, uuid, builder);

      return !(result instanceof Boolean b) || b;

    } catch (Throwable exception) {

      return false;
    }
  }

  /**
   * Reads a CustomFormResponse through the shared JDK {@link Iterable} interface (its concrete
   * Cumulus type is not visible to ReportDesk). The response yields values in the order the
   * components were added: target input, optional dropdown, detail input.
   */
  private void readCustomResponse(Object response, FormSubmitCallback onValid) {

    if (!(response instanceof Iterable<?> iterable)) {

      onValid.onValidSubmit("", -1, "");

      return;
    }

    String target = "";

    String detail = "";

    int categoryIndex = -1;

    int stringsSeen = 0;

    for (Object value : iterable) {

      if (value instanceof String s) {

        if (stringsSeen == 0) {

          target = s;

        } else {

          detail = s;
        }

        stringsSeen++;

      } else if (value instanceof Number n) {

        categoryIndex = n.intValue();
      }
    }

    onValid.onValidSubmit(target, categoryIndex, detail);
  }

  /**
   * Builds a {@link Consumer} proxy so the Cumulus handler (a generic
   * {@code Consumer<Response>}) can be satisfied without a compile-time reference to the
   * Cumulus response type. The proxy implements the shared JDK {@code Consumer} interface.
   */
  private Object createConsumerProxy(Consumer<Object> delegate) {

    try {

      InvocationHandler handler =
          (proxy, method, args) -> {
            if ("accept".equals(method.getName()) && args != null && args.length == 1) {

              delegate.accept(args[0]);
            }

            return null;
          };

      return Proxy.newProxyInstance(
          BedrockFormBridge.class.getClassLoader(), new Class<?>[] {Consumer.class}, handler);

    } catch (Throwable exception) {

      return null;
    }
  }

  /** Invokes a static no-arg method (e.g. {@code CustomForm.builder()}). */
  private static Object invokeStatic(Method method) {

    if (method == null) {

      return null;
    }

    try {

      return method.invoke(null);

    } catch (Throwable ignored) {

      return null;
    }
  }

  /** Invokes a parameterized method on an instance, returning null silently on failure. */
  private static Object invokeOn(
      Object target, String methodName, Class<?>[] paramTypes, Object[] args) {

    if (target == null) {

      return null;
    }

    Method method = null;

    try {

      method = target.getClass().getMethod(methodName, paramTypes);

    } catch (Throwable ignored) {

      method = null;
    }

    if (method == null) {

      for (Method candidate : target.getClass().getMethods()) {

        if (candidate.getName().equals(methodName)
            && candidate.getParameterCount() == paramTypes.length) {

          method = candidate;

          break;
        }
      }
    }

    if (method == null) {

      return null;
    }

    try {

      return method.invoke(target, args);

    } catch (Throwable ignored) {

      return null;
    }
  }
}
