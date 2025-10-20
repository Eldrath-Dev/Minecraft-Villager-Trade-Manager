package com.alan.VillagerTradeManager.platform;

import com.alan.VillagerTradeManager.scheduler.BukkitSchedulerWrapper;
import com.alan.VillagerTradeManager.scheduler.FoliaScheduler;
import com.alan.VillagerTradeManager.scheduler.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Service to detect the current server platform and provide appropriate scheduler
 */
public class PlatformService {

    private final Plugin plugin;
    private PlatformType detectedPlatform;
    private TaskScheduler scheduler;

    public PlatformService(Plugin plugin) {
        this.plugin = plugin;
        detectPlatform();
        initializeScheduler();
    }

    private void detectPlatform() {
        String serverName = Bukkit.getName();
        String version = Bukkit.getVersion();

        // Debug logging to see what we're actually working with
        plugin.getLogger().info("Server Name: " + (serverName != null ? serverName : "null"));
        plugin.getLogger().info("Server Version: " + (version != null ? version : "null"));

        // Check for Folia - but be more careful about it
        if (isFolia()) {
            detectedPlatform = PlatformType.FOLIA;
            plugin.getLogger().info("Platform Detected: Folia (Region Threaded)");
            return;
        }

        // Check for Purpur
        if (isPurpur()) {
            detectedPlatform = PlatformType.PURPUR;
            plugin.getLogger().info("Platform Detected: Purpur");
            return;
        }

        // Check for Paper
        if (isPaper()) {
            detectedPlatform = PlatformType.PAPER;
            plugin.getLogger().info("Platform Detected: Paper");
            return;
        }

        // Default to Spigot/Bukkit
        detectedPlatform = PlatformType.SPIGOT;
        plugin.getLogger().info("Platform Detected: Spigot/Bukkit");
    }

    private boolean isFolia() {
        try {
            // Check if we're actually running on Folia by checking for Folia-specific classes
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");

            // Additional check - Folia typically has "Folia" in the server name
            String serverName = Bukkit.getName();
            if (serverName != null && serverName.contains("Folia")) {
                return true;
            }

            // If we're on a Paper server that has region threading, it might be Folia in disguise
            // But let's be conservative and only say it's Folia if we're sure
            String version = Bukkit.getVersion();
            return version != null && version.contains("Folia");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isPaper() {
        // Check if it's Paper (but not Folia)
        String serverName = Bukkit.getName();
        String version = Bukkit.getVersion();

        // Paper typically has "Paper" in name but not "Folia"
        boolean hasPaper = serverName != null && serverName.contains("Paper");
        boolean noFolia = serverName == null || !serverName.contains("Folia");
        boolean versionNoFolia = version == null || !version.contains("Folia");

        return hasPaper && noFolia && versionNoFolia;
    }

    private boolean isPurpur() {
        // Check for Purpur
        String serverName = Bukkit.getName();
        String version = Bukkit.getVersion();
        return (serverName != null && serverName.contains("Purpur")) ||
                (version != null && version.contains("Purpur"));
    }

    private void initializeScheduler() {
        switch (detectedPlatform) {
            case FOLIA:
                try {
                    scheduler = new FoliaScheduler(plugin);
                    plugin.getLogger().info("Scheduler: Folia RegionScheduler initialized");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to initialize Folia scheduler, falling back to Bukkit: " + e.getMessage());
                    scheduler = new BukkitSchedulerWrapper(plugin);
                    plugin.getLogger().info("Scheduler: BukkitScheduler fallback initialized");
                }
                break;
            default:
                scheduler = new BukkitSchedulerWrapper(plugin);
                plugin.getLogger().info("Scheduler: BukkitScheduler initialized");
                break;
        }
    }

    public PlatformType getDetectedPlatform() {
        return detectedPlatform;
    }

    public TaskScheduler getScheduler() {
        return scheduler;
    }

    public boolean isFoliaPlatform() {
        return detectedPlatform == PlatformType.FOLIA;
    }

    public enum PlatformType {
        FOLIA,
        PAPER,
        PURPUR,
        SPIGOT
    }
}