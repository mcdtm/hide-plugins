package me.kvd.hp.api;

import java.util.Set;

public final class HidePluginsDefaults {

  public static final Set<String> HIDDEN_COMMANDS = Set.of(
    "plugins", "pl", "about", "ver", "version", "icanhasbukkit",
    "help", "?",
    "bukkit:plugins", "bukkit:pl", "bukkit:about", "bukkit:ver",
    "bukkit:version", "bukkit:icanhasbukkit", "bukkit:help", "bukkit:?"
  );

  public static final Set<String> REVEALING_COMMANDS = Set.of(
    "plugins", "pl", "about", "ver", "version", "icanhasbukkit"
  );

  public static final String BYPASS_PERMISSION = "hideplugins.bypass";
  public static final String BYPASS_PLUGINS_PERMISSION =
    "hideplugins.bypass.plugins";
  public static final String BYPASS_HELP_PERMISSION =
    "hideplugins.bypass.help";
  public static final String VANILLA_BYPASS_PERMISSION =
    "bukkit.command.plugins";
  public static final String ADMIN_PERMISSION = "hideplugins.admin";
  public static final String NOTIFY_PERMISSION = "hideplugins.notify";

  private HidePluginsDefaults() {
    throw new AssertionError("No instances.");
  }
}