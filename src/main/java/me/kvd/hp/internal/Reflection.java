package me.kvd.hp.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Null-safe, exception-swallowing reflective helpers.
 *
 * <p>Every method returns {@code null} (or {@code false}) instead of
 * throwing. Callers must handle the missing value explicitly. This is
 * the single place in the plugin where reflection is allowed.</p>
 */
final class Reflection {

  private Reflection() {
    throw new AssertionError("No instances.");
  }

  /**
   * Resolves a class by name, returning {@code null} when not present.
   */
  static Class<?> findClass(final String name) {
    try {
      return Class.forName(name);
    } catch (final ClassNotFoundException exception) {
      return null;
    }
  }

  /**
   * Resolves a declared field (any visibility) and marks it
   * accessible, returning {@code null} when absent.
   */
  static Field findField(final Class<?> owner, final String name) {
    if (null == owner) {
      return null;
    }
    try {
      final Field field = owner.getDeclaredField(name);
      field.setAccessible(true);
      return field;
    } catch (final NoSuchFieldException | SecurityException exception) {
      return null;
    }
  }

  /**
   * Convenience overload for {@link #findField(Class, String)} that
   * resolves the owner by name.
   */
  static Field findField(final String className, final String fieldName) {
    return findField(findClass(className), fieldName);
  }

  /**
   * Reads the field from the target, returning {@code null} on any
   * failure.
   */
  static Object get(final Field field, final Object target) {
    if (null == field || null == target) {
      return null;
    }
    try {
      return field.get(target);
    } catch (final IllegalAccessException exception) {
      return null;
    }
  }

  /**
   * Writes the value into the field, returning whether the write
   * succeeded.
   */
  static boolean set(final Field field,
                     final Object target,
                     final Object value) {
    if (null == field || null == target) {
      return false;
    }
    try {
      field.set(target, value);
      return true;
    } catch (final IllegalAccessException exception) {
      return false;
    }
  }

  /**
   * Invokes a no-argument method, returning {@code null} on any
   * failure. Tries every candidate name in order.
   */
  static Object invokeAnyNoArg(final Object target,
                               final String... candidates) {
    if (null == target || null == candidates) {
      return null;
    }
    for (final String name : candidates) {
      try {
        final Method method = target.getClass().getMethod(name);
        return method.invoke(target);
      } catch (final ReflectiveOperationException ignored) {
        // Try the next candidate.
      }
    }
    return null;
  }

  /**
   * Invokes a static, single-String-argument method, returning
   * {@code null} on any failure. Tries every candidate name in order.
   */
  static Object invokeStaticStringArg(final String className,
                                      final String argument,
                                      final String... candidates) {
    final Class<?> owner = findClass(className);
    if (null == owner) {
      return null;
    }
    for (final String name : candidates) {
      try {
        final Method method = owner.getMethod(name, String.class);
        return method.invoke(null, argument);
      } catch (final ReflectiveOperationException ignored) {
        // Try the next candidate.
      }
    }
    return null;
  }
}