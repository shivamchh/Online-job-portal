package com.example.Online_job_portal.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe service for mitigating brute-force and credential-stuffing attacks.
 * Tracks failed login attempts per key (email + IP) and enforces temporary lockout.
 */
@Service
public class LoginAttemptService {

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MILLIS = 15 * 60 * 1000; // 15 minutes

    public static class AttemptRecord {
        private final AtomicInteger attempts = new AtomicInteger(0);
        private volatile long lastAttemptTimestamp = System.currentTimeMillis();

        public int increment() {
            lastAttemptTimestamp = System.currentTimeMillis();
            return attempts.incrementAndGet();
        }

        public void reset() {
            attempts.set(0);
            lastAttemptTimestamp = System.currentTimeMillis();
        }

        public int getAttempts() {
            return attempts.get();
        }

        public long getLastAttemptTimestamp() {
            return lastAttemptTimestamp;
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        if (key != null) {
            attemptsCache.remove(normalizeKey(key));
        }
    }

    public void loginFailed(String key) {
        if (key == null) return;
        String normalized = normalizeKey(key);
        attemptsCache.compute(normalized, (k, existing) -> {
            if (existing == null) {
                AttemptRecord record = new AttemptRecord();
                record.increment();
                return record;
            }
            // If lockout period has expired since last attempt, reset and restart count
            if (System.currentTimeMillis() - existing.getLastAttemptTimestamp() > LOCKOUT_DURATION_MILLIS) {
                existing.reset();
            }
            existing.increment();
            return existing;
        });
    }

    public boolean isBlocked(String key) {
        if (key == null) return false;
        AttemptRecord record = attemptsCache.get(normalizeKey(key));
        if (record == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - record.getLastAttemptTimestamp();
        if (elapsed > LOCKOUT_DURATION_MILLIS) {
            attemptsCache.remove(normalizeKey(key));
            return false;
        }

        return record.getAttempts() >= MAX_ATTEMPTS;
    }

    public int getRemainingLockoutMinutes(String key) {
        if (key == null) return 0;
        AttemptRecord record = attemptsCache.get(normalizeKey(key));
        if (record == null || record.getAttempts() < MAX_ATTEMPTS) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - record.getLastAttemptTimestamp();
        long remainingMillis = LOCKOUT_DURATION_MILLIS - elapsed;
        return remainingMillis > 0 ? (int) Math.ceil(remainingMillis / 60000.0) : 0;
    }

    public void reset(String key) {
        if (key != null) {
            attemptsCache.remove(normalizeKey(key));
        }
    }

    public void clearAll() {
        attemptsCache.clear();
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase();
    }
}
