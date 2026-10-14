package me.kvd.hp.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import me.kvd.hp.api.Settings;

public final class PluginListSpoofer {

  private volatile Settings settings;
  private final PluginNameCache cache;

  public PluginListSpoofer(final Settings settings, final PluginNameCache cache) {
    this.settings = Objects.requireNonNull(settings, "settings");
    this.cache = Objects.requireNonNull(cache, "cache");
  }

  void reload(final Settings settings) {
    this.settings = Objects.requireNonNull(settings, "settings");
  }

  public boolean isEnabled() {
    final Settings current = this.settings;
    return current.spoofEnabled() && !current.spoofPlugins().isEmpty();
  }

  String renderPluginsLine() {
    final Settings current = this.settings;
    final List<String> plugins = current.spoofPlugins();
    final String header = current.spoofHeader().replace(
      "%d", String.valueOf(plugins.size()));
    final StringBuilder sb = new StringBuilder("\u00A76");
    sb.append(header).append(" \u00A7r");
    for (int i = 0; i < plugins.size(); i++) {
      if (0 < i) {
        sb.append("\u00A7r, ");
      }
      sb.append("\u00A7a").append(plugins.get(i));
    }
    return sb.toString();
  }

  List<String> fakeNames() {
    return this.settings.spoofPlugins();
  }

  Set<String> realNames() {
    return this.cache.names();
  }

  String spoofNames(final String text) {
    if (!isEnabled() || null == text || text.isEmpty()) {
      return text;
    }
    final List<String> realNames = List.copyOf(this.cache.names());
    if (realNames.isEmpty()) {
      return text;
    }
    final List<String> fakes = fakeNames();
    String result = text;
    for (int i = 0; i < realNames.size(); i++) {
      final String realName = realNames.get(i);
      if (!result.contains(realName)) {
        continue;
      }
      result = result.replace(realName, fakes.get(i % fakes.size()));
    }
    return result;
  }

  String[] fakeSuggestionsFor(final String hiddenCommand) {
    final List<String> fakes = fakeNames();
    if (fakes.isEmpty()) {
      return new String[0];
    }
    if ("help".equals(hiddenCommand) || "?".equals(hiddenCommand)) {
      final List<String> pages = new ArrayList<>(fakes.size() + 1);
      pages.add("1");
      pages.addAll(fakes);
      return pages.toArray(new String[0]);
    }
    return fakes.toArray(new String[0]);
  }

  String matchHiddenPrefix(final String rawInput,
                           final Set<String> hiddenCommands) {
    if (null == rawInput || rawInput.isEmpty()) {
      return null;
    }
    String value = rawInput;
    if (value.startsWith("/")) {
      value = value.substring(1);
    }
    final int space = value.indexOf(' ');
    if (0 <= space) {
      value = value.substring(0, space);
    }
    final String normalized = value.toLowerCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      return null;
    }
    if (hiddenCommands.contains(normalized)) {
      return normalized;
    }
    final int colon = normalized.indexOf(':');
    if (0 < colon) {
      final String tail = normalized.substring(colon + 1);
      if (hiddenCommands.contains(tail)) {
        return tail;
      }
    }
    return null;
  }
}