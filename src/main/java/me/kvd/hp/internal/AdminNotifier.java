package me.kvd.hp.internal;

import java.util.Objects;
import java.util.logging.Logger;
import me.kvd.hp.api.HidePluginsDefaults;
import me.kvd.hp.api.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Sends probe notifications to online staff and the console.
 *
 * <p>Notifications are throttled: only every {@code N}-th probe of a
 * sliding window is broadcast, where {@code N} is
 * {@link Settings#notificationThreshold()}. The console always receives
 * a WARNING entry regardless of the throttle.</p>
 */
public final class AdminNotifier {

  private volatile Settings settings;
  private final Logger logger;

  public AdminNotifier(final Settings settings, final Logger logger) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  void reload(final Settings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  /**
   * Fires a notification if the attempt count matches the configured
   * threshold cycle.
   *
   * @param player   the probing player; never {@code null}
   * @param command  the normalized command name; never {@code null}
   * @param attempts total attempts within the tracking window
   */
  void probe(final Player player, final String command,
             final int attempts) {
    final Settings current = this.settings;
    if (!current.notificationsEnabled()) {
      return;
    }
    final int threshold = current.notificationThreshold();
    if (attempts < threshold || 0 != attempts % threshold) {
      return;
    }

    final String summary = player.getName() + " probed /" + command
      + " (" + attempts + " times in "
      + current.notificationWindowSeconds() + "s)";
    this.logger.warning(summary);

    final String chat = "\u00A7c[HidePlugins] \u00A7f"
      + player.getName() + " \u00A77probed \u00A7f/" + command
      + " \u00A78(" + attempts + "\u00A78)";
    for (final Player online : Bukkit.getServer().getOnlinePlayers()) {
      if (online.hasPermission(HidePluginsDefaults.NOTIFY_PERMISSION)) {
        online.sendMessage(chat);
      }
    }
  }
}