package me.kvd.hp.api;

import java.util.List;
import java.util.Objects;

public record Settings(
  MessageMode messageMode,
  String customMessage,
  boolean spoofEnabled,
  List<String> spoofPlugins,
  String spoofHeader,
  boolean debug,
  boolean notificationsEnabled,
  int notificationThreshold,
  int notificationWindowSeconds
) {

  public enum MessageMode {
    MINECRAFT, BUKKIT, CUSTOM
  }

  public static final String DEFAULT_CUSTOM_MESSAGE =
    "&cYou are not permitted to use this command.";
  public static final String DEFAULT_SPOOF_HEADER = "Plugins (%d):";

  public Settings {
    Objects.requireNonNull(messageMode, "messageMode");
    Objects.requireNonNull(customMessage, "customMessage");
    Objects.requireNonNull(spoofPlugins, "spoofPlugins");
    Objects.requireNonNull(spoofHeader, "spoofHeader");
    spoofPlugins = List.copyOf(spoofPlugins);
    if (1 > notificationThreshold) {
      throw new IllegalArgumentException(
        "notificationThreshold must be >= 1");
    }
    if (1 > notificationWindowSeconds) {
      throw new IllegalArgumentException(
        "notificationWindowSeconds must be >= 1");
    }
  }

  public static Settings defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private MessageMode messageMode = MessageMode.MINECRAFT;
    private String customMessage = DEFAULT_CUSTOM_MESSAGE;
    private boolean spoofEnabled;
    private List<String> spoofPlugins = List.of();
    private String spoofHeader = DEFAULT_SPOOF_HEADER;
    private boolean debug;
    private boolean notificationsEnabled = true;
    private int notificationThreshold = 5;
    private int notificationWindowSeconds = 60;

    public Builder messageMode(final MessageMode value) {
      this.messageMode = value;
      return this;
    }

    public Builder customMessage(final String value) {
      this.customMessage = value;
      return this;
    }

    public Builder spoofEnabled(final boolean value) {
      this.spoofEnabled = value;
      return this;
    }

    public Builder spoofPlugins(final List<String> value) {
      this.spoofPlugins = value;
      return this;
    }

    public Builder spoofHeader(final String value) {
      this.spoofHeader = value;
      return this;
    }

    public Builder debug(final boolean value) {
      this.debug = value;
      return this;
    }

    public Builder notificationsEnabled(final boolean value) {
      this.notificationsEnabled = value;
      return this;
    }

    public Builder notificationThreshold(final int value) {
      this.notificationThreshold = value;
      return this;
    }

    public Builder notificationWindowSeconds(final int value) {
      this.notificationWindowSeconds = value;
      return this;
    }

    public Settings build() {
      return new Settings(
        this.messageMode, this.customMessage,
        this.spoofEnabled, this.spoofPlugins, this.spoofHeader,
        this.debug, this.notificationsEnabled,
        this.notificationThreshold, this.notificationWindowSeconds);
    }
  }
}