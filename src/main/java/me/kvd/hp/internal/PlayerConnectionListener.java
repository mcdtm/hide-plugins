package me.kvd.hp.internal;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.logging.Logger;
import net.minecraft.util.io.netty.channel.Channel;
import net.minecraft.util.io.netty.channel.ChannelPipeline;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

  private final PluginListSpoofer spoofer;
  private final PlayerOverrides overrides;
  private final Logger logger;

  public PlayerConnectionListener(final PluginListSpoofer spoofer,
                                  final PlayerOverrides overrides,
                                  final Logger logger) {
    this.spoofer = Objects.requireNonNull(spoofer, "spoofer");
    this.overrides = Objects.requireNonNull(overrides, "overrides");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(final PlayerJoinEvent event) {
    SafeExecution.run(this.logger,
      "Install interceptor for " + event.getPlayer().getName(),
      () -> this.install(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(final PlayerQuitEvent event) {
    SafeExecution.run(this.logger,
      "Remove interceptor for " + event.getPlayer().getName(),
      () -> this.uninstall(event.getPlayer()));
  }

  private void install(final Player player) {
    final Channel channel = PlayerChannels.channelOf(player, this.logger);
    if (null == channel) {
      return;
    }
    final ChannelPipeline pipeline = channel.pipeline();
    if (null != pipeline.get(PacketInterceptor.handlerName())) {
      return;
    }
    final PacketInterceptor interceptor = new PacketInterceptor(
      player, this.spoofer, this.overrides, this.logger);
    try {
      pipeline.addAfter("packet_handler",
        PacketInterceptor.handlerName(), interceptor);
    } catch (final NoSuchElementException exception) {
      pipeline.addLast(PacketInterceptor.handlerName(), interceptor);
    }
  }

  private void uninstall(final Player player) {
    final Channel channel = PlayerChannels.channelOf(player, this.logger);
    if (null == channel) {
      return;
    }
    final ChannelPipeline pipeline = channel.pipeline();
    if (null != pipeline.get(PacketInterceptor.handlerName())) {
      pipeline.remove(PacketInterceptor.handlerName());
    }
  }
}