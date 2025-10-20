package com.alan.VillagerTradeManager.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Scheduler implementation for Folia (region-threaded)
 */
public class FoliaScheduler implements TaskScheduler {

    private final Plugin plugin;

    public FoliaScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void runSync(Runnable task) {
        // On Folia, we need to run on the global region scheduler for global tasks
        try {
            Bukkit.getGlobalRegionScheduler().execute(plugin, task);
        } catch (Exception e) {
            // Fallback to Bukkit scheduler if Folia API is not available
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        try {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delayTicks);
        } catch (Exception e) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    @Override
    public void runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        try {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), delayTicks, periodTicks);
        } catch (Exception e) {
            Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
        }
    }

    @Override
    public void runAsync(Runnable task) {
        try {
            Bukkit.getAsyncScheduler().runNow(plugin, (scheduledTask) -> task.run());
        } catch (Exception e) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    @Override
    public void runAsyncLater(Runnable task, long delayTicks) {
        long delayMillis = (delayTicks * 50); // Convert ticks to milliseconds
        try {
            Bukkit.getAsyncScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), delayMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
        }
    }

    @Override
    public void runAsyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        long delayMillis = (delayTicks * 50); // Convert ticks to milliseconds
        long periodMillis = (periodTicks * 50); // Convert ticks to milliseconds
        try {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (scheduledTask) -> task.run(), delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
        }
    }

    @Override
    public void runSyncForEntity(Entity entity, Runnable task) {
        try {
            entity.getScheduler().run(plugin, (scheduledTask) -> task.run(), null);
        } catch (Exception e) {
            runSync(task);
        }
    }

    @Override
    public void runSyncLaterForEntity(Entity entity, Runnable task, long delayTicks) {
        try {
            entity.getScheduler().runDelayed(plugin, (scheduledTask) -> task.run(), null, delayTicks);
        } catch (Exception e) {
            runSyncLater(task, delayTicks);
        }
    }

    @Override
    public void runSyncAtLocation(Location location, Runnable task) {
        try {
            Bukkit.getRegionScheduler().run(plugin, location, (scheduledTask) -> task.run());
        } catch (Exception e) {
            runSync(task);
        }
    }

    @Override
    public void runSyncLaterAtLocation(Location location, Runnable task, long delayTicks) {
        try {
            Bukkit.getRegionScheduler().runDelayed(plugin, location, (scheduledTask) -> task.run(), delayTicks);
        } catch (Exception e) {
            runSyncLater(task, delayTicks);
        }
    }

    @Override
    public void cancelAllTasks() {
        try {
            // Cancel global region tasks
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);

            // Cancel async tasks
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
        } catch (Exception e) {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }

    @Override
    public String getSchedulerName() {
        return "FoliaRegionScheduler";
    }
}