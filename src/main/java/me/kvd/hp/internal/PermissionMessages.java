package me.kvd.hp.internal;

import java.util.Objects;
import java.util.logging.Logger;
import me.kvd.hp.api.Settings;

public final class PermissionMessages {

  private static final String LOCALE_CLASS =
    "net.minecraft.server.v1_7_R4.LocaleI18n";
  private static final String PERMISSION_KEY = "commands.generic.permission";
  private static final String BUKKIT_MESSAGE =
    "I'm sorry, but you do not have permission to perform "
      + "this command. Please contact the server administrators "
      + "if you believe that this is in error.";

  private volatile String cachedMessage;

  public PermissionMessages(final Settings settings, final Logger logger) {
    Objects.requireNonNull(logger, "logger");
    this.cachedMessage = resolve(settings, logger);
  }

  String message() {
    return this.cachedMessage;
  }

  void reload(final Settings settings, final Logger logger) {
    this.cachedMessage = resolve(settings, logger);
  }

  private static String resolve(final Settings settings,
                                final Logger logger) {
    return switch (settings.messageMode()) {
      case MINECRAFT -> fromMinecraft(logger);
      case BUKKIT -> "\u00A7c" + BUKKIT_MESSAGE;
      case CUSTOM -> colorize(settings.customMessage());
    };
  }

  private static String fromMinecraft(final Logger logger) {
    final Object result = Reflection.invokeStaticStringArg(
      LOCALE_CLASS, PERMISSION_KEY, "get", "a");
    if (result instanceof String text
      && !text.isBlank()
      && !PERMISSION_KEY.equals(text)) {
      return "\u00A7c" + text;
    }
    logger.fine("LocaleI18n lookup failed; using Bukkit fallback.");
    return "\u00A7c" + BUKKIT_MESSAGE;
  }

  private static String colorize(final String raw) {
    if (null == raw || raw.isEmpty()) {
      return "\u00A7c" + BUKKIT_MESSAGE;
    }
    return raw.replace('&', '\u00A7');
  }
}