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
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Reflective bridge to the GeyserMC Cumulus form library.
 *
 * <p>ReportDesk is a pure Java Paper plugin targeting {@code api-version 1.20}. It cannot declare a
 * compile-time dependency on Geyser, Floodgate, or Cumulus, because servers without Geyser would
 * fail to load the plugin. All interactions with the Bedrock form stack happen through reflection.
 *
 * <p><b>Backend detection order</b> (first match wins):
 * <ol>
 *   <li>Floodgate API — {@code org.geysermc.floodgate.api.FloodgateApi} — the most common
 *       cross-play setup on Paper (Floodgate provides Bedrock login without a Java account)</li>
 *   <li>Geyser API — {@code org.geysermc.geyser.api.GeyserApi} — standalone Geyser without
 *       Floodgate</li>
 * </ol>
 *
 * <p>When neither backend is found the bridge reports unavailable and all form calls are no-ops.
 *
 * <p><b>Threading note</b>: Cumulus form callbacks are delivered on the Geyser/Floodgate worker
 * thread, not the Bukkit main thread. Callers must schedule Bukkit work back via
 * {@code Bukkit.getScheduler()}.
 */
public final class BedrockFormBridge {

  // Cached API instance and method handles (resolved once at construction)
  private final Object api;
  private final Method sendFormMethod;
  private final Method isBedrockMethod;
  private final Method customFormBuilderMethod;
  private final Class<?> consumerClass;
  private final String backendName;

  /**
   * Probes the runtime classpath for Floodgate / Geyser / Cumulus.
   *
   * <p>Results are cached; calling this on an already-constructed instance is unnecessary.
   */
  public BedrockFormBridge() {

    this.consumerClass = tryLoad("java.util.function.Consumer");

    // Resolve Cumulus CustomForm static builder
    Class<?> customFormClass = tryLoad("org.geysermc.cumulus.form.CustomForm");
    this.customFormBuilderMethod =
        customFormClass != null ? safeGetMethod(customFormClass, "builder") : null;

    Object resolvedApi = null;
    Method resolvedSendForm = null;
    Method resolvedIsBedrock = null;
    String resolvedBackend = "none";

    // --- Try Floodgate ---
    Object fg = loadFloodgateApi();
    if (fg != null && this.customFormBuilderMethod != null) {

      Method sf = findSendForm(fg);
      Method ib = findIsBedrock(fg, "isFloodgatePlayer");
      if (sf != null && ib != null) {

        resolvedApi = fg;
        resolvedSendForm = sf;
        resolvedIsBedrock = ib;
        resolvedBackend = "floodgate";
      }
    }

    // --- Try standalone Geyser ---
    if (resolvedApi == null) {

      Object gs = loadGeyserApi();
      if (gs != null && this.customFormBuilderMethod != null) {

        Method sf = findSendForm(gs);
        Method ib = findIsBedrock(gs, "isBedrockPlayer");
        if (sf != null && ib != null) {

          resolvedApi = gs;
          resolvedSendForm = sf;
          resolvedIsBedrock = ib;
          resolvedBackend = "geyser";
        }
      }
    }

    this.api = resolvedApi;
    this.sendFormMethod = resolvedSendForm;
    this.isBedrockMethod = resolvedIsBedrock;
    this.backendName = resolvedBackend;
  }

  // -------------------------------------------------------------------
  // Public queries
  // -------------------------------------------------------------------

  /**
   * Whether the Bedrock form stack (Cumulus + Geyser/Floodgate) is available at runtime.
   *
   * <p>Cached result; does not re-probe the classpath on every call.
   */
  public boolean available() {

    return api != null && sendFormMethod != null && isBedrockMethod != null;
  }

  /** Human-readable backend name: "floodgate", "geyser", or "none". */
  public String backend() {

    return backendName;
  }

  /**
   * Checks whether the given player UUID is a Bedrock client.
   *
   * @return true when the bridge is available and the player is Bedrock; false otherwise
   */
  public boolean isBedrockPlayer(UUID uuid) {

    if (!available() || uuid == null) {

      return false;
    }

    try {

      Object result = isBedrockMethod.invoke(api, uuid);

      return Boolean.TRUE.equals(result);

    } catch (Exception exception) {

      // Player may have disconnected or the API may have thrown for a transient reason.

      return false;
    }
  }

  // -------------------------------------------------------------------
  // Form building + sending
  // -------------------------------------------------------------------

  /** Callback for a custom-form submission delivered on the Cumulus/Geyser worker thread. */
  public interface FormSubmitCallback {

    /**
     * @param target        text the player entered for the target input; empty when absent
     * @param categoryIndex 0-based index into the category dropdown; {@code -1} when no dropdown
     *                      was included in the form or the response could not be parsed
     * @param detail        text the player entered in the detail input; empty string when absent
     */
    void onValidSubmit(String target, int categoryIndex, String detail);
  }

  /** Callback for a simple-form button click delivered on the Cumulus/Geyser worker thread. */
  public interface SimpleFormCallback {

    /**
     * @param buttonIndex 0-based index of the clicked button
     */
    void onButtonClicked(int buttonIndex);
  }

  /**
   * Builds and sends a Cumulus {@code CustomForm} with a target-player input, an optional
   * category dropdown and a detail input to the given Bedrock player.
   *
   * <p>Field order in the built form is: target input, category dropdown, detail input. The
   * valid-result handler reads them sequentially after {@code reset()} and invokes
   * {@code onValid} on the Cumulus worker thread. The caller is responsible for scheduling any
   * Bukkit work back to the main thread.
   *
   * @param targetUuid        Bedrock player UUID
   * @param title             form title
   * @param contentLabel      optional label displayed under the title (empty to omit)
   * @param targetLabel       label for the target input
   * @param targetPlaceholder text input placeholder for the target field
   * @param targetDefault     default text in the target input (empty to omit)
   * @param categoryLabel     dropdown label (empty when categoryOptions is empty)
   * @param categoryOptions   category option strings; empty to omit the dropdown
   * @param detailLabel       text input label
   * @param detailPlaceholder text input placeholder
   * @param onValid           callback for successful submission (may be null)
   * @param onClosedOrInvalid callback for cancelled/invalid forms (may be null)
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

    if (!available() || targetUuid == null) {

      return false;
    }

    Object builder = invokeStatic(customFormBuilderMethod);

    if (builder == null) {

      return false;
    }

    // title(String)
    invokeOn(builder, "title", new Class<?>[] {String.class}, new Object[] {title});

    // label(String) on CustomForm.Builder — displayed as content/label under the title
    if (contentLabel != null && !contentLabel.isBlank()) {

      invokeOn(builder, "label", new Class<?>[] {String.class}, new Object[] {contentLabel});
    }

    // Target player input: input(String, String, String) when a default is supplied,
    // otherwise input(String, String).
    if (targetDefault != null && !targetDefault.isBlank()) {

      invokeOn(
          builder,
          "input",
          new Class<?>[] {String.class, String.class, String.class},
          new Object[] {
              targetLabel,
              targetPlaceholder == null ? "" : targetPlaceholder,
              targetDefault});
    } else {

      invokeOn(
          builder,
          "input",
          new Class<?>[] {String.class, String.class},
          new Object[] {
              targetLabel, targetPlaceholder == null ? "" : targetPlaceholder});
    }

    // dropdown(String, List, int) when options are present
    if (categoryOptions != null && !categoryOptions.isEmpty()) {

      invokeOn(
          builder,
          "dropdown",
          new Class<?>[] {String.class, List.class, int.class},
          new Object[] {categoryLabel, categoryOptions, 0});
    }

    // Detail input: input(String, String)
    invokeOn(
        builder,
        "input",
        new Class<?>[] {String.class, String.class},
        new Object[] {
            detailLabel, detailPlaceholder == null ? "" : detailPlaceholder});

    // validResultHandler(Consumer<R>) — create a Proxy that implements Consumer
    if (onValid != null) {

      final List<String> options = categoryOptions;

      Object consumerProxy =
          createConsumerProxy(
              response -> {
                if (response == null) {

                  return;
                }

                // Reset the CustomFormResponse iterator to the beginning
                invokeNoArg(response, "reset");

                String target = "";

                // First field is always the target input
                Object targetResult = invokeNoArg(response, "asInput");

                if (targetResult instanceof String s) {

                  target = s;
                }

                int catIndex = -1;

                if (options != null && !options.isEmpty()) {

                  // asDropdown() returns the selected option index and advances the iterator
                  Object dropdownResult = invokeNoArg(response, "asDropdown");

                  if (dropdownResult instanceof Number n) {

                    catIndex = n.intValue();
                  }
                }

                String detail = "";

                Object inputResult = invokeNoArg(response, "asInput");

                if (inputResult instanceof String s) {

                  detail = s;
                }

                onValid.onValidSubmit(target, catIndex, detail);
              });

      invokeOn(
          builder,
          "validResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {consumerProxy});
    }

    // closedOrInvalidResultHandler(Consumer<F>) — fire-and-forget
    if (onClosedOrInvalid != null) {

      Object closedProxy = createConsumerProxy(ignored -> onClosedOrInvalid.run());

      invokeOn(
          builder,
          "closedOrInvalidResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {closedProxy});
    }

    // build() → CustomForm
    Object form = invokeNoArg(builder, "build");

    if (form == null) {

      return false;
    }

    return sendForm(targetUuid, form);
  }

  /**
   * Builds and sends a Cumulus {@code SimpleForm} with a list of buttons to the given Bedrock
   * player.
   *
   * @param targetUuid Bedrock player UUID
   * @param title      form title
   * @param content    form description (empty to omit)
   * @param buttons    button labels
   * @param onValid    callback receiving the 0-based button index; may be null
   * @return true when the form was sent; false when unavailable or send failed
   */
  public boolean sendSimpleForm(
      UUID targetUuid, String title, String content, List<String> buttons,
      SimpleFormCallback onValid) {

    if (!available() || targetUuid == null) {

      return false;
    }

    // Try to build a SimpleForm; fall back to a CustomForm with no dropdowns when SimpleForm
    // is not available (e.g. very old Cumulus).
    Class<?> simpleFormClass = tryLoad("org.geysermc.cumulus.form.SimpleForm");

    if (simpleFormClass == null) {

      // Fall back: send a custom form with no components, which effectively does nothing useful.
      // This path is only reached when SimpleForm is not in the Cumulus version; log and skip.

      return false;
    }

    Object builder = invokeStatic(safeGetMethod(simpleFormClass, "builder"));

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

      Object consumerProxy =
          createConsumerProxy(
              response -> {
                if (response != null) {

                  // SimpleFormResponse.selection() → int (0-based button index)
                  Object selection = invokeNoArg(response, "selection");

                  if (selection instanceof Number n) {

                    onValid.onButtonClicked(n.intValue());
                  }
                }
              });

      invokeOn(
          builder,
          "validResultHandler",
          new Class<?>[] {Consumer.class},
          new Object[] {consumerProxy});
    }

    Object form = invokeNoArg(builder, "build");

    if (form == null) {

      return false;
    }

    return sendForm(targetUuid, form);
  }

  // -------------------------------------------------------------------
  // Internal helpers
  // -------------------------------------------------------------------

  private boolean sendForm(UUID uuid, Object form) {

    if (sendFormMethod == null || api == null) {

      return false;
    }

    try {

      Object result = sendFormMethod.invoke(api, uuid, form);

      // sendForm returns boolean; some implementations may return null (void cast).

      return !(result instanceof Boolean b) || b;

    } catch (Exception exception) {

      return false;
    }
  }

  private static Object loadFloodgateApi() {

    Class<?> floodgateClass = tryLoad("org.geysermc.floodgate.api.FloodgateApi");

    if (floodgateClass == null) {

      return null;
    }

    Method getInstance = safeGetMethod(floodgateClass, "getInstance");

    return getInstance == null ? null : invokeStatic(getInstance);
  }

  private static Object loadGeyserApi() {

    Class<?> geyserClass = tryLoad("org.geysermc.geyser.api.GeyserApi");

    if (geyserClass == null) {

      return null;
    }

    // GeyserApi uses a static factory method "api()"
    Method getApi = safeGetMethod(geyserClass, "api");

    if (getApi != null) {

      return invokeStatic(getApi);
    }

    // Some older Geyser versions use "getInstance()"
    Method getInstance = safeGetMethod(geyserClass, "getInstance");

    return getInstance == null ? null : invokeStatic(getInstance);
  }

  /**
   * Finds the {@code sendForm(UUID, Form)} overload.
   *
   * <p>Both FloodgateApi and GeyserApi declare two overloads:
   * <ul>
   *   <li>{@code sendForm(UUID, Form)}</li>
   *   <li>{@code sendForm(UUID, FormBuilder)}</li>
   * </ul>
   * We prefer the {@code Form} variant because we always pass a built form instance.
   */
  private static Method findSendForm(Object apiInstance) {

    if (apiInstance == null) {

      return null;
    }

    // Preferred: second param is assignable from Cumulus Form
    Class<?> formClass = tryLoad("org.geysermc.cumulus.form.Form");

    for (Method m : apiInstance.getClass().getMethods()) {

      if (!m.getName().equals("sendForm") || m.getParameterCount() != 2) {

        continue;
      }

      if (m.getParameterTypes()[0] != UUID.class) {

        continue;
      }

      if (formClass != null && formClass.isAssignableFrom(m.getParameterTypes()[1])) {

        return m;
      }
    }

    // Fallback: any sendForm(UUID, *)
    for (Method m : apiInstance.getClass().getMethods()) {

      if (m.getName().equals("sendForm")
          && m.getParameterCount() == 2
          && m.getParameterTypes()[0] == UUID.class) {

        return m;
      }
    }

    return null;
  }

  /**
   * Finds the Bedrock-player detection method on the given API instance.
   *
   * @param apiInstance    the API object (FloodgateApi or GeyserApi)
   * @param preferredName  method name to look for first (e.g. "isFloodgatePlayer" or "isBedrockPlayer")
   */
  private static Method findIsBedrock(Object apiInstance, String preferredName) {

    if (apiInstance == null) {

      return null;
    }

    // Preferred name match
    for (Method m : apiInstance.getClass().getMethods()) {

      if (m.getName().equals(preferredName)
          && m.getParameterCount() == 1
          && m.getParameterTypes()[0] == UUID.class
          && m.getReturnType() == boolean.class) {

        return m;
      }
    }

    // Fallback: any single-UUID boolean method containing "Floodgate" or "Bedrock"
    for (Method m : apiInstance.getClass().getMethods()) {

      if (m.getParameterCount() == 1
          && m.getParameterTypes()[0] == UUID.class
          && m.getReturnType() == boolean.class
          && (m.getName().contains("Floodgate") || m.getName().contains("Bedrock"))) {

        return m;
      }
    }

    return null;
  }

  /**
   * Creates a {@link Consumer} proxy that delegates to the given functional callback.
   *
   * <p>Using {@link Proxy} avoids a compile-time dependency on the Cumulus
   * {@code CustomFormResponse} / {@code SimpleFormResponse} types. The proxy implements
   * {@code java.util.function.Consumer}; at runtime the Cumulus library calls {@code accept(obj)}
   * where obj is the actual response type.
   */
  private Object createConsumerProxy(Consumer<Object> delegate) {

    if (consumerClass == null) {

      // Consumer class should always be loadable (it's in the JDK).
      return null;
    }

    InvocationHandler handler =
        (proxy, method, args) -> {
          if ("accept".equals(method.getName()) && args != null && args.length == 1) {

            delegate.accept(args[0]);
          }

          return null;
        };

    return Proxy.newProxyInstance(
        BedrockFormBridge.class.getClassLoader(), new Class<?>[] {consumerClass}, handler);
  }

  /** Invokes a static no-arg method (e.g. {@code CustomForm.builder()}). */
  private static Object invokeStatic(Method method) {

    if (method == null) {

      return null;
    }

    try {

      return method.invoke(null);

    } catch (Exception ignored) {

      return null;
    }
  }

  /** Invokes a parameterized method on an instance, silently returning null on failure. */
  private static Object invokeOn(
      Object target, String methodName, Class<?>[] paramTypes, Object[] args) {

    if (target == null) {

      return null;
    }

    // Try exact match first
    Method method = findMethod(target.getClass(), methodName, paramTypes);

    if (method == null) {

      // Fuzzy match: same method name with the same arg count
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

    } catch (Exception ignored) {

      return null;
    }
  }

  /** Invokes a no-arg method on an instance, silently returning null on failure. */
  private static Object invokeNoArg(Object target, String methodName) {

    return invokeOn(target, methodName, new Class<?>[0], new Object[0]);
  }

  private static Method findMethod(Class<?> clazz, String name, Class<?>[] types) {

    try {

      return clazz.getMethod(name, types);

    } catch (Exception ignored) {

      return null;
    }
  }

  private static Method safeGetMethod(Class<?> clazz, String name) {

    try {

      return clazz.getMethod(name);

    } catch (Exception ignored) {

      return null;
    }
  }

  private static Class<?> tryLoad(String className) {

    try {

      return Class.forName(className);

    } catch (ClassNotFoundException ignored) {

      return null;
    }
  }
}
