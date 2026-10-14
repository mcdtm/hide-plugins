package me.kvd.hp.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import me.kvd.hp.api.Settings;
import org.bukkit.plugin.Plugin;

/**
 * Loads {@link Settings} from {@code settings.properties} in the plugin
 * data folder. The file is created from the packaged resource on first
 * run.
 */
public final class SettingsLoader {

  private static final String FILE_NAME = "settings.properties";

  private final Plugin plugin;
  private final Logger logger;

  public SettingsLoader(final Plugin plugin, final Logger logger) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
    this.logger = Objects.requireNonNull(logger, "logger");
  }

  public Settings load() {
    this.ensureFileExists();
    final Path path = this.plugin.getDataFolder().toPath()
      .resolve(FILE_NAME);
    final Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
    } catch (final IOException exception) {
      this.logger.log(Level.SEVERE,
        "Failed to read " + FILE_NAME + "; using defaults.",
        exception);
      return Settings.defaults();
    }
    return parse(props);
  }

  private void ensureFileExists() {
    final var folder = this.plugin.getDataFolder();
    if (!folder.exists() && !folder.mkdirs() && !folder.exists()) {
      this.logger.warning("Unable to create plugin data folder.");
      return;
    }
    if (null != this.plugin.getResource(FILE_NAME)) {
      this.plugin.saveResource(FILE_NAME, false);
    }
  }

  private static Settings parse(final Properties props) {
    return Settings.builder()
      .messageMode(parseMode(props.getProperty(
        "permission.message.mode")))
      .customMessage(props.getProperty(
        "permission.message.custom",
        Settings.DEFAULT_CUSTOM_MESSAGE))
      .spoofEnabled(Boolean.parseBoolean(props.getProperty(
        "spoof.enabled", "false")))
      .spoofPlugins(parseList(props.getProperty(
        "spoof.plugins", "")))
      .spoofHeader(props.getProperty(
        "spoof.header", Settings.DEFAULT_SPOOF_HEADER))
      .debug(Boolean.parseBoolean(props.getProperty(
        "debug", "false")))
      .notificationsEnabled(Boolean.parseBoolean(props.getProperty(
        "notifications.enabled", "true")))
      .notificationThreshold(parseInt(props.getProperty(
        "notifications.threshold", "5"), 5))
      .notificationWindowSeconds(parseInt(props.getProperty(
        "notifications.window.seconds", "60"), 60))
      .build();
  }

  private static int parseInt(final String raw, final int fallback) {
    try {
      return Integer.parseInt(raw.trim());
    } catch (final NumberFormatException exception) {
      return fallback;
    }
  }

  private static Settings.MessageMode parseMode(final String raw) {
    if (null == raw) {
      return Settings.MessageMode.MINECRAFT;
    }
    try {
      return Settings.MessageMode.valueOf(
        raw.trim().toUpperCase(Locale.ROOT));
    } catch (final IllegalArgumentException exception) {
      return Settings.MessageMode.MINECRAFT;
    }
  }

  private static List<String> parseList(final String raw) {
    if (null == raw || raw.isBlank()) {
      return List.of();
    }
    final List<String> result = new ArrayList<>();
    for (final String part : raw.split(",")) {
      final String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return List.copyOf(result);
  }
}