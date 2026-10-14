package me.kvd.hp.internal;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import me.kvd.hp.api.HidePluginsDefaults;
import net.minecraft.util.io.netty.channel.ChannelDuplexHandler;
import net.minecraft.util.io.netty.channel.ChannelHandlerContext;
import net.minecraft.util.io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PacketInterceptor extends ChannelDuplexHandler {

  private static final String HANDLER_NAME = "hide_plugins_tab";
  private static final String NMS_PACKAGE = "net.minecraft.server.v1_7_R4.";

  private static final String INCOMING_TAB =
    NMS_PACKAGE + "PacketPlayInTabComplete";
  private static final String OUTGOING_TAB =
    NMS_PACKAGE + "PacketPlayOutTabComplete";
  private static final String OUTGOING_CHAT =
    NMS_PACKAGE + "PacketPlayOutChat";
  private static final String CHAT_COMPONENT_TEXT =
    NMS_PACKAGE + "ChatComponentText";

  private static final String PAYLOAD_FIELD = "a";

  /**
   * Precompiled: matches the vanilla {@code /plugins} header line.
   */
  private static final Pattern PLUGINS_HEADER = Pattern.compile(
    ".*Plugins \\(\\d+\\):.*");

  private final Player player;
  private final PluginListSpoofer spoofer;
  private final PlayerOverrides overrides;
  private final Logger logger;

  private final Field incomingTabField;
  private final Field outgoingTabField;
  private final Field outgoingChatField;

  private final AtomicLong chatFilterExpiryNanos = new AtomicLong();
  private volatile String lastTabInput;

  public PacketInterceptor(final Player player,
                           final PluginListSpoofer spoofer,
                           final PlayerOverrides overrides,
                           final Logger logger) {
    this.player = Objects.requireNonNull(player, "player");
    this.spoofer = Objects.requireNonNull(spoofer, "spoofer");
    this.overrides = Objects.requireNonNull(overrides, "overrides");
    this.logger = Objects.requireNonNull(logger, "logger");

    this.incomingTabField = Reflection.findField(
      INCOMING_TAB, PAYLOAD_FIELD);
    this.outgoingTabField = Reflection.findField(
      OUTGOING_TAB, PAYLOAD_FIELD);
    this.outgoingChatField = Reflection.findField(
      OUTGOING_CHAT, PAYLOAD_FIELD);
  }

  public void startChatFilter(final Duration duration) {
    final long newExpiry = System.nanoTime() + duration.toNanos();
    this.chatFilterExpiryNanos.updateAndGet(
      current -> Math.max(current, newExpiry));
  }

  private boolean isChatFilterActive() {
    return System.nanoTime() < this.chatFilterExpiryNanos.get();
  }

  // ------------------------------------------------------------------
  // Netty callbacks
  // ------------------------------------------------------------------

  @Override
  public void channelRead(final ChannelHandlerContext context,
                          final Object message) throws Exception {
    SafeExecution.run(this.logger,
      "Filter incoming packet for " + this.player.getName(),
      () -> this.rewriteIncomingTab(message));
    super.channelRead(context, message);
  }

  @Override
  public void write(final ChannelHandlerContext context,
                    final Object message,
                    final ChannelPromise promise) throws Exception {
    try {
      if (this.shouldDropChat(message)) {
        promise.setSuccess();
        return;
      }
      if (this.shouldRewriteChat(message)) {
        this.rewriteChat(message);
      }
      if (this.shouldProcessOutgoingTab(message)) {
        this.processOutgoingTab(message);
      }
    } catch (final RuntimeException exception) {
      this.logger.log(Level.SEVERE,
        "Failed to filter outgoing packet for "
          + this.player.getName(),
        exception);
    }
    super.write(context, message, promise);
  }

  // ------------------------------------------------------------------
  // Chat processing
  // ------------------------------------------------------------------

  private boolean shouldDropChat(final Object message) {
    if (!this.isChatFilterActive() || this.hasHelpBypass()) {
      return false;
    }
    if (this.isSpoofActive() || null == this.outgoingChatField) {
      return false;
    }
    if (!isType(message, OUTGOING_CHAT)) {
      return false;
    }
    final String text = extractText(
      Reflection.get(this.outgoingChatField, message));
    return null != text && this.isLeakingLine(text);
  }

  private boolean shouldRewriteChat(final Object message) {
    if (!this.isChatFilterActive() || this.hasHelpBypass()) {
      return false;
    }
    if (!this.isSpoofActive() || null == this.outgoingChatField) {
      return false;
    }
    if (!isType(message, OUTGOING_CHAT)) {
      return false;
    }
    final String text = extractText(
      Reflection.get(this.outgoingChatField, message));
    return null != text && this.isLeakingLine(text);
  }

  private void rewriteChat(final Object message) throws Exception {
    final String text = extractText(
      Reflection.get(this.outgoingChatField, message));
    if (null == text) {
      return;
    }
    final String replaced = PLUGINS_HEADER.matcher(text).matches()
      ? this.spoofer.renderPluginsLine()
      : this.spoofer.spoofNames(text);
    if (replaced.equals(text)) {
      return;
    }
    final Object component = buildTextComponent(replaced);
    if (null != component) {
      Reflection.set(this.outgoingChatField, message, component);
    }
  }

  private static Object buildTextComponent(final String text) {
    try {
      final Class<?> clazz = Class.forName(CHAT_COMPONENT_TEXT);
      return clazz.getConstructor(String.class).newInstance(text);
    } catch (final ReflectiveOperationException exception) {
      return null;
    }
  }

  private boolean isLeakingLine(final String text) {
    for (final String name : this.spoofer.realNames()) {
      if (LegacyText.containsWord(text, name)) {
        return true;
      }
    }
    for (final String hidden : HidePluginsDefaults.REVEALING_COMMANDS) {
      if (text.contains("/" + hidden)) {
        return true;
      }
    }
    return false;
  }

  private static String extractText(final Object component) {
    if (null == component) {
      return null;
    }
    final StringBuilder sb = new StringBuilder();
    appendText(component, sb);
    if (0 < sb.length()) {
      return sb.toString();
    }
    final String fallback = component.toString();
    return (null == fallback || fallback.isEmpty()) ? null : fallback;
  }

  private static void appendText(final Object component,
                                 final StringBuilder sb) {
    if (null == component) {
      return;
    }
    final Object own = Reflection.invokeAnyNoArg(
      component, "getText", "c");
    if (own instanceof String s && !s.isEmpty()) {
      sb.append(s);
    }
    final Object siblings = Reflection.invokeAnyNoArg(
      component, "getSiblings", "a");
    if (siblings instanceof Iterable<?> iterable) {
      for (final Object child : iterable) {
        appendText(child, sb);
      }
    }
  }

  // ------------------------------------------------------------------
  // Tab-completion processing
  // ------------------------------------------------------------------

  private void rewriteIncomingTab(final Object message) {
    if (!isType(message, INCOMING_TAB) || null == this.incomingTabField) {
      return;
    }
    final Object raw = Reflection.get(this.incomingTabField, message);
    if (!(raw instanceof String text)) {
      return;
    }
    this.lastTabInput = text;

    if (this.isSpoofActive()) {
      return;
    }
    if (!text.startsWith("/")) {
      return;
    }
    final String command = CommandNames.normalize(text);
    if (!HidePluginsDefaults.HIDDEN_COMMANDS.contains(command)) {
      return;
    }
    if (Permissions.hasBypass(this.player, command)) {
      return;
    }
    Reflection.set(this.incomingTabField, message, " " + text);
  }

  private boolean shouldProcessOutgoingTab(final Object message) {
    if (null == this.outgoingTabField) {
      return false;
    }
    return isType(message, OUTGOING_TAB);
  }

  private void processOutgoingTab(final Object message) {
    final Object raw = Reflection.get(this.outgoingTabField, message);
    if (!(raw instanceof String[] suggestions)) {
      return;
    }
    if (this.isSpoofActive()) {
      this.processSpoofTab(message, suggestions);
    } else {
      this.processHideTab(message, suggestions);
    }
  }

  private void processHideTab(final Object message,
                              final String[] suggestions) {
    if (0 == suggestions.length) {
      return;
    }
    final List<String> kept = new ArrayList<>(suggestions.length);
    for (final String candidate : suggestions) {
      if (null == candidate) {
        continue;
      }
      final String normalized = CommandNames.normalize(candidate);
      if (Permissions.hasBypass(this.player, normalized)) {
        kept.add(candidate);
        continue;
      }
      if (this.isHiddenCandidate(normalized)) {
        continue;
      }
      kept.add(candidate);
    }
    if (kept.size() == suggestions.length) {
      return;
    }
    Reflection.set(this.outgoingTabField, message,
      kept.toArray(new String[0]));
  }

  private void processSpoofTab(final Object message,
                               final String[] suggestions) {
    final Set<String> realNames = this.spoofer.realNames();
    final List<String> fakes = this.spoofer.fakeNames();
    boolean touched = false;

    if (0 < suggestions.length && !realNames.isEmpty()) {
      for (int i = 0; i < suggestions.length; i++) {
        final String candidate = suggestions[i];
        if (null != candidate && realNames.contains(candidate)) {
          suggestions[i] = fakes.get(i % fakes.size());
          touched = true;
        }
      }
    }

    if (0 == suggestions.length) {
      final String hidden = this.spoofer.matchHiddenPrefix(
        this.lastTabInput, HidePluginsDefaults.HIDDEN_COMMANDS);
      if (null != hidden && !Permissions.hasBypass(this.player, hidden)) {
        final String[] injected =
          this.spoofer.fakeSuggestionsFor(hidden);
        if (0 < injected.length) {
          Reflection.set(this.outgoingTabField, message, injected);
          return;
        }
      }
    }

    if (touched) {
      Reflection.set(this.outgoingTabField, message, suggestions);
    }
  }

  private boolean isHiddenCandidate(final String normalized) {
    if (normalized.isEmpty()) {
      return false;
    }
    if (HidePluginsDefaults.HIDDEN_COMMANDS.contains(normalized)) {
      return true;
    }
    final int colon = normalized.indexOf(':');
    if (0 < colon) {
      final String tail = normalized.substring(colon + 1);
      if (HidePluginsDefaults.HIDDEN_COMMANDS.contains(tail)) {
        return true;
      }
    }
    return null != Bukkit.getPluginCommand(normalized);
  }

  // ------------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------------

  private boolean hasHelpBypass() {
    return Permissions.hasAnyBypass(this.player);
  }

  private boolean isSpoofActive() {
    if (Permissions.hasAnyBypass(this.player)) {
      return false;
    }
    final Boolean override = this.overrides.get(
      this.player.getUniqueId());
    if (null != override) {
      return override;
    }
    return this.spoofer.isEnabled();
  }

  private static boolean isType(final Object message, final String className) {
    return null != message
      && message.getClass().getName().equals(className);
  }

  public static String handlerName() {
    return HANDLER_NAME;
  }
}