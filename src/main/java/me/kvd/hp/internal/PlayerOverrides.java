package me.kvd.hp.internal;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player spoof override. Absent entry = follow the global setting.
 */
public final class PlayerOverrides {

  private final Map<UUID, Boolean> values = new ConcurrentHashMap<>();

  /**
   * Returns the override for the player, or {@code null} when the
   * global setting applies.
   */
  Boolean get(final UUID playerId) {
    return this.values.get(playerId);
  }

  void set(final UUID playerId, final boolean spoof) {
    this.values.put(playerId, spoof);
  }

  void reset(final UUID playerId) {
    this.values.remove(playerId);
  }
}