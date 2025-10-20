package com.alan.VillagerTradeManager.database;

import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Asynchronous database executor with thread pool management
 */
public class AsyncDatabaseExecutor {

    private final Plugin plugin;
    private final ExecutorService databaseExecutor;
    private final int threadPoolSize;

    public AsyncDatabaseExecutor(Plugin plugin, int threadPoolSize) {
        this.plugin = plugin;
        this.threadPoolSize = threadPoolSize;
        this.databaseExecutor = Executors.newFixedThreadPool(threadPoolSize);
        plugin.getLogger().info("Async Database Executor started (" + threadPoolSize + " threads)");
    }

    /**
     * Execute a database operation asynchronously
     */
    public void executeAsync(Runnable operation) {
        databaseExecutor.execute(operation);
    }

    /**
     * Execute a database query asynchronously and return a result
     */
    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> queryFunction, Connection connection) {
        return CompletableFuture.supplyAsync(() -> queryFunction.apply(connection), databaseExecutor);
    }

    /**
     * Shutdown the executor service gracefully
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Async Database Executor...");
        databaseExecutor.shutdown();
        try {
            if (!databaseExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Database executor did not terminate in time, forcing shutdown");
                databaseExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            plugin.getLogger().severe("Interrupted while shutting down database executor: " + e.getMessage());
            databaseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        plugin.getLogger().info("Async Database Executor shutdown complete");
    }

    public int getThreadPoolSize() {
        return threadPoolSize;
    }
}