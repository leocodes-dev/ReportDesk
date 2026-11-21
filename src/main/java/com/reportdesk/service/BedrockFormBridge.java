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
import java.lang.reflect.Modifier;
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
 * Cumulus and Geyser types are only visible through the Geyser/Floodgate plugin's loader. This
 * bridge discovers that backend plugin via {@link org.bukkit.plugin.PluginManager} and resolves
 * every Cumulus/Geyser type through that plugin's classloader rather than ReportDesk's own.
 *
 * <p><b>Backend detection order</b> (first match wins):
 * <ol>
 *   <li>Geyser-Spigot — the live {@code GeyserApi} is obtained from
 *       {@code GeyserApi.api()} / {@code GeyserImpl.getInstance()}}</li>
 *   <li>Floodgate — {@code FloodgateApi.getInstance()}</li>
 * </ol>
 *
 * <p><b>Enable-ordering</b>: ReportDesk may enable before Geyser. The reflectable method handles
 * (Cumulus builders, {@code GeyserApiBase.sendForm}/{@code isBedrockPlayer}, the API factory)
 * are resolved once at construction, while the live API singleton is fetched lazily on every
 * form call via the factory method, so a not-yet-enabled Geyser simply yields null no-ops.
 *
 * <p><b>Threading note</b>: Cumulus form callbacks are delivered on the Geyser/Floodgate worker
 * thread, not the Bukkit main thread. Callers schedule Bukkit work back via
 * {@code Bukkit.getScheduler()}.
 */
public final class BedrockFormBridge {

  /** Classloader of the detected backend plugin; used to resolve Cumulus/Geyser types. */
  private final ClassLoader apiLoader;

  /** Static {@code CustomForm.builder()} handle; null when Cumulus is absent. */
  private final Method customFormBuilderMethod;

  /** Static {@code SimpleForm.builder()} handle; null when Cumulus is absent. */
  private final Method simpleFormBuilderMethod;

  /** Static factory yielding the live API: {@code GeyserApi.api()} or {@code FloodgateApi.getInstance()}. */
  private final Method apiFactoryMethod;

  /** {@code sendForm(UUID, FormBuilder)} resolved on the API base interface. */
  private final Method sendFormMethod;

  /** {@code isBedrockPlayer(UUID)}/{@code isFloodgatePlayer(UUID)} resolved on the API base interface. */
  private final Method isBedrockMethod;

  private final String backendName;

  public BedrockFormBridge() {

    String backend = "none";
    ClassLoader loader = null;
    Method customBuilder = null;
    Method simpleBuilder = null;
    Method factory = null;
    Method sendForm = null;
    Method isBedrock = null;

    // --- Geyser backend ---
    Plugin geyser = findPlugin("geyser");
    if (geyser != null) {

      loader = geyser.getClass().getClassLoader();
      customBuilder = resolveStaticBuilder(loader, "org.geysermc.cumulus.form.CustomForm");
      simpleBuilder = resolveStaticBuilder(loader, "org.geysermc.cumulus.form.SimpleForm");
      factory = resolveGeyserFactory(loader);
      Class<?> apiBase = loadClass(loader, "org.geysermc.api.GeyserApiBase");
      if (apiBase != null) {

        sendForm = findMethod(apiBase, "sendForm", 2, "UUID", "FormBuilder");
        isBedrock = findMethod(apiBase, "isBedrockPlayer", 1, "UUID");
      }
      backend = "geyser";
    }

    // --- Floodgate backend (only if Geyser was not found) ---
    if (geyser == null) {

      Plugin floodgate = findPlugin("floodgate");
      if (floodgate != null) {

        loader = floodgate.getClass().getClassLoader();
        customBuilder = resolveStaticBuilder(loader, "org.geysermc.cumulus.form.CustomForm");
        simpleBuilder = resolveStaticBuilder(loader, "org.geysermc.cumulus.form.SimpleForm");

        try {
          factory =
              loadClass(loader, "org.geysermc.floodgate.api.FloodgateApi").getMethod("getInstance");
        } catch (Throwable ignored) {

          factory = null;
        }

        Class<?> fgClass = loadClass(loader, "org.geysermc.floodgate.api.FloodgateApi");
        if (fgClass != null) {

          sendForm = findMethod(fgClass, "sendForm", 2, "UUID", "FormBuilder");
          isBedrock = findMethod(fgClass, "isFloodgatePlayer", 1, "UUID");
        }
        backend = "floodgate";
      }
    }

    this.apiLoader = loader;
    this.customFormBuilderMethod = customBuilder;
    this.simpleFormBuilderMethod = simpleBuilder;
    this.apiFactoryMethod = factory;
    this.sendFormMethod = sendForm;
    this.isBedrockMethod = isBedrock;
    this.backendName = backend;
  }

  // -------------------------------------------------------------------
  // Public queries
  // -------------------------------------------------------------------

  /**
   * Whether the backend is present and every reflectable handle resolved.
   *
   * <p>This can be true even before Geyser finishes enabling, because it only reflects on stable
   * types; the live singleton is fetched lazily on each call and a null singleton degrades to
   * no-ops.
   */
  public boolean available() {

    return apiLoader != null
        && apiFactoryMethod != null
        && sendFormMethod != null
        && isBedrockMethod != null
        && (customFormBuilderMethod != null || simpleFormBuilderMethod != null);
  }

  /**
   * Human-readable backend name: "floodgate", "geyser", or "none".
   */
  public String backend() {

    return backendName;
  }

  /**
   * Whether the given player UUID is a Bedrock client.
   *
   * @return true when a live backend reports the player as Bedrock; false otherwise
   */
  public boolean isBedrockPlayer(UUID uuid) {

    if (!available() || uuid == null) {

      return false;
    }

    Object api = resolveApi();

    if (api == null) {

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

  /**
   * Callback for a two-field CustomForm (dropdown + text input), delivered on the Cumulus
   * worker thread.
   *
   * @param dropdownIndex 0-based dropdown index, or {@code -1} when none was selected
   * @param text          the text entered in the input field; empty when blank
   */
  public interface FormValid2Callback {

    void onValid(int dropdownIndex, String text);
  }

  // -------------------------------------------------------------------
  // Form building + sending
  // -------------------------------------------------------------------

  /**
   * Builds and sends a Cumulus {@code CustomForm} with a target input, an optional category
   * dropdown and a detail input to the given Bedrock player.
   *
   * <p>Field order: target input, category dropdown, detail input. The valid-result handler reads
   * the response through the shared JDK {@link Iterable} interface and invokes
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
          new Object[] {createConsumerProxy(response -> readCustomResponse(response, onValid))});
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
              createConsumerProxy(
                  response -> {
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

  /**
   * Builds and sends a two-field Cumulus {@code CustomForm} (one dropdown + one text input)
   * to the given Bedrock player.
   *
   * <p>Component order: dropdown, then text input. The valid handler reads the response
   * through the shared JDK {@link Iterable} and invokes {@code onValid} with the dropdown
   * index and the input text on the Cumulus worker thread; the caller schedules Bukkit work
   * back to the main thread.
   *
   * @return true when the form was sent; false when the bridge is unavailable or send failed
   */
  public boolean sendCustomForm2(
      UUID targetUuid,
      String title,
      String contentLabel,
      String dropdownLabel,
      List<String> options,
      String inputLabel,
      String inputPlaceholder,
      FormValid2Callback onValid) {

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

    if (options != null && !options.isEmpty()) {

      invokeOn(
          builder,
          "dropdown",
          new Class<?>[] {String.class, List.class, int.class},
          new Object[] {dropdownLabel == null ? "" : dropdownLabel, options, 0});
    }

    invokeOn(
        builder,
        "input",
        new Class<?>[] {String.class, String.class},
        new Object[] {
            inputLabel == null ? "" : inputLabel,
            inputPlaceholder == null ? "" : inputPlaceholder});

    if (onValid != null) {

      invokeOn(
          builder,
          "validResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {createConsumerProxy(response -> readDropdownAndInput(response, onValid))});
    }

    return sendForm(targetUuid, builder);
  }

  // -------------------------------------------------------------------
  // Runtime API + reflection plumbing
  // -------------------------------------------------------------------

  /**
   * Fetches the live backend singleton through the factory method. Returns null when the backend
   * has not finished enabling yet, which the caller treats as a no-op.
   */
  private Object resolveApi() {

    try {

      return apiFactoryMethod.invoke(null);

    } catch (Throwable exception) {

      return null;
    }
  }

  private boolean sendForm(UUID uuid, Object builder) {

    if (apiFactoryMethod == null || sendFormMethod == null) {

      return false;
    }

    Object api = resolveApi();

    if (api == null) {

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
   * Cumulus type is not visible to ReportDesk). Values arrive in component order: target input,
   * optional dropdown, detail input.
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

  /** Resolves a static no-arg {@code builder()} factory on a Cumulus form class. */
  private static Method resolveStaticBuilder(ClassLoader loader, String fqcn) {

    Class<?> clazz = loadClass(loader, fqcn);

    if (clazz == null) {

      return null;
    }

    try {

      return clazz.getMethod("builder");

    } catch (Throwable ignored) {

      return null;
    }
  }

  /**
   * Resolves the static factory that yields the live Geyser API:
   *
   * <p>{@code GeyserApi.api()} preferred, falling back to {@code GeyserImpl.getInstance()}.
   */
  private static Method resolveGeyserFactory(ClassLoader loader) {

    Class<?> geyserApi = loadClass(loader, "org.geysermc.geyser.api.GeyserApi");

    if (geyserApi != null) {

      try {
        Method api = geyserApi.getMethod("api");
        if (Modifier.isStatic(api.getModifiers())) {

          return api;
        }
      } catch (Throwable ignored) {

        // fall through to GeyserImpl
      }
    }

    Class<?> geyserImpl = loadClass(loader, "org.geysermc.geyser.GeyserImpl");

    if (geyserImpl != null) {

      try {
        Method getInstance = geyserImpl.getMethod("getInstance");
        if (Modifier.isStatic(getInstance.getModifiers())) {

          return getInstance;
        }
      } catch (Throwable ignored) {

        // no usable factory
      }
    }

    return null;
  }

  /**
   * Finds a method by name, parameter count and parameter simple-name substring match. Used so the
   * exact generic Cumulus types do not need to be referenced at compile time.
   */
  private static Method findMethod(
      Class<?> clazz, String name, int paramCount, String... paramSimpleNameContains) {

    if (clazz == null) {

      return null;
    }

    try {

      for (Method method : clazz.getMethods()) {

        if (!method.getName().equals(name) || method.getParameterCount() != paramCount) {

          continue;
        }

        Class<?>[] types = method.getParameterTypes();

        if (types.length != paramSimpleNameContains.length) {

          continue;
        }

        boolean allMatch = true;

        for (int i = 0; i < types.length; i++) {

          String simple = types[i].getSimpleName();

          if (!simple.contains(paramSimpleNameContains[i])) {

            allMatch = false;

            break;
          }
        }

        if (allMatch) {

          return method;
        }
      }

    } catch (Throwable ignored) {
      // getMethods() may throw when an optional dependency class is missing; degrade.
    }

    return null;
  }

  /** Loads a class through the backend loader, returning null when absent. */
  private static Class<?> loadClass(ClassLoader loader, String fqcn) {

    if (loader == null) {

      return null;
    }

    try {

      return Class.forName(fqcn, true, loader);

    } catch (Throwable ignored) {

      return null;
    }
  }

  /**
   * Builds a {@link Consumer} proxy so the Cumulus handler (a generic
   * {@code Consumer<Response>}) can be satisfied without a compile-time reference to the Cumulus
   * response type. The proxy implements the shared JDK {@code Consumer} interface.
   */
  /**
   * Reads a two-field (dropdown + text) CustomForm response through the shared JDK
   * {@link Iterable} interface. Values arrive in component order: dropdown value, input text.
   */
  private void readDropdownAndInput(Object response, FormValid2Callback onValid) {

    if (!(response instanceof Iterable<?> iterable)) {

      onValid.onValid(-1, "");

      return;
    }

    int dropdownIndex = -1;

    String text = "";

    int index = 0;

    for (Object value : iterable) {

      if (index == 0 && value instanceof Number n) {

        dropdownIndex = n.intValue();

      } else if (value instanceof String s) {

        text = s;
      }

      index++;
    }

    onValid.onValid(dropdownIndex, text);
  }

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

      try {
        for (Method candidate : target.getClass().getMethods()) {

          if (candidate.getName().equals(methodName)
              && candidate.getParameterCount() == paramTypes.length) {

            method = candidate;

            break;
          }
        }
      } catch (Throwable ignored) {
        // getMethods() may throw for optional-dependency classes; skip the fuzzy match.
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
