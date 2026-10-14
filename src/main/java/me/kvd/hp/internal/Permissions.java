package me.kvd.hp.internal;

import java.util.Locale;
import me.kvd.hp.api.HidePluginsDefaults;
import org.bukkit.entity.Player;

/**
 * Centralizes permission checks.
 *
 * <p>Permission hierarchy (Vault-aware, since it relies solely on
 * {@link Player#hasPermission(String)}):</p>
 * <ul>
 *   <li>{@code hideplugins.bypass} — all hidden commands;</li>
 *   <li>{@code hideplugins.bypass.plugins} — {@code /plugins},
 *       {@code /pl}, {@code /ver}, {@code /version}, {@code /about},
 *       {@code /icanhasbukkit};</li>
 *   <li>{@code hideplugins.bypass.help} — {@code /help}, {@code /?};</li>
 *   <li>{@code bukkit.command.plugins} — vanilla node, treated as
 *       {@code hideplugins.bypass.plugins}.</li>
 * </ul>
 */
final class Permissions {

  private Permissions() {
    throw new AssertionError("No instances.");
  }

  /**
   * Returns whether the player bypasses the filter for the given
   * command.
   */
  static boolean hasBypass(final Player player, final String command) {
    if (player.hasPermission(HidePluginsDefaults.BYPASS_PERMISSION)) {
      return true;
    }
    if (player.hasPermission(
      HidePluginsDefaults.VANILLA_BYPASS_PERMISSION)) {
      return true;
    }
    final String category = categoryOf(command);
    if (null == category) {
      return false;
    }
    return player.hasPermission(
      "hideplugins.bypass." + category);
  }

  /**
   * Returns whether the player has any form of bypass, regardless of
   * command. Used by the chat filter which has no command context.
   */
  static boolean hasAnyBypass(final Player player) {
    return player.hasPermission(HidePluginsDefaults.BYPASS_PERMISSION)
      || player.hasPermission(
      HidePluginsDefaults.VANILLA_BYPASS_PERMISSION)
      || player.hasPermission(
      HidePluginsDefaults.BYPASS_PLUGINS_PERMISSION)
      || player.hasPermission(
      HidePluginsDefaults.BYPASS_HELP_PERMISSION);
  }

  /**
   * Returns the permission category for a command name, or
   * {@code null} when the command is not mapped.
   */
  static String categoryOf(final String command) {
    if (null == command || command.isEmpty()) {
      return null;
    }
    String name = command.toLowerCase(Locale.ROOT);
    final int colon = name.indexOf(':');
    if (0 < colon) {
      name = name.substring(colon + 1);
    }
    return switch (name) {
      case "plugins", "pl", "ver", "version", "about",
           "icanhasbukkit" -> "plugins";
      case "help", "?" -> "help";
      default -> null;
    };
  }
}