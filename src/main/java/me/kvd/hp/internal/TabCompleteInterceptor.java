package me.kvd.hp.internal;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.kvd.hp.api.HidePluginsDefaults;
import me.kvd.hp.api.event.PluginTabCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

/**
 * Removes plugin commands from tab completion suggestions for players
 * who lack the bypass permission.
 *
 * <p>On 1.7.10 the only hook for tab completion — including the
 * {@code /}&nbsp;+&nbsp;TAB root completion and the {@code /?} help
 * argument completion — is {@link PlayerChatTabCompleteEvent}. The
 * event exposes a mutable list of full suggestion strings, which is
 * what we filter here.</p>
 *
 * <p>Filtering is purely cosmetic: it never prevents a player from
 * typing a plugin command manually. Only the discovery channel is
 * closed.</p>
 */
public final class TabCompleteInterceptor implements Listener {

  private final Logger logger;

  public TabCompleteInterceptor(final Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onTabComplete(final PlayerChatTabCompleteEvent event) {
    try {
      this.handle(event);
    } catch (final RuntimeException exception) {
      this.logger.log(Level.SEVERE,
        "Failed to filter tab completion for "
          + event.getPlayer().getName(),
        exception);
    }
  }

  private void handle(final PlayerChatTabCompleteEvent event) {
    final Player player = event.getPlayer();
    if (player.hasPermission(HidePluginsDefaults.BYPASS_PERMISSION)) {
      return;
    }

    final Collection<String> completions = event.getTabCompletions();
    if (null == completions || completions.isEmpty()) {
      return;
    }

    final PluginTabCompleteEvent filterEvent = new PluginTabCompleteEvent(
      player, event.getChatMessage(), completions);
    Bukkit.getPluginManager().callEvent(filterEvent);
    if (filterEvent.isCancelled()) {
      return;
    }

    final Iterator<String> iterator = completions.iterator();
    while (iterator.hasNext()) {
      final String candidate = iterator.next();
      if (null == candidate) {
        iterator.remove();
        continue;
      }
      if (isPluginCommand(candidate)) {
        iterator.remove();
      }
    }
  }

  /**
   * Returns whether the given completion candidate resolves to a
   * command registered by a plugin (i.e. anything but a vanilla
   * command).
   */
  private static boolean isPluginCommand(final String candidate) {
    String name = CommandNames.normalize(candidate);
    if (name.isEmpty()) {
      return false;
    }

    // Namespaced form, e.g. "plotme:plotme" -> also try the tail.
    final int colon = name.indexOf(':');
    if (0 < colon) {
      if (null != Bukkit.getPluginCommand(name)) {
        return true;
      }
      name = name.substring(colon + 1);
    }

    return null != Bukkit.getPluginCommand(name.toLowerCase(Locale.ROOT));
  }
}