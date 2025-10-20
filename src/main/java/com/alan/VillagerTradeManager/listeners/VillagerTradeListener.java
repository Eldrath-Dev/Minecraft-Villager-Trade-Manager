package com.alan.VillagerTradeManager.listeners;

import com.alan.VillagerTradeManager.VillagerTradeManager;
import com.alan.VillagerTradeManager.platform.PlatformService;
import com.alan.VillagerTradeManager.scheduler.TaskScheduler;
import com.alan.VillagerTradeManager.services.RestockService;
import com.alan.VillagerTradeManager.services.TradeService;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.raid.RaidFinishEvent;

/**
 * Listener class for handling villager trade events
 */
public class VillagerTradeListener implements Listener {

    private final VillagerTradeManager plugin;
    private final TradeService tradeService;
    private final RestockService restockService;
    private final PlatformService platformService;

    public VillagerTradeListener(VillagerTradeManager plugin, TradeService tradeService,
                                 RestockService restockService, PlatformService platformService) {
        this.plugin = plugin;
        this.tradeService = tradeService;
        this.restockService = restockService;
        this.platformService = platformService;
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!tradeService.isTradeManagementEnabled()) return;

        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Player player = event.getPlayer();

            // Remove Hero of the Village effect if available
            tradeService.removeHeroEffect(player);

            Villager villager = (Villager) event.getRightClicked();

            // Process trades - use region-safe scheduling for Folia
            TaskScheduler taskScheduler = plugin.getTaskScheduler();
            if (platformService.isFoliaPlatform()) {
                taskScheduler.runSyncForEntity(villager, () -> tradeService.processVillagerTrades(villager));
            } else {
                tradeService.processVillagerTrades(villager);
            }

            // Schedule additional processing to catch any late updates
            taskScheduler.runSyncLaterForEntity(villager, () -> tradeService.processVillagerTrades(villager), 1L);

            // Handle instant restock if enabled
            if (restockService.isInstantRestockEnabled()) {
                restockService.handleInstantRestock(villager, platformService, taskScheduler);
            }
        }
    }

    @EventHandler
    public void onVillagerTransform(EntityTransformEvent event) {
        if (!tradeService.isTradeManagementEnabled()) return;

        if (event.getEntity().getType() == EntityType.VILLAGER) {
            if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
                Villager villager = (Villager) event.getEntity();

                // Process trades - use region-safe scheduling for Folia
                TaskScheduler taskScheduler = plugin.getTaskScheduler();
                if (platformService.isFoliaPlatform()) {
                    taskScheduler.runSyncForEntity(villager, () -> tradeService.processVillagerTrades(villager));
                } else {
                    tradeService.processVillagerTrades(villager);
                }
            }
        }
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!tradeService.isTradeManagementEnabled() || !tradeService.hasRaidEvents()) return;

        // Prevent Hero of the Village effect by removing it from winners
        for (Player player : event.getWinners()) {
            if (tradeService.hasHeroOfTheVillageEffect()) {
                tradeService.removeHeroEffect(player);
            }
        }
    }
}