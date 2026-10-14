package me.kvd.hp.internal;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

/**
 * Caches the names of every loaded plugin so that hot-path code does
 * not iterate the plugin manager on each chat line or tab request.
 *
 * <p>The cache is rebuilt whenever a plugin is enabled or disabled.
 * Values are immutable and safe to publish across threads.</p>
 */
public final class PluginNameCache implements Listener {

  private static final int MIN_NAME_LENGTH = 3;

  private volatile Set<String> names = Set.of();

  public PluginNameCache() {
    this.rebuild();
  }

  public Set<String> names() {
    return this.names;
  }

  void rebuild() {
    final Plugin[] plugins = Bukkit.getPluginManager().getPlugins();
    final Set<String> collected = new HashSet<>(plugins.length * 2);
    for (final Plugin plugin : plugins) {
      final String name = plugin.getName();
      if (null != name && MIN_NAME_LENGTH <= name.length()) {
        collected.add(name);
      }
    }
    this.names = Set.copyOf(collected);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onEnable(final PluginEnableEvent event) {
    this.rebuild();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onDisable(final PluginDisableEvent event) {
    this.rebuild();
  }
}