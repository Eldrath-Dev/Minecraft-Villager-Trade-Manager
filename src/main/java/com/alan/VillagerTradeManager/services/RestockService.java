package com.alan.VillagerTradeManager.services;

import com.alan.VillagerTradeManager.VillagerTradeManager;
import com.alan.VillagerTradeManager.platform.PlatformService;
import com.alan.VillagerTradeManager.scheduler.TaskScheduler;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantRecipe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling villager restock functionality
 */
public class RestockService {

    private final VillagerTradeManager plugin;
    private final Connection databaseConnection;
    private final com.alan.VillagerTradeManager.database.AsyncDatabaseExecutor databaseExecutor;

    // Restock settings
    private boolean customRestockEnabled = false;
    private int restockIntervalMinutes = 30; // Default 30 minutes
    private int restockLimitPerDay = 5; // Default 5 restocks per day
    private boolean unlimitedRestock = false;
    private boolean instantRestockEnabled = false; // NEW: Instant restock setting

    private Map<UUID, RestockData> villagerRestockData = new ConcurrentHashMap<>();

    public RestockService(VillagerTradeManager plugin, Connection databaseConnection,
                          com.alan.VillagerTradeManager.database.AsyncDatabaseExecutor databaseExecutor) {
        this.plugin = plugin;
        this.databaseConnection = databaseConnection;
        this.databaseExecutor = databaseExecutor;

        // Create restock table if it doesn't exist
        createRestockTable();
    }

    private void createRestockTable() {
        try {
            String createRestockTableSQL = "CREATE TABLE IF NOT EXISTS villager_restock_data (" +
                    "villager_uuid TEXT PRIMARY KEY, " +
                    "last_restock_time INTEGER, " +
                    "restock_count_today INTEGER, " +
                    "last_reset_day INTEGER" +
                    ");";

            try (Statement stmt = databaseConnection.createStatement()) {
                stmt.execute(createRestockTableSQL);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create restock table: " + e.getMessage());
        }
    }

    public void processVillagerRestock(Villager villager) {
        // NEW: Skip normal restock if instant restock is enabled
        if (instantRestockEnabled) return;

        if (!customRestockEnabled) return;

        UUID villagerUUID = villager.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long currentDay = currentTime / (24 * 60 * 60 * 1000); // Days since epoch

        // Load restock data for this villager
        RestockData restockData = loadVillagerRestockData(villagerUUID);

        // Reset count if it's a new day
        if (restockData.lastResetDay != currentDay) {
            restockData.restockCountToday = 0;
            restockData.lastResetDay = currentDay;
        }

        // Check if restock is allowed
        if (!unlimitedRestock && restockData.restockCountToday >= restockLimitPerDay) {
            return; // Reached daily limit
        }

        // Check if enough time has passed since last restock
        if (currentTime - restockData.lastRestockTime < (restockIntervalMinutes * 60 * 1000L)) {
            return; // Not enough time has passed
        }

        // Perform restock
        restockVillagerTrades(villager);

        // Update restock data
        restockData.lastRestockTime = currentTime;
        restockData.restockCountToday++;
        saveVillagerRestockData(villagerUUID, restockData);
    }

    public void restockVillagerTrades(Villager villager) {
        // Reset uses for all recipes to allow trading again
        List<MerchantRecipe> recipes = new ArrayList<>();
        for (MerchantRecipe recipe : villager.getRecipes()) {
            MerchantRecipe newRecipe = new MerchantRecipe(
                    recipe.getResult(),
                    0, // Reset uses to 0
                    recipe.getMaxUses(),
                    recipe.hasExperienceReward(),
                    recipe.getVillagerExperience(),
                    recipe.getPriceMultiplier()
            );
            newRecipe.setIngredients(new ArrayList<>(recipe.getIngredients()));
            newRecipe.setDemand(recipe.getDemand());
            newRecipe.setSpecialPrice(recipe.getSpecialPrice());
            recipes.add(newRecipe);
        }
        villager.setRecipes(recipes);
    }

    private RestockData loadVillagerRestockData(UUID villagerUUID) {
        // Check memory cache first
        RestockData data = villagerRestockData.get(villagerUUID);
        if (data != null) {
            return data;
        }

        // Load from database async
        CompletableFuture<RestockData> future = databaseExecutor.queryAsync(connection -> {
            try {
                String selectSQL = "SELECT last_restock_time, restock_count_today, last_reset_day FROM villager_restock_data WHERE villager_uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
                    pstmt.setString(1, villagerUUID.toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        return new RestockData(
                                rs.getLong("last_restock_time"),
                                rs.getInt("restock_count_today"),
                                rs.getLong("last_reset_day")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not load villager restock data: " + e.getMessage());
            }
            return new RestockData(0, 0, 0);
        }, databaseConnection);

        try {
            data = future.get(); // This blocks but it's only on startup/loading
        } catch (Exception e) {
            data = new RestockData(0, 0, 0);
        }

        villagerRestockData.put(villagerUUID, data);
        return data;
    }

    private void saveVillagerRestockData(UUID villagerUUID, RestockData data) {
        databaseExecutor.executeAsync(() -> {
            try {
                String insertSQL = "INSERT OR REPLACE INTO villager_restock_data (villager_uuid, last_restock_time, restock_count_today, last_reset_day) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmt = databaseConnection.prepareStatement(insertSQL)) {
                    pstmt.setString(1, villagerUUID.toString());
                    pstmt.setLong(2, data.lastRestockTime);
                    pstmt.setInt(3, data.restockCountToday);
                    pstmt.setLong(4, data.lastResetDay);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save villager restock data: " + e.getMessage());
            }
        });
    }

    // NEW: Handle instant restock for a villager
    public void handleInstantRestock(Villager villager, PlatformService platformService, TaskScheduler taskScheduler) {
        if (!instantRestockEnabled) return;

        // Use platform-appropriate scheduling
        if (platformService.isFoliaPlatform()) {
            taskScheduler.runSyncForEntity(villager, () -> restockVillagerTrades(villager));
        } else {
            // For non-Folia platforms, run immediately
            restockVillagerTrades(villager);
        }
    }

    public void startRestockMonitor(PlatformService platformService, TaskScheduler taskScheduler) {
        taskScheduler.runSyncRepeating(() -> {
            if (!customRestockEnabled) return;

            // Process restock for all villagers - FIXED FOR FOLIA
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // For Folia, run in player's region
                if (platformService.isFoliaPlatform()) {
                    taskScheduler.runSyncAtLocation(player.getLocation(), () -> {
                        processPlayerRestock(player);
                    });
                } else {
                    processPlayerRestock(player);
                }
            }
        }, 1200L, 1200L); // Run every minute (20 ticks = 1 second)
    }

    // NEW: Process restock for a specific player (region-safe)
    private void processPlayerRestock(Player player) {
        try {
            player.getNearbyEntities(64, 64, 64).stream()
                    .filter(entity -> entity instanceof Villager)
                    .map(entity -> (Villager) entity)
                    .forEach(villager -> {
                        processVillagerRestock(villager);
                    });
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
                if (entity instanceof Villager && entity.getLocation().distance(player.getLocation()) <= 64) {
                    Villager villager = (Villager) entity;
                    processVillagerRestock(villager);
                }
            }
        }
    }

    // Getters and setters
    public boolean isCustomRestockEnabled() { return customRestockEnabled; }
    public void setCustomRestockEnabled(boolean enabled) { this.customRestockEnabled = enabled; }
    public int getRestockIntervalMinutes() { return restockIntervalMinutes; }
    public void setRestockIntervalMinutes(int minutes) { this.restockIntervalMinutes = minutes; }
    public int getRestockLimitPerDay() { return restockLimitPerDay; }
    public void setRestockLimitPerDay(int limit) { this.restockLimitPerDay = limit; }
    public boolean isUnlimitedRestock() { return unlimitedRestock; }
    public void setUnlimitedRestock(boolean unlimited) { this.unlimitedRestock = unlimited; }
    public boolean isInstantRestockEnabled() { return instantRestockEnabled; }
    public void setInstantRestockEnabled(boolean enabled) { this.instantRestockEnabled = enabled; }

    // Inner class to store restock data for each villager
    private static class RestockData {
        long lastRestockTime;
        int restockCountToday;
        long lastResetDay;

        RestockData(long lastRestockTime, int restockCountToday, long lastResetDay) {
            this.lastRestockTime = lastRestockTime;
            this.restockCountToday = restockCountToday;
            this.lastResetDay = lastResetDay;
        }
    }
}