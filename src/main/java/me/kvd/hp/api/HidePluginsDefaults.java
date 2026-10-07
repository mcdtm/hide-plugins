package me.kvd.hp.api;

import java.util.Set;

/**
 * Compile-time defaults for the HidePlugins plugin.
 *
 * <p>The set of commands that would reveal the plugin list is finite
 * and part of the plugin's contract, therefore it is hardcoded instead
 * of being configurable. Servers requiring different behaviour should
 * wrap the plugin through
 * {@link me.kvdpxne.hideplugins.api.HidePluginsAPI}.</p>
 */
public final class HidePluginsDefaults {

  /**
   * Command names (lowercase, no leading slash) whose execution would
   * reveal the installed plugins. The namespaced {@code bukkit:}
   * variants must be listed explicitly because the vanilla command
   * map registers them under a different label.
   */
  public static final Set<String> HIDDEN_COMMANDS = Set.of(
    "plugins",
    "pl",
    "bukkit:plugins",
    "bukkit:pl");

  /**
   * Legacy-formatted message sent to players who lack the bypass
   * permission.
   */
  public static final String HIDDEN_MESSAGE =
    "\u00A7cYou don't have permission to view the plugin list.";

  /**
   * Permission node granting access to the full plugin list.
   */
  public static final String BYPASS_PERMISSION = "hideplugins.bypass";

  private HidePluginsDefaults() {
    throw new AssertionError("No instances.");
  }
}