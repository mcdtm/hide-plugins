package me.kvd.hp.internal;

import me.kvd.hp.api.HidePluginsAPI;
import me.kvd.hp.api.HidePluginsDefaults;
import org.bukkit.entity.Player;

/**
 * Default {@link HidePluginsAPI} implementation.
 *
 * <p>Stateless: every decision is derived from
 * {@link HidePluginsDefaults}. This is intentional — removing the
 * YAML configuration also removed the only mutable state the plugin
 * ever had.</p>
 */
public final class HidePluginsServiceImpl implements HidePluginsAPI {

  @Override
  public boolean isHiddenFor(final Player player) {
    if (null == player) {
      return false;
    }
    return !player.hasPermission(HidePluginsDefaults.BYPASS_PERMISSION);
  }
}