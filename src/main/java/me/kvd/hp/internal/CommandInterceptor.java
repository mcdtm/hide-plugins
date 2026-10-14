package me.kvd.hp.internal;

import java.time.Duration;
import java.util.Objects;
import java.util.logging.Logger;
import me.kvd.hp.api.HidePluginsDefaults;
import me.kvd.hp.api.event.PluginProbeEvent;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class CommandInterceptor implements Listener {

  private static final Duration FILTER_WINDOW = Duration.ofSeconds(1);

  private final Plugin plugin;
  private final PermissionMessages messages;
  private final PluginListSpoofer spoofer;
  private final PlayerOverrides overrides;
  private final ProbeTracker probes;
  private final AdminNotifier notifier;
  private final Logger logger;

  public CommandInterceptor(final Plugin plugin,
                            final PermissionMessages messages,
                            final PluginListSpoofer spoofer,
                            final PlayerOverrides overrides,
                            final ProbeTracker probes,
                            final AdminNotifier notifier,
                            final Logger logger) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.messages = Objects.requireNonNull(messages, "messages");
    this.spoofer = Objects.requireNonNull(spoofer, "spoofer");
    this.overrides = Objects.requireNonNull(overrides, "overrides");
    this.probes = Objects.requireNonNull(probes, "probes");
    this.notifier = Objects.requireNonNull(notifier, "notifier");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onCommandPreprocess(final PlayerCommandPreprocessEvent event) {
    SafeExecution.run(this.logger,
      "Process command for " + event.getPlayer().getName(),
      () -> this.handle(event));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(final PlayerQuitEvent event) {
    this.probes.forget(event.getPlayer().getUniqueId());
  }

  private void handle(final PlayerCommandPreprocessEvent event) {
    final Player player = event.getPlayer();
    final String command = CommandNames.normalize(event.getMessage());
    if (!HidePluginsDefaults.HIDDEN_COMMANDS.contains(command)) {
      return;
    }
    if (Permissions.hasBypass(player, command)) {
      this.logger.fine(() -> "bypass for " + player.getName()
        + " on /" + command);
      return;
    }

    this.trackProbe(player, command);

    final Boolean override = this.overrides.get(player.getUniqueId());
    final boolean spoof = null != override
      ? override
      : this.spoofer.isEnabled();

    if (spoof) {
      this.logger.fine(() -> "spoof filter for " + player.getName()
        + " on /" + command);
      this.activateChatFilter(player);
      return;
    }
    if (isHelpCommand(command)) {
      this.logger.fine(() -> "help filter for " + player.getName());
      this.activateChatFilter(player);
      return;
    }

    this.logger.fine(() -> "blocked /" + command
      + " for " + player.getName());
    event.setCancelled(true);
    player.sendMessage(this.messages.message());
  }

  private void trackProbe(final Player player, final String command) {
    final int attempts = this.probes.record(
      player.getUniqueId(), System.nanoTime());
    final PluginProbeEvent probeEvent = new PluginProbeEvent(
      player, command, attempts);
    Bukkit.getPluginManager().callEvent(probeEvent);
    this.notifier.probe(player, command, attempts);
  }

  private void activateChatFilter(final Player player) {
    final PacketInterceptor interceptor = this.lookup(player);
    if (null == interceptor) {
      return;
    }
    interceptor.startChatFilter(FILTER_WINDOW);
  }

  private PacketInterceptor lookup(final Player player) {
    final Channel channel = PlayerChannels.channelOf(player, this.logger);
    if (null == channel) {
      return null;
    }
    final ChannelHandler handler =
      channel.pipeline().get(PacketInterceptor.handlerName());
    return handler instanceof PacketInterceptor interceptor
      ? interceptor
      : null;
  }

  private static boolean isHelpCommand(final String command) {
    return "help".equals(command)
      || "?".equals(command)
      || "bukkit:help".equals(command)
      || "bukkit:?".equals(command);
  }
}