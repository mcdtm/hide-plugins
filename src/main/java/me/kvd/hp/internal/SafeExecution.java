package me.kvd.hp.internal;

import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps listener bodies with a uniform exception-handling policy.
 *
 * <p>Every event handler and packet hook in the plugin runs through this
 * class. Exceptions are logged at {@code SEVERE} with a caller-supplied
 * context string; they never propagate back to Bukkit or Netty, which
 * would otherwise disconnect the player or crash the tick loop.</p>
 */
final class SafeExecution {

  private SafeExecution() {
    throw new AssertionError("No instances.");
  }

  /**
   * Runs the given body, logging any {@link RuntimeException} that
   * escapes it.
   */
  static void run(final Logger logger,
                  final String context,
                  final Runnable body) {
    Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(body, "body");
    try {
      body.run();
    } catch (final RuntimeException exception) {
      logger.log(Level.SEVERE, context, exception);
    }
  }

  /**
   * Runs the given supplier, returning {@code fallback} when a
   * {@link RuntimeException} escapes it.
   */
  static <T> T supply(final Logger logger,
                      final String context,
                      final Supplier<T> body,
                      final T fallback) {
    Objects.requireNonNull(logger, "logger");
    Objects.requireNonNull(context, "context");
    Objects.requireNonNull(body, "body");
    try {
      return body.get();
    } catch (final RuntimeException exception) {
      logger.log(Level.SEVERE, context, exception);
      return fallback;
    }
  }
}