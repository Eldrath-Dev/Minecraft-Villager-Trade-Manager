package com.alan.VillagerTradeManager.services;

import com.alan.VillagerTradeManager.VillagerTradeManager;
import com.alan.VillagerTradeManager.database.AsyncDatabaseExecutor;

import java.sql.*;
import java.io.File;

/**
 * Service for handling plugin settings and database operations
 */
public class SettingsService {

    private final VillagerTradeManager plugin;
    private Connection databaseConnection;
    private final AsyncDatabaseExecutor databaseExecutor;

    public SettingsService(VillagerTradeManager plugin, Connection databaseConnection, AsyncDatabaseExecutor databaseExecutor) {
        this.plugin = plugin;
        this.databaseConnection = databaseConnection;
        this.databaseExecutor = databaseExecutor;
    }

    public void initializeDatabase() {
        try {
            // Create database file if it doesn't exist
            File dbFile = new File(plugin.getDataFolder(), "villager_data.db");
            if (!dbFile.exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Connect to SQLite database
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            databaseConnection = DriverManager.getConnection(url);

            // Create tables if they don't exist
            createTables();

        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize database: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        String createPricesTableSQL = "CREATE TABLE IF NOT EXISTS custom_prices (" +
                "item_key TEXT PRIMARY KEY, " +
                "price INTEGER NOT NULL" +
                ");";

        String createSettingsTableSQL = "CREATE TABLE IF NOT EXISTS settings (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT" +
                ");";

        String createRestockTableSQL = "CREATE TABLE IF NOT EXISTS villager_restock_data (" +
                "villager_uuid TEXT PRIMARY KEY, " +
                "last_restock_time INTEGER, " +
                "restock_count_today INTEGER, " +
                "last_reset_day INTEGER" +
                ");";

        try (Statement stmt = databaseConnection.createStatement()) {
            stmt.execute(createPricesTableSQL);
            stmt.execute(createSettingsTableSQL);
            stmt.execute(createRestockTableSQL);
        }
    }

    public void loadSettingsFromDatabase() {
        // Use async database executor
        databaseExecutor.executeAsync(() -> {
            try {
                String selectSQL = "SELECT key, value FROM settings";
                try (Statement stmt = databaseConnection.createStatement();
                     ResultSet rs = stmt.executeQuery(selectSQL)) {
                    while (rs.next()) {
                        String key = rs.getString("key");
                        String value = rs.getString("value");

                        // Update settings on the main thread
                        plugin.getTaskScheduler().runSync(() -> {
                            // Settings are handled by the main plugin class now
                        });
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not load settings from database: " + e.getMessage());
            }
        });
    }

    public void saveSettingToDatabase(String key, String value) {
        databaseExecutor.executeAsync(() -> {
            try {
                String insertSQL = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
                try (PreparedStatement pstmt = databaseConnection.prepareStatement(insertSQL)) {
                    pstmt.setString(1, key);
                    pstmt.setString(2, value);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not save setting to database: " + e.getMessage());
            }
        });
    }

    public void closeDatabase() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not close database connection: " + e.getMessage());
        }
    }

    // Getters
    public Connection getDatabaseConnection() { return databaseConnection; }
}