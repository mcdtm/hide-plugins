package me.kvd.hp.internal;

import java.util.Locale;

/**
 * Utility methods for normalizing command strings shared by the
 * command and tab-complete interceptors.
 */
final class CommandNames {

    private CommandNames() {
        throw new AssertionError("No instances.");
    }

    /**
     * Reduces a raw input line to its canonical form: trimmed,
     * lowercase, without a leading slash and without arguments.
     *
     * <pre>
     *   "/plugins"          -&gt; "plugins"
     *   "/PLUGINS arg"      -&gt; "plugins"
     *   "/bukkit:plugins"   -&gt; "bukkit:plugins"
     *   "  /pl  "           -&gt; "pl"
     *   null                -&gt; ""
     * </pre>
     *
     * @param raw the raw input; may be {@code null}
     * @return the normalized command name, never {@code null}
     */
    static String normalize(final String raw) {
        if (null == raw) {
            return "";
        }
        String value = raw.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        final int space = value.indexOf(' ');
        if (0 <= space) {
            value = value.substring(0, space);
        }
        return value.toLowerCase(Locale.ROOT);
    }
}