package me.kvd.hp.internal;

/**
 * Utilities for legacy-formatted Minecraft text ({@code §}-codes).
 */
final class LegacyText {

  private static final char COLOR_CODE = '\u00A7';

  private LegacyText() {
    throw new AssertionError("No instances.");
  }

  /**
   * Removes every legacy formatting code ({@code §} followed by any
   * character) from the given text.
   *
   * @param text the input; may be {@code null}
   * @return the stripped text, or an empty string for {@code null}
   */
  static String stripFormatting(final String text) {
    if (null == text || text.isEmpty()) {
      return "";
    }
    final StringBuilder sb = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      final char c = text.charAt(i);
      if (COLOR_CODE == c && i + 1 < text.length()) {
        i++;
        continue;
      }
      sb.append(c);
    }
    return sb.toString();
  }

  /**
   * Case-sensitive word-boundary search after stripping legacy
   * formatting codes. A word boundary is any position adjacent to a
   * non-alphanumeric character (or the start/end of the string).
   *
   * @param haystack the text to search
   * @param needle   the word to look for
   * @return {@code true} when {@code needle} occurs as a whole word
   */
  static boolean containsWord(final String haystack,
                              final String needle) {
    if (null == haystack || null == needle || needle.isEmpty()) {
      return false;
    }
    final String stripped = stripFormatting(haystack);
    int index = 0;
    while (0 <= (index = stripped.indexOf(needle, index))) {
      final boolean startOk = 0 == index
        || !Character.isLetterOrDigit(stripped.charAt(index - 1));
      final int end = index + needle.length();
      final boolean endOk = end >= stripped.length()
        || !Character.isLetterOrDigit(stripped.charAt(end));
      if (startOk && endOk) {
        return true;
      }
      index = end;
    }
    return false;
  }
}