package me.kvd.hp.internal;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

/**
 * Read-only reflective access to the server's {@link SimpleCommandMap}
 * and its {@code knownCommands} registry.
 *
 * <p>Bukkit 1.7.10 does not expose {@code Bukkit.getCommandMap()}; the
 * map is obtained from the {@code commandMap} field of the underlying
 * {@code CraftServer} implementation. The accessor never mutates the
 * registry, so players with the bypass permission keep full access to
 * every command.</p>
 */
public final class CommandMapAccessor {

  private static final String COMMAND_MAP_FIELD = "commandMap";
  private static final String KNOWN_COMMANDS_FIELD = "knownCommands";

  private final Map<String, Command> knownCommands;

  public CommandMapAccessor(final Logger logger) {
    Objects.requireNonNull(logger, "logger");
    this.knownCommands = resolve(logger);
  }

  /**
   * Returns the live {@code knownCommands} map, or an empty map when
   * the server implementation does not expose it.
   *
   * @return the known commands registry; never {@code null}
   */
  Map<String, Command> knownCommands() {
    return this.knownCommands;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Command> resolve(final Logger logger) {
    final Object server = Bukkit.getServer();
    if (null == server) {
      return Collections.emptyMap();
    }

    Field field = Reflection.findField(
      server.getClass(), COMMAND_MAP_FIELD);
    if (null == field) {
      field = Reflection.findField(
        server.getClass().getSuperclass(), COMMAND_MAP_FIELD);
    }
    final Object commandMap = Reflection.get(field, server);
    if (!(commandMap instanceof SimpleCommandMap)) {
      logger.warning(
        "commandMap field is not a SimpleCommandMap; "
          + "help listing will be limited.");
      return Collections.emptyMap();
    }

    final Field known = Reflection.findField(
      SimpleCommandMap.class, KNOWN_COMMANDS_FIELD);
    final Object value = Reflection.get(known, commandMap);
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Command>) map;
    }

    logger.warning(
      "Unable to read SimpleCommandMap#knownCommands; "
        + "help listing will be limited.");
    return Collections.emptyMap();
  }
}