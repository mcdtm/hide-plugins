
package me.kvd.hp.api.event;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before the plugin list is hidden from a player.
 *
 * <p>Other plugins may:
 * <ul>
 *   <li>cancel the event to let the vanilla command run,</li>
 *   <li>replace {@link #getMessage() the message} sent to the player.</li>
 * </ul>
 * </p>
 */
public final class PluginListHideEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final String command;
  private String message;
  private boolean cancelled;

  /**
   * @param player  the player who invoked the command; never {@code null}
   * @param command normalized command name without arguments; never {@code null}
   * @param message initial message to send to the player; never {@code null}
   */
  public PluginListHideEvent(final Player player,
                             final String command,
                             final String message) {
    this.player = Objects.requireNonNull(player, "player");
    this.command = Objects.requireNonNull(command, "command");
    this.message = Objects.requireNonNull(message, "message");
  }

  public Player getPlayer() {
    return this.player;
  }

  public String getCommand() {
    return this.command;
  }

  public String getMessage() {
    return this.message;
  }

  public void setMessage(final String message) {
    this.message = Objects.requireNonNull(message, "message");
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  @Override
  public void setCancelled(final boolean cancel) {
    this.cancelled = cancel;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}