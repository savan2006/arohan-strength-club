package com.arohan.strengthclub.security;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/** Small in-memory brake on repeated failed sign-ins for the same email address. */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 8;
    private static final long WINDOW_NANOS = 10L * 60 * 1_000_000_000L; // 10 minutes
    private static final int MAX_TRACKED = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public boolean isBlocked(String email) {
        Deque<Long> attempts = failures.get(key(email));
        if (attempts == null) return false;
        synchronized (attempts) {
            prune(attempts);
            return attempts.size() >= MAX_FAILURES;
        }
    }

    public void recordFailure(String email) {
        if (failures.size() >= MAX_TRACKED) failures.clear();
        Deque<Long> attempts = failures.computeIfAbsent(key(email), ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            prune(attempts);
            attempts.addLast(System.nanoTime());
        }
    }

    public void recordSuccess(String email) { failures.remove(key(email)); }

    private static void prune(Deque<Long> attempts) {
        long cutoff = System.nanoTime() - WINDOW_NANOS;
        while (!attempts.isEmpty() && attempts.peekFirst() < cutoff) attempts.pollFirst();
    }

    private static String key(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}
