package me.kvd.hp.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProbeTracker {

  private final long windowNanos;

  public ProbeTracker(final int windowSeconds) {
    this.windowNanos = (long) windowSeconds * 1_000_000_000L;
  }

  int record(final UUID playerId, final long nowNanos) {
    final Deque<Long> deque = this.attempts.computeIfAbsent(
      playerId, ignored -> new ArrayDeque<>());
    synchronized (deque) {
      final long cutoff = nowNanos - this.windowNanos;
      while (!deque.isEmpty() && deque.peekFirst() < cutoff) {
        deque.pollFirst();
      }
      deque.addLast(nowNanos);
      return deque.size();
    }
  }

  void forget(final UUID playerId) {
    this.attempts.remove(playerId);
  }

  private final Map<UUID, Deque<Long>> attempts = new ConcurrentHashMap<>();
}