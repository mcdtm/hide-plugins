package me.kvd.hp.api.event;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired when a player without bypass permission attempts to execute a
 * command whose output would reveal the plugin list.
 *
 * <p>Other plugins may use this event to implement rate-limiting,
 * notifications, or logging. The event is not cancellable — it is a
 * notification, not an interception point.</p>
 */
public final class PluginProbeEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final String command;
  private final int recentAttempts;

  public PluginProbeEvent(final Player player,
                          final String command,
                          final int recentAttempts) {
    this.player = Objects.requireNonNull(player, "player");
    this.command = Objects.requireNonNull(command, "command");
    this.recentAttempts = recentAttempts;
  }

  public Player getPlayer() {
    return this.player;
  }

  public String getCommand() {
    return this.command;
  }

  /**
   * Number of probes this player has made within the tracking window,
   * including the current one.
   */
  public int getRecentAttempts() {
    return this.recentAttempts;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}