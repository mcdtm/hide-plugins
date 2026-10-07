package me.kvd.hp.api;

import org.bukkit.entity.Player;

/**
 * Public contract exposed by the HidePlugins plugin.
 *
 * <p>Consumers obtain an instance via
 * {@code Bukkit.getServicesManager().load(HidePluginsAPI.class)}.</p>
 */
public interface HidePluginsAPI {

  /**
   * Version of the public API. Incremented on every breaking change.
   */
  int API_VERSION = 1;

  /**
   * Returns whether the plugin list should be hidden from the given
   * player.
   *
   * @param player the player to check; may be {@code null}
   * @return {@code true} when the list must be hidden
   */
  boolean isHiddenFor(Player player);
}