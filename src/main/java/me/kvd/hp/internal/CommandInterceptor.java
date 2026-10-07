package me.kvd.hp.internal;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.kvd.hp.api.HidePluginsDefaults;
import me.kvd.hp.api.event.PluginListHideEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Intercepts player commands that would reveal the plugin list.
 *
 * <p>{@link PlayerCommandPreprocessEvent} fires only for players, so
 * console and RCON invocations of {@code /plugins} remain unaffected —
 * which is intended.</p>
 */
public final class CommandInterceptor implements Listener {

  private final Logger logger;

  public CommandInterceptor(final Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @EventHandler(
    priority = EventPriority.NORMAL,
    ignoreCancelled = true)
  public void onCommandPreprocess(final PlayerCommandPreprocessEvent event) {
    try {
      this.handle(event);
    } catch (final RuntimeException exception) {
      this.logger.log(Level.SEVERE,
        "Failed to process command for "
          + event.getPlayer().getName(),
        exception);
    }
  }

  private void handle(final PlayerCommandPreprocessEvent event) {
    final Player player = event.getPlayer();
    final String command = CommandNames.normalize(event.getMessage());
    if (command.isEmpty()) {
      return;
    }
    if (!HidePluginsDefaults.HIDDEN_COMMANDS.contains(command)) {
      return;
    }
    if (player.hasPermission(HidePluginsDefaults.BYPASS_PERMISSION)) {
      return;
    }

    final var hideEvent = new PluginListHideEvent(
      player, command, HidePluginsDefaults.HIDDEN_MESSAGE);
    Bukkit.getPluginManager().callEvent(hideEvent);
    if (hideEvent.isCancelled()) {
      return;
    }

    event.setCancelled(true);
    player.sendMessage(hideEvent.getMessage());
  }
}