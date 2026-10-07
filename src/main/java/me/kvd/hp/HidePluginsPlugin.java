package me.kvd.hp;

import me.kvd.hp.api.HidePluginsAPI;
import me.kvd.hp.internal.CommandInterceptor;
import me.kvd.hp.internal.HidePluginsServiceImpl;
import me.kvd.hp.internal.TabCompleteInterceptor;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point.
 *
 * <p>Responsibilities are limited to the plugin lifecycle: wiring
 * listeners, exposing the API through the
 * {@link org.bukkit.plugin.ServicesManager ServicesManager}, and
 * tearing everything down on disable.</p>
 */
public final class HidePluginsPlugin extends JavaPlugin {

  private HidePluginsServiceImpl api;
  private CommandInterceptor commandInterceptor;
  private TabCompleteInterceptor tabCompleteInterceptor;

  @Override
  public void onEnable() {
    var logger = this.getLogger();
    this.api = new HidePluginsServiceImpl();
    this.commandInterceptor = new CommandInterceptor(logger);
    this.tabCompleteInterceptor = new TabCompleteInterceptor(logger);

    this.getServer().getPluginManager()
      .registerEvents(this.commandInterceptor, this);
    this.getServer().getPluginManager()
      .registerEvents(this.tabCompleteInterceptor, this);

    this.getServer().getServicesManager().register(
      HidePluginsAPI.class,
      this.api,
      this,
      ServicePriority.Normal);

    logger.info("HidePlugins enabled (API v"
      + HidePluginsAPI.API_VERSION + ").");
  }

  @Override
  public void onDisable() {
    if (null != this.commandInterceptor) {
      HandlerList.unregisterAll(this.commandInterceptor);
      this.commandInterceptor = null;
    }
    if (null != this.tabCompleteInterceptor) {
      HandlerList.unregisterAll(this.tabCompleteInterceptor);
      this.tabCompleteInterceptor = null;
    }
    this.getServer().getServicesManager().unregisterAll(this);
    this.api = null;
    this.getLogger().info("HidePlugins disabled.");
  }
}