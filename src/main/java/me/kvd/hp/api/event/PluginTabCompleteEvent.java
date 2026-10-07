package me.kvd.hp.api.event;

import java.util.Collection;
import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before the plugin filters plugin commands out of a tab
 * completion list.
 *
 * <p>The {@link #getCompletions() completion collection} is the live
 * list that will be shown to the player. Listeners may remove entries
 * to pre-filter them, or add entries to whitelist specific commands.
 * Cancelling the event disables the built-in filter entirely.</p>
 */
public final class PluginTabCompleteEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Player player;
  private final String chatMessage;
  private final Collection<String> completions;
  private boolean cancelled;

  /**
   * @param player      the player triggering the completion; never {@code null}
   * @param chatMessage the full input, including the leading slash
   *                    for commands; never {@code null}
   * @param completions the mutable suggestion list; never {@code null}
   */
  public PluginTabCompleteEvent(final Player player,
                                final String chatMessage,
                                final Collection<String> completions) {
    this.player = Objects.requireNonNull(player, "player");
    this.chatMessage = Objects.requireNonNull(chatMessage, "chatMessage");
    this.completions = Objects.requireNonNull(completions, "completions");
  }

  public Player getPlayer() {
    return this.player;
  }

  public String getChatMessage() {
    return this.chatMessage;
  }

  public Collection<String> getCompletions() {
    return this.completions;
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