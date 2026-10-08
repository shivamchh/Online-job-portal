package com.example.Online_job_portal.util;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency & Thread Manager for Asynchronous Job Portal Tasks.
 * Visibly demonstrates core Java multithreading and synchronization constructs:
 *  1. ExecutorService (Fixed thread pool for parallel notification and background work)
 *  2. AtomicInteger (Thread-safe lockless task counters)
 *  3. ConcurrentHashMap (Thread-safe concurrent data tracking)
 *  4. synchronized keyword (Critical section mutual exclusion)
 */
@Component
public class ThreadManager {

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final AtomicInteger activeTasksCount = new AtomicInteger(0);
    private final AtomicInteger totalExecutedCount = new AtomicInteger(0);
    private final Map<String, Long> taskExecutionTimestamps = new ConcurrentHashMap<>();

    private int peakConcurrentTasks = 0;

    /**
     * Executes a background task while atomically tracking active concurrency.
     */
    public void executeTask(Runnable task) {
        activeTasksCount.incrementAndGet();
        recordPeakConcurrency();

        executorService.submit(() -> {
            try {
                task.run();
            } finally {
                activeTasksCount.decrementAndGet();
                totalExecutedCount.incrementAndGet();
            }
        });
    }

    public <T> Future<T> submitTask(Callable<T> task) {
        activeTasksCount.incrementAndGet();
        recordPeakConcurrency();

        return executorService.submit(() -> {
            try {
                return task.call();
            } finally {
                activeTasksCount.decrementAndGet();
                totalExecutedCount.incrementAndGet();
            }
        });
    }

    /**
     * Demonstrates explicit thread synchronization via the `synchronized` keyword
     * to protect shared mutable state in a multithreaded environment.
     */
    public synchronized void recordPeakConcurrency() {
        int current = activeTasksCount.get();
        if (current > peakConcurrentTasks) {
            peakConcurrentTasks = current;
        }
    }

    public synchronized int getPeakConcurrentTasks() {
        return peakConcurrentTasks;
    }

    public int getActiveTasksCount() {
        return activeTasksCount.get();
    }

    public int getTotalExecutedCount() {
        return totalExecutedCount.get();
    }

    public Map<String, Long> getTaskExecutionTimestamps() {
        return taskExecutionTimestamps;
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
