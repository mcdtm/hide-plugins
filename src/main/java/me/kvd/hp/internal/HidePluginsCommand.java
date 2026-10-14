package me.kvd.hp.internal;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.kvd.hp.api.HidePluginsDefaults;
import me.kvd.hp.api.Settings;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

/**
 * Implements the {@code /hideplugins} administration command.
 *
 * <p>Subcommands: {@code reload}, {@code status}, {@code spoof
 * <player> <on|off|reset>}.</p>
 */
public final class HidePluginsCommand implements CommandExecutor {

  private final Plugin plugin;
  private final SettingsLoader loader;
  private final PermissionMessages messages;
  private final PluginListSpoofer spoofer;
  private final PlayerOverrides overrides;
  private final AdminNotifier notifier;
  private final Logger logger;

  private volatile Settings settings;

  public HidePluginsCommand(final Plugin plugin,
                            final SettingsLoader loader,
                            final Settings initialSettings,
                            final PermissionMessages messages,
                            final PluginListSpoofer spoofer,
                            final PlayerOverrides overrides,
                            final AdminNotifier notifier,
                            final Logger logger) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.loader = Objects.requireNonNull(loader, "loader");
    this.settings = Objects.requireNonNull(initialSettings, "settings");
    this.messages = Objects.requireNonNull(messages, "messages");
    this.spoofer = Objects.requireNonNull(spoofer, "spoofer");
    this.overrides = Objects.requireNonNull(overrides, "overrides");
    this.notifier = Objects.requireNonNull(notifier, "notifier");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  @Override
  public boolean onCommand(final CommandSender sender,
                           final Command command,
                           final String label,
                           final String[] args) {
    if (!sender.hasPermission(HidePluginsDefaults.ADMIN_PERMISSION)) {
      sender.sendMessage("\u00A7cYou don't have permission.");
      return true;
    }
    if (0 == args.length) {
      this.sendUsage(sender, label);
      return true;
    }
    final String sub = args[0].toLowerCase(Locale.ROOT);
    return switch (sub) {
      case "reload" -> this.handleReload(sender);
      case "status" -> this.handleStatus(sender);
      case "spoof" -> this.handleSpoof(sender, args);
      default -> {
        sender.sendMessage("\u00A7cUnknown subcommand.");
        this.sendUsage(sender, label);
        yield true;
      }
    };
  }

  private void sendUsage(final CommandSender sender, final String label) {
    sender.sendMessage("\u00A76HidePlugins \u00A77administration");
    sender.sendMessage("\u00A77/" + label + " reload");
    sender.sendMessage("\u00A77/" + label + " status");
    sender.sendMessage("\u00A77/" + label + " spoof <player> <on|off|reset>");
  }

  private boolean handleReload(final CommandSender sender) {
    try {
      final Settings reloaded = this.loader.load();
      this.settings = reloaded;
      this.messages.reload(reloaded, this.logger);
      this.spoofer.reload(reloaded);
      this.notifier.reload(reloaded);
      sender.sendMessage("\u00A7aHidePlugins reloaded. spoof="
        + reloaded.spoofEnabled()
        + ", mode=" + reloaded.messageMode()
        + ", debug=" + reloaded.debug());
    } catch (final RuntimeException exception) {
      this.logger.log(Level.SEVERE, "Failed to reload settings.", exception);
      sender.sendMessage("\u00A7cReload failed; see console.");
    }
    return true;
  }

  private boolean handleStatus(final CommandSender sender) {
    final Settings current = this.settings;
    sender.sendMessage("\u00A76HidePlugins \u00A77status");
    sender.sendMessage("\u00A77Message mode: \u00A7f" + current.messageMode());
    sender.sendMessage("\u00A77Spoof enabled: \u00A7f" + current.spoofEnabled());
    sender.sendMessage("\u00A77Fake plugins (\u00A7f"
      + current.spoofPlugins().size() + "\u00A77): \u00A7f"
      + String.join(", ", current.spoofPlugins()));
    sender.sendMessage("\u00A77Spoof header: \u00A7f"
      + current.spoofHeader());
    sender.sendMessage("\u00A77Debug: \u00A7f" + current.debug());
    sender.sendMessage("\u00A77Notifications: \u00A7f"
      + current.notificationsEnabled()
      + " \u00A77(threshold \u00A7f"
      + current.notificationThreshold() + "\u00A77/"
      + current.notificationWindowSeconds() + "s)");
    return true;
  }

  private boolean handleSpoof(final CommandSender sender,
                              final String[] args) {
    if (3 > args.length) {
      sender.sendMessage("\u00A7cUsage: /hideplugins spoof <player> <on|off|reset>");
      return true;
    }
    final String playerName = args[1];
    final String action = args[2].toLowerCase(Locale.ROOT);

    final OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
    if (null == target || (null == target.getName() && !target.hasPlayedBefore())) {
      sender.sendMessage("\u00A7cUnknown player: " + playerName);
      return true;
    }

    return switch (action) {
      case "on" -> {
        this.overrides.set(target.getUniqueId(), true);
        sender.sendMessage("\u00A7aSpoof forced ON for "
          + target.getName());
        yield true;
      }
      case "off" -> {
        this.overrides.set(target.getUniqueId(), false);
        sender.sendMessage("\u00A7aSpoof forced OFF for "
          + target.getName());
        yield true;
      }
      case "reset" -> {
        this.overrides.reset(target.getUniqueId());
        sender.sendMessage("\u00A7aSpoof override cleared for "
          + target.getName());
        yield true;
      }
      default -> {
        sender.sendMessage("\u00A7cAction must be on, off or reset.");
        yield true;
      }
    };
  }
}