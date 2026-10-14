package me.kvd.hp.internal;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * Provides the server's default messages that are normally emitted by
 * the vanilla command dispatcher.
 *
 * <p>The permission-denied message is resolved reflectively from the
 * NMS {@code LocaleI18n} registry under the key
 * {@code commands.generic.permission} — the exact source used by the
 * server itself. A hardcoded fallback is used only when the reflective
 * lookup fails or returns the raw key.</p>
 */
public final class DefaultMessages {

  private static final String LOCALE_CLASS =
    "net.minecraft.server.v1_7_R4.LocaleI18n";

  private static final String PERMISSION_KEY = "commands.generic.permission";

  private static final String FALLBACK_PERMISSION_MESSAGE =
    "I'm sorry, but you do not have permission to perform "
      + "this command. Please contact the server administrators "
      + "if you believe that this is in error.";

  private final String permissionMessage;

  public DefaultMessages(final Logger logger) {
    Objects.requireNonNull(logger, "logger");
    this.permissionMessage = resolve(logger);
  }

  /**
   * Returns the server's default permission-denied message, prefixed
   * with the legacy red colour code.
   *
   * @return the permission message; never {@code null}
   */
  String permissionMessage() {
    return this.permissionMessage;
  }

  private static String resolve(final Logger logger) {
    final Object result = Reflection.invokeStaticStringArg(
      LOCALE_CLASS, PERMISSION_KEY, "get", "a");
    if (result instanceof String text
      && !text.isBlank()
      && !PERMISSION_KEY.equals(text)) {
      return "\u00A7c" + text;
    }
    logger.fine("LocaleI18n lookup failed; using hardcoded fallback.");
    return "\u00A7c" + FALLBACK_PERMISSION_MESSAGE;
  }
}