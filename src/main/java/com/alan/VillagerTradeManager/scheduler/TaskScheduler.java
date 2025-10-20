package com.alan.VillagerTradeManager.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Unified scheduler interface for cross-platform compatibility
 */
public interface TaskScheduler {

    /**
     * Run a task synchronously on the main thread
     */
    void runSync(Runnable task);

    /**
     * Run a task synchronously on the main thread with delay
     */
    void runSyncLater(Runnable task, long delayTicks);

    /**
     * Run a repeating task synchronously on the main thread
     */
    void runSyncRepeating(Runnable task, long delayTicks, long periodTicks);

    /**
     * Run a task asynchronously
     */
    void runAsync(Runnable task);

    /**
     * Run a task asynchronously with delay
     */
    void runAsyncLater(Runnable task, long delayTicks);

    /**
     * Run a repeating task asynchronously
     */
    void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks);

    /**
     * Run a task synchronously in the region thread (Folia only)
     * For non-Folia platforms, runs on main thread
     */
    void runSyncForEntity(Entity entity, Runnable task);

    /**
     * Run a task synchronously in the region thread with delay (Folia only)
     */
    void runSyncLaterForEntity(Entity entity, Runnable task, long delayTicks);

    /**
     * Run a task synchronously in the region thread (Folia only)
     * For non-Folia platforms, runs on main thread
     */
    void runSyncAtLocation(Location location, Runnable task);

    /**
     * Run a task synchronously in the region thread with delay (Folia only)
     */
    void runSyncLaterAtLocation(Location location, Runnable task, long delayTicks);

    /**
     * Cancel all tasks for this plugin
     */
    void cancelAllTasks();

    /**
     * Get the name of this scheduler implementation
     */
    String getSchedulerName();
}