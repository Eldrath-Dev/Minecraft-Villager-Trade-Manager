package com.alan.VillagerTradeManager.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Wrapper for traditional Bukkit scheduler (Paper, Spigot, etc.)
 */
public class BukkitSchedulerWrapper implements TaskScheduler {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    public BukkitSchedulerWrapper(Plugin plugin) {
        this.plugin = plugin;
        this.scheduler = Bukkit.getScheduler();
    }

    @Override
    public void runSync(Runnable task) {
        scheduler.runTask(plugin, task);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        scheduler.runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        scheduler.runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void runAsync(Runnable task) {
        scheduler.runTaskAsynchronously(plugin, task);
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        scheduler.runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    @Override
    public void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        scheduler.runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void runSyncForEntity(Entity entity, Runnable task) {
        // For Bukkit platforms, run on main thread
        runSync(task);
    }

    @Override
    public void runSyncLaterForEntity(Entity entity, Runnable task, long delayTicks) {
        // For Bukkit platforms, run on main thread
        runSyncLater(task, delayTicks);
    }

    @Override
    public void runSyncAtLocation(Location location, Runnable task) {
        // For Bukkit platforms, run on main thread
        runSync(task);
    }

    @Override
    public void runSyncLaterAtLocation(Location location, Runnable task, long delayTicks) {
        // For Bukkit platforms, run on main thread
        runSyncLater(task, delayTicks);
    }

    @Override
    public void cancelAllTasks() {
        scheduler.cancelTasks(plugin);
    }

    @Override
    public String getSchedulerName() {
        return "BukkitScheduler";
    }
}