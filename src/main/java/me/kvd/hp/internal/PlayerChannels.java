package me.kvd.hp.internal;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.logging.Logger;
import net.minecraft.util.io.netty.channel.Channel;
import org.bukkit.entity.Player;

/**
 * Resolves a player's Netty channel via NMS reflection.
 *
 * <p>In 1.7.10 the channel is stored in
 * {@code NetworkManager} under the field name {@code m}. Other
 * historical revisions used {@code k} (1.7.2) or {@code channel}
 * (1.8.3+); the lookup tries each candidate in turn.</p>
 */
final class PlayerChannels {

  private static final String[] CHANNEL_FIELD_CANDIDATES = {
    "m", "k", "i", "channel"
  };

  private PlayerChannels() {
    throw new AssertionError("No instances.");
  }

  /**
   * Returns the Netty channel backing the given player, or
   * {@code null} when it cannot be resolved.
   */
  static Channel channelOf(final Player player, final Logger logger) {
    Objects.requireNonNull(player, "player");
    Objects.requireNonNull(logger, "logger");

    final Object craftPlayer = invokeNoArg(player, "getHandle");
    if (null == craftPlayer) {
      return null;
    }
    final Object playerConnection = readPublic(craftPlayer, "playerConnection");
    if (null == playerConnection) {
      return null;
    }
    final Object networkManager = readPublic(playerConnection, "networkManager");
    if (null == networkManager) {
      return null;
    }

    for (final String candidate : CHANNEL_FIELD_CANDIDATES) {
      final Field field = Reflection.findField(
        networkManager.getClass(), candidate);
      final Object value = Reflection.get(field, networkManager);
      if (value instanceof Channel channel) {
        return channel;
      }
    }

    logger.warning("Unable to locate Netty channel field in "
      + networkManager.getClass().getName());
    return null;
  }

  private static Object invokeNoArg(final Object target,
                                    final String method) {
    try {
      return target.getClass().getMethod(method).invoke(target);
    } catch (final ReflectiveOperationException exception) {
      return null;
    }
  }

  private static Object readPublic(final Object target,
                                   final String field) {
    try {
      return target.getClass().getField(field).get(target);
    } catch (final ReflectiveOperationException exception) {
      return null;
    }
  }
}