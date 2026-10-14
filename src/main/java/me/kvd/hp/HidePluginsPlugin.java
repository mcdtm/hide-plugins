package me.kvd.hp;

import me.kvd.hp.api.HidePluginsAPI;
import me.kvd.hp.api.Settings;
import me.kvd.hp.internal.AdminNotifier;
import me.kvd.hp.internal.CommandInterceptor;
import me.kvd.hp.internal.HidePluginsCommand;
import me.kvd.hp.internal.HidePluginsServiceImpl;
import me.kvd.hp.internal.PermissionMessages;
import me.kvd.hp.internal.PlayerConnectionListener;
import me.kvd.hp.internal.PlayerOverrides;
import me.kvd.hp.internal.PluginListSpoofer;
import me.kvd.hp.internal.PluginNameCache;
import me.kvd.hp.internal.ProbeTracker;
import me.kvd.hp.internal.SettingsLoader;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class HidePluginsPlugin extends JavaPlugin {

  private HidePluginsServiceImpl api;
  private Settings settings;
  private SettingsLoader settingsLoader;
  private PermissionMessages messages;
  private PluginListSpoofer spoofer;
  private PluginNameCache pluginNameCache;
  private PlayerOverrides overrides;
  private ProbeTracker probes;
  private AdminNotifier notifier;
  private CommandInterceptor commandInterceptor;
  private PlayerConnectionListener connectionListener;

  @Override
  public void onEnable() {
    this.settingsLoader = new SettingsLoader(this, this.getLogger());
    this.settings = this.settingsLoader.load();
    this.applyLogLevel();

    this.pluginNameCache = new PluginNameCache();
    this.messages = new PermissionMessages(this.settings, this.getLogger());
    this.spoofer = new PluginListSpoofer(this.settings, this.pluginNameCache);
    this.overrides = new PlayerOverrides();
    this.probes = new ProbeTracker(this.settings.notificationWindowSeconds());
    this.notifier = new AdminNotifier(this.settings, this.getLogger());
    this.api = new HidePluginsServiceImpl();

    this.commandInterceptor = new CommandInterceptor(
      this, this.messages, this.spoofer, this.overrides,
      this.probes, this.notifier, this.getLogger());
    this.connectionListener = new PlayerConnectionListener(
      this.spoofer, this.overrides, this.getLogger());

    this.getServer().getPluginManager()
      .registerEvents(this.commandInterceptor, this);
    this.getServer().getPluginManager()
      .registerEvents(this.connectionListener, this);
    this.getServer().getPluginManager()
      .registerEvents(this.pluginNameCache, this);

    final HidePluginsCommand command = new HidePluginsCommand(
      this, this.settingsLoader, this.settings,
      this.messages, this.spoofer, this.overrides,
      this.notifier, this.getLogger());
    final var pluginCommand = this.getCommand("hideplugins");
    if (null != pluginCommand) {
      pluginCommand.setExecutor(command);
    }

    this.getServer().getServicesManager().register(
      HidePluginsAPI.class, this.api, this, ServicePriority.Normal);

    this.getLogger().info("HidePlugins enabled (API v"
      + HidePluginsAPI.API_VERSION
      + ", mode=" + this.settings.messageMode()
      + ", spoof=" + this.spoofer.isEnabled()
      + ", debug=" + this.settings.debug()
      + ", tracked plugins=" + this.pluginNameCache.names().size()
      + ").");
  }

  @Override
  public void onDisable() {
    if (null != this.commandInterceptor) {
      HandlerList.unregisterAll(this.commandInterceptor);
      this.commandInterceptor = null;
    }
    if (null != this.connectionListener) {
      HandlerList.unregisterAll(this.connectionListener);
      this.connectionListener = null;
    }
    if (null != this.pluginNameCache) {
      HandlerList.unregisterAll(this.pluginNameCache);
      this.pluginNameCache = null;
    }
    this.getServer().getServicesManager().unregisterAll(this);
    this.api = null;
    this.notifier = null;
    this.probes = null;
    this.overrides = null;
    this.spoofer = null;
    this.messages = null;
    this.settings = null;
    this.getLogger().info("HidePlugins disabled.");
  }

  private void applyLogLevel() {
    this.getLogger().setLevel(
      this.settings.debug() ? Level.ALL : Level.INFO);
  }
}