package com.alan.VillagerTradeManager.services;

import com.alan.VillagerTradeManager.VillagerTradeManager;
import com.alan.VillagerTradeManager.platform.PlatformService;
import com.alan.VillagerTradeManager.scheduler.TaskScheduler;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.potion.PotionEffectType;

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling villager trade processing and custom pricing
 */
public class TradeService {

    private final VillagerTradeManager plugin;
    private boolean tradeManagementEnabled = true;
    private boolean hasHeroOfTheVillageEffect = true;
    private boolean hasRaidEvents = true;
    private Map<String, Integer> customPrices = new ConcurrentHashMap<>();

    public TradeService(VillagerTradeManager plugin) {
        this.plugin = plugin;
        detectServerCapabilities();
    }

    private void detectServerCapabilities() {
        // Check if Hero of the Village effect is available
        try {
            PotionEffectType.HERO_OF_THE_VILLAGE.getClass();
        } catch (NoSuchFieldError e) {
            hasHeroOfTheVillageEffect = false;
            plugin.getLogger().info("Hero of the Village effect not available on this server version");
        }

        // Check if Raid events are available
        try {
            Class.forName("org.bukkit.event.raid.RaidFinishEvent");
        } catch (ClassNotFoundException e) {
            hasRaidEvents = false;
            plugin.getLogger().info("Raid events not available on this server version");
        }
    }

    public void processVillagerTrades(Villager villager) {
        if (!tradeManagementEnabled) return;

        List<MerchantRecipe> recipes = new ArrayList<>();
        boolean modified = false;

        try {
            for (MerchantRecipe recipe : villager.getRecipes()) {
                ItemStack result = recipe.getResult();

                // Check if this is an enchanted book trade
                if (result.getType() == Material.ENCHANTED_BOOK && result.hasItemMeta()) {
                    // Create custom enchanted book trade with configured prices
                    MerchantRecipe customRecipe = createCustomEnchantedBookRecipe(recipe);
                    recipes.add(customRecipe);
                    modified = true;
                } else {
                    // For all other trades, neutralize discounts
                    MerchantRecipe neutralizedRecipe = createNeutralizedRecipe(recipe);
                    recipes.add(neutralizedRecipe);
                    modified = true;
                }
            }

            // Update recipes if we made changes
            if (modified) {
                villager.setRecipes(recipes);
            }
        } catch (NoSuchMethodError e) {
            // Handle older versions that might not have all methods
            plugin.getLogger().warning("Could not process villager trades on this server version");
        }
    }

    private MerchantRecipe createCustomEnchantedBookRecipe(MerchantRecipe originalRecipe) {
        ItemStack result = originalRecipe.getResult();

        // Get enchantment information
        String enchantKey = "enchanted_book";

        if (result.hasItemMeta() && result.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
            if (meta.hasStoredEnchants()) {
                // Get the first enchantment for pricing
                Map.Entry<Enchantment, Integer> firstEnchant = meta.getStoredEnchants().entrySet().iterator().next();
                String enchantName = getEnchantmentKey(firstEnchant.getKey());
                int level = firstEnchant.getValue();
                enchantKey = "enchanted_book_" + enchantName + "_" + level;
            }
        }

        // Get custom price from memory or use default (max 64 emeralds)
        int emeraldCost = Math.min(customPrices.getOrDefault(enchantKey, 15), 64); // Max 64 emeralds

        // Create a new recipe with custom pricing
        MerchantRecipe customRecipe = createMerchantRecipe(
                result,
                getRecipeUses(originalRecipe),
                getRecipeMaxUses(originalRecipe),
                getRecipeHasExperienceReward(originalRecipe),
                getRecipeVillagerExperience(originalRecipe),
                0.0f // No price multiplier
        );

        // Set fixed prices for enchanted books - CONSISTENT 1 BOOK PER TRADE
        List<ItemStack> ingredients = new ArrayList<>();

        // First ingredient: Exactly 1 Book
        ItemStack books = new ItemStack(Material.BOOK, 1);
        ingredients.add(books);

        // Second ingredient: Emeralds (custom amount, max 64)
        ItemStack emeralds = new ItemStack(Material.EMERALD, emeraldCost);
        ingredients.add(emeralds);

        customRecipe.setIngredients(ingredients);

        // Neutralize all discount mechanisms
        setRecipeDemand(customRecipe, 0);
        setRecipeSpecialPrice(customRecipe, 0);
        setRecipePriceMultiplier(customRecipe, 0.0f);

        return customRecipe;
    }

    private MerchantRecipe createNeutralizedRecipe(MerchantRecipe originalRecipe) {
        MerchantRecipe neutralizedRecipe = createMerchantRecipe(
                originalRecipe.getResult(),
                getRecipeUses(originalRecipe),
                getRecipeMaxUses(originalRecipe),
                getRecipeHasExperienceReward(originalRecipe),
                getRecipeVillagerExperience(originalRecipe),
                0.0f // Neutral price multiplier
        );

        // Copy ingredients
        neutralizedRecipe.setIngredients(new ArrayList<>(originalRecipe.getIngredients()));

        // Aggressively neutralize all discount mechanisms
        setRecipeDemand(neutralizedRecipe, 0);
        setRecipeSpecialPrice(neutralizedRecipe, 0);
        setRecipePriceMultiplier(neutralizedRecipe, 0.0f);

        return neutralizedRecipe;
    }

    public void removeHeroEffect(Player player) {
        if (!tradeManagementEnabled || !hasHeroOfTheVillageEffect) return;

        // Remove Hero of the Village effect
        try {
            player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            try {
                Method removePotionEffectMethod = player.getClass().getMethod("removePotionEffect", PotionEffectType.class);
                removePotionEffectMethod.invoke(player, PotionEffectType.HERO_OF_THE_VILLAGE);
            } catch (Exception ex) {
                // Ignore if method doesn't exist
            }
        }
    }

    // Version compatibility methods
    @SuppressWarnings("removal")
    public String getEnchantmentKey(Enchantment enchantment) {
        try {
            return enchantment.getKey().getKey();
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            return enchantment.getName().toLowerCase();
        }
    }

    private int getRecipeUses(MerchantRecipe recipe) {
        try {
            return recipe.getUses();
        } catch (NoSuchMethodError e) {
            return 0; // Default value
        }
    }

    private int getRecipeMaxUses(MerchantRecipe recipe) {
        try {
            return recipe.getMaxUses();
        } catch (NoSuchMethodError e) {
            return 99999; // Default high value
        }
    }

    private boolean getRecipeHasExperienceReward(MerchantRecipe recipe) {
        try {
            return recipe.hasExperienceReward();
        } catch (NoSuchMethodError e) {
            return false; // Default value
        }
    }

    private int getRecipeVillagerExperience(MerchantRecipe recipe) {
        try {
            return recipe.getVillagerExperience();
        } catch (NoSuchMethodError e) {
            return 0; // Default value
        }
    }

    private void setRecipeDemand(MerchantRecipe recipe, int demand) {
        try {
            recipe.setDemand(demand);
        } catch (NoSuchMethodError e) {
            // Method not available, ignore
        }
    }

    private void setRecipeSpecialPrice(MerchantRecipe recipe, int specialPrice) {
        try {
            recipe.setSpecialPrice(specialPrice);
        } catch (NoSuchMethodError e) {
            // Method not available, ignore
        }
    }

    private void setRecipePriceMultiplier(MerchantRecipe recipe, float multiplier) {
        try {
            recipe.setPriceMultiplier(multiplier);
        } catch (NoSuchMethodError e) {
            // Method not available, ignore
        }
    }

    private MerchantRecipe createMerchantRecipe(ItemStack result, int uses, int maxUses, boolean experienceReward, int villagerExperience, float priceMultiplier) {
        try {
            return new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            try {
                return new MerchantRecipe(result, uses, maxUses, experienceReward, villagerExperience, priceMultiplier);
            } catch (Exception ex) {
                // Ultimate fallback
                return new MerchantRecipe(result, maxUses);
            }
        }
    }

    public void saveCustomPricesToFile(File pricesFile) {
        try {
            if (!pricesFile.exists()) {
                pricesFile.createNewFile();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pricesFile))) {
                oos.writeObject(new HashMap<>(customPrices));
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save custom prices to file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void loadCustomPricesFromFile(File pricesFile) {
        if (!pricesFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pricesFile))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                customPrices = new ConcurrentHashMap<>((Map<String, Integer>) obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not load custom prices from file: " + e.getMessage());
        }
    }

    public String convertToUserFriendlyFormat(String internalKey) {
        // Convert "enchanted_book_efficiency_1" to "efficiency 1"
        if (internalKey.startsWith("enchanted_book_")) {
            return internalKey.substring(15).replace("_", " ");
        }
        return internalKey;
    }

    public void startTradeMonitor(PlatformService platformService, TaskScheduler taskScheduler) {
        taskScheduler.runSyncRepeating(() -> {
            if (!tradeManagementEnabled) return;

            // Remove Hero effects from all players if available
            if (hasHeroOfTheVillageEffect) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    removeHeroEffect(player);
                }
            }

            // Process trades for all villagers near players - FIXED FOR FOLIA
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // For Folia, we need to run this in the player's region
                if (platformService.isFoliaPlatform()) {
                    taskScheduler.runSyncAtLocation(player.getLocation(), () -> {
                        processPlayerVillagers(player);
                    });
                } else {
                    // For non-Folia platforms, use the original approach
                    processPlayerVillagers(player);
                }
            }
        }, 20L, 60L); // Run every 3 seconds
    }

    // NEW: Process villagers for a specific player (region-safe)
    private void processPlayerVillagers(Player player) {
        try {
            // Get villagers in a reasonable radius using region-safe methods
            player.getNearbyEntities(32, 32, 32).stream()
                    .filter(entity -> entity instanceof Villager)
                    .map(entity -> (Villager) entity)
                    .forEach(villager -> {
                        processVillagerTrades(villager);
                    });
        } catch (NoSuchMethodError e) {
            // Fallback for older versions
            for (org.bukkit.entity.Entity entity : player.getWorld().getEntities()) {
                if (entity instanceof Villager && entity.getLocation().distance(player.getLocation()) <= 32) {
                    Villager villager = (Villager) entity;
                    processVillagerTrades(villager);
                }
            }
        }
    }

    // Getters and setters
    public boolean isTradeManagementEnabled() { return tradeManagementEnabled; }
    public void setTradeManagementEnabled(boolean enabled) { this.tradeManagementEnabled = enabled; }
    public boolean hasHeroOfTheVillageEffect() { return hasHeroOfTheVillageEffect; }
    public boolean hasRaidEvents() { return hasRaidEvents; }
    public Map<String, Integer> getCustomPrices() { return customPrices; }
}