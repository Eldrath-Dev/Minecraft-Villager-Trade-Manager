package com.alan.VillagerTradeManager;

import com.alan.VillagerTradeManager.commands.VillagerTradeCommand;
import com.alan.VillagerTradeManager.database.AsyncDatabaseExecutor;
import com.alan.VillagerTradeManager.listeners.VillagerTradeListener;
import com.alan.VillagerTradeManager.platform.PlatformService;
import com.alan.VillagerTradeManager.scheduler.TaskScheduler;
import com.alan.VillagerTradeManager.services.TradeService;
import com.alan.VillagerTradeManager.services.RestockService;
import com.alan.VillagerTradeManager.services.SettingsService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Main plugin class for Minecraft Villager Trade Manager
 * Version 10.1 - "Instant Restock & Folia Optimization Update"
 */
public final class VillagerTradeManager extends JavaPlugin {

    // Services
    private PlatformService platformService;
    private TaskScheduler taskScheduler;
    private AsyncDatabaseExecutor databaseExecutor;
    private TradeService tradeService;
    private RestockService restockService;
    private SettingsService settingsService;

    // Database connection
    private Connection databaseConnection;

    // File paths
    private File pricesFile;

    @Override
    public void onEnable() {
        // Initialize platform service first
        platformService = new PlatformService(this);
        taskScheduler = platformService.getScheduler();

        // Initialize file paths
        pricesFile = new File(getDataFolder(), "prices.dat");
        getDataFolder().mkdirs();

        // Initialize async database executor
        databaseExecutor = new AsyncDatabaseExecutor(this, 2); // Default 2 threads

        // Initialize database connection
        initializeDatabase();

        // Initialize services AFTER database connection is established
        settingsService = new SettingsService(this, databaseConnection, databaseExecutor);
        tradeService = new TradeService(this);
        restockService = new RestockService(this, databaseConnection, databaseExecutor);

        // Load data
        loadCustomPricesFromFile();
        settingsService.loadSettingsFromDatabase();

        // Register events
        getServer().getPluginManager().registerEvents(new VillagerTradeListener(this, tradeService, restockService, platformService), this);

        // Register commands
        getCommand("villagertrade").setExecutor(new VillagerTradeCommand(this, settingsService, tradeService, restockService));
        getCommand("villagertrade").setTabCompleter(new VillagerTradeCommand(this, settingsService, tradeService, restockService));

        // Start monitoring tasks
        startTradeMonitor();
        startRestockMonitor();

        getLogger().info("Minecraft Villager Trade Manager plugin enabled!");
        getLogger().info("Complete control over villager trading economics.");
        getLogger().info("Compatible with Minecraft versions 1.20.x - 1.21.x");
        getLogger().info("Platform: " + platformService.getDetectedPlatform() + " | Scheduler: " + taskScheduler.getSchedulerName());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // Cancel all scheduled tasks
        if (taskScheduler != null) {
            taskScheduler.cancelAllTasks();
        }

        // Shutdown database executor
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }

        // Save custom prices to file
        if (tradeService != null) {
            saveCustomPricesToFile();
        }

        // Close database connection
        closeDatabase();
    }

    private void initializeDatabase() {
        try {
            // Create database file if it doesn't exist
            File dbFile = new File(getDataFolder(), "villager_data.db");
            if (!dbFile.exists()) {
                getDataFolder().mkdirs();
            }

            // Connect to SQLite database
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            databaseConnection = DriverManager.getConnection(url);

            getLogger().info("Database connection established successfully");

        } catch (SQLException e) {
            getLogger().severe("Could not initialize database: " + e.getMessage());
        }
    }

    private void closeDatabase() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                getLogger().info("Database connection closed successfully");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not close database connection: " + e.getMessage());
        }
    }

    private void startTradeMonitor() {
        if (tradeService != null) {
            tradeService.startTradeMonitor(platformService, taskScheduler);
        }
    }

    private void startRestockMonitor() {
        if (restockService != null) {
            restockService.startRestockMonitor(platformService, taskScheduler);
        }
    }

    private void saveCustomPricesToFile() {
        if (tradeService != null) {
            tradeService.saveCustomPricesToFile(pricesFile);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCustomPricesFromFile() {
        if (tradeService != null) {
            tradeService.loadCustomPricesFromFile(pricesFile);
        }
    }

    // Getters for services
    public PlatformService getPlatformService() { return platformService; }
    public TaskScheduler getTaskScheduler() { return taskScheduler; }
    public AsyncDatabaseExecutor getDatabaseExecutor() { return databaseExecutor; }
    public TradeService getTradeService() { return tradeService; }
    public RestockService getRestockService() { return restockService; }
    public SettingsService getSettingsService() { return settingsService; }
    public Connection getDatabaseConnection() { return databaseConnection; }
    public File getPricesFile() { return pricesFile; }
}