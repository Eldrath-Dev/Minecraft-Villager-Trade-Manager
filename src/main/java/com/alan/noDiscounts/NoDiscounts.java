package com.alan.noDiscounts;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NoDiscounts extends JavaPlugin implements Listener, TabExecutor {

    private boolean discountsDisabled = true;
    private BukkitRunnable tradeMonitorTask;
    private Map<String, Integer> customPrices = new ConcurrentHashMap<>();
    private File pricesFile;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this, this);

        // Register command
        this.getCommand("nodiscounts").setExecutor(this);
        this.getCommand("nodiscounts").setTabCompleter(this);

        // Initialize prices file
        pricesFile = new File(getDataFolder(), "prices.dat");
        getDataFolder().mkdirs();

        // Load custom prices from file
        loadCustomPricesFromFile();

        // Start trade monitoring task
        startTradeMonitor();

        getLogger().info("NoDiscounts plugin enabled! All villager discounts neutralized.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (tradeMonitorTask != null) {
            tradeMonitorTask.cancel();
        }

        // Save custom prices to file
        saveCustomPricesToFile();
    }

    @EventHandler
    public void onVillagerInteract(PlayerInteractEntityEvent event) {
        if (!discountsDisabled) return;

        if (event.getRightClicked().getType() == EntityType.VILLAGER) {
            Player player = event.getPlayer();

            // Remove Hero of the Village effect immediately
            removeHeroEffect(player);

            Villager villager = (Villager) event.getRightClicked();

            // Process trades
            processVillagerTrades(villager);

            // Schedule additional processing to catch any late updates
            new BukkitRunnable() {
                @Override
                public void run() {
                    processVillagerTrades(villager);
                }
            }.runTaskLater(this, 1L);
        }
    }

    @EventHandler
    public void onVillagerTransform(EntityTransformEvent event) {
        if (!discountsDisabled) return;

        if (event.getEntity().getType() == EntityType.VILLAGER) {
            if (event.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
                Villager villager = (Villager) event.getEntity();
                processVillagerTrades(villager);
            }
        }
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent event) {
        if (!discountsDisabled) return;

        // Prevent Hero of the Village effect by removing it from winners
        for (Player player : event.getWinners()) {
            removeHeroEffect(player);
        }
    }

    private void startTradeMonitor() {
        tradeMonitorTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!discountsDisabled) return;

                // Remove Hero effects from all players
                for (Player player : getServer().getOnlinePlayers()) {
                    removeHeroEffect(player);
                }

                // Process trades for all villagers near players
                for (Player player : getServer().getOnlinePlayers()) {
                    // Get villagers in a reasonable radius
                    player.getNearbyEntities(32, 32, 32).stream()
                            .filter(entity -> entity instanceof Villager)
                            .map(entity -> (Villager) entity)
                            .forEach(NoDiscounts.this::processVillagerTrades);
                }
            }
        };
        tradeMonitorTask.runTaskTimer(this, 20L, 60L); // Run every 3 seconds
    }

    private void processVillagerTrades(Villager villager) {
        if (!discountsDisabled) return;

        List<MerchantRecipe> recipes = new ArrayList<>();
        boolean modified = false;

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
                String enchantName = firstEnchant.getKey().getKey().getKey();
                int level = firstEnchant.getValue();
                enchantKey = "enchanted_book_" + enchantName + "_" + level;
            }
        }

        // Get custom price from memory or use default
        int emeraldCost = customPrices.getOrDefault(enchantKey, 15); // Default 15 emeralds

        // Create a new recipe with custom pricing
        MerchantRecipe customRecipe = new MerchantRecipe(
                result,
                originalRecipe.getUses(),
                originalRecipe.getMaxUses(),
                false, // No experience reward to prevent manipulation
                0, // No villager experience
                0.0f // No price multiplier
        );

        // Set fixed prices for enchanted books
        List<ItemStack> ingredients = new ArrayList<>();

        // First ingredient: Books (3-5 books)
        ItemStack books = new ItemStack(Material.BOOK, 3 + new Random().nextInt(3));
        ingredients.add(books);

        // Second ingredient: Emeralds (custom amount)
        ItemStack emeralds = new ItemStack(Material.EMERALD, emeraldCost);
        ingredients.add(emeralds);

        customRecipe.setIngredients(ingredients);

        // Neutralize all discount mechanisms
        customRecipe.setDemand(0);
        customRecipe.setSpecialPrice(0);
        customRecipe.setPriceMultiplier(0.0f);

        return customRecipe;
    }

    private MerchantRecipe createNeutralizedRecipe(MerchantRecipe originalRecipe) {
        MerchantRecipe neutralizedRecipe = new MerchantRecipe(
                originalRecipe.getResult(),
                originalRecipe.getUses(),
                originalRecipe.getMaxUses(),
                originalRecipe.hasExperienceReward(),
                originalRecipe.getVillagerExperience(),
                0.0f // Neutral price multiplier
        );

        // Copy ingredients
        neutralizedRecipe.setIngredients(new ArrayList<>(originalRecipe.getIngredients()));

        // Aggressively neutralize all discount mechanisms
        neutralizedRecipe.setDemand(0);
        neutralizedRecipe.setSpecialPrice(0);
        neutralizedRecipe.setPriceMultiplier(0.0f);

        return neutralizedRecipe;
    }

    private void removeHeroEffect(Player player) {
        if (!discountsDisabled) return;

        // Remove Hero of the Village effect
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE);
    }

    private void saveCustomPricesToFile() {
        try {
            if (!pricesFile.exists()) {
                pricesFile.createNewFile();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(pricesFile))) {
                oos.writeObject(new HashMap<>(customPrices));
            }
        } catch (IOException e) {
            getLogger().severe("Could not save custom prices to file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadCustomPricesFromFile() {
        if (!pricesFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(pricesFile))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                customPrices = new ConcurrentHashMap<>((Map<String, Integer>) obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            getLogger().severe("Could not load custom prices from file: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nodiscounts")) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /nodiscounts <on|off|status|setprice>");
                sender.sendMessage("§7Current status: " + (discountsDisabled ? "§aActive" : "§cInactive"));
                return true;
            }

            if (args[0].equalsIgnoreCase("on")) {
                if (discountsDisabled) {
                    sender.sendMessage("§cVillager discount neutralizer is already active!");
                    return true;
                }

                discountsDisabled = true;
                sender.sendMessage("§aVillager discount neutralizer activated!");
                return true;
            } else if (args[0].equalsIgnoreCase("off")) {
                if (!discountsDisabled) {
                    sender.sendMessage("§cVillager discount neutralizer is already inactive!");
                    return true;
                }

                discountsDisabled = false;
                sender.sendMessage("§aVillager discount neutralizer deactivated!");
                return true;
            } else if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage("§7Villager discount neutralizer is currently: " + (discountsDisabled ? "§aActive" : "§cInactive"));
                sender.sendMessage("§7Custom prices:");
                if (customPrices.isEmpty()) {
                    sender.sendMessage("  §7No custom prices set");
                } else {
                    for (Map.Entry<String, Integer> entry : customPrices.entrySet()) {
                        sender.sendMessage("  §7" + entry.getKey() + ": §a" + entry.getValue() + " §7emeralds");
                    }
                }
                return true;
            } else if (args[0].equalsIgnoreCase("setprice")) {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /nodiscounts setprice <item_key> <price>");
                    sender.sendMessage("§7Examples:");
                    sender.sendMessage("  §7/nodiscounts setprice enchanted_book_sharpness_1 20");
                    sender.sendMessage("  §7/nodiscounts setprice enchanted_book_protection_4 35");
                    sender.sendMessage("  §7/nodiscounts setprice enchanted_book_mending_1 50");
                    sender.sendMessage("  §7/nodiscounts setprice enchanted_book_unbreaking_3 25");
                    sender.sendMessage("  §7(Format: enchanted_book_<enchant_name>_<level>)");
                    return true;
                }

                String itemKey = args[1].toLowerCase();
                int price;

                try {
                    price = Integer.parseInt(args[2]);
                    if (price <= 0) {
                        sender.sendMessage("§cPrice must be a positive number!");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid price! Please enter a valid number.");
                    return true;
                }

                customPrices.put(itemKey, price);
                saveCustomPricesToFile();
                sender.sendMessage("§aSet custom price for §7" + itemKey + " §ato §e" + price + " §aemeralds.");

                return true;
            } else {
                sender.sendMessage("§cUsage: /nodiscounts <on|off|status|setprice>");
                sender.sendMessage("§7Current status: " + (discountsDisabled ? "§aActive" : "§cInactive"));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("nodiscounts")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                List<String> commands = new ArrayList<>();
                commands.add("on");
                commands.add("off");
                commands.add("status");
                commands.add("setprice");

                StringUtil.copyPartialMatches(args[0], commands, completions);
                Collections.sort(completions);
                return completions;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("setprice")) {
                List<String> completions = new ArrayList<>();

                // Generate tab completions for all enchantments and levels
                for (Enchantment enchantment : Enchantment.values()) {
                    String enchantName = enchantment.getKey().getKey();
                    for (int level = 1; level <= enchantment.getMaxLevel(); level++) {
                        String enchantKey = "enchanted_book_" + enchantName + "_" + level;
                        completions.add(enchantKey);
                    }
                }

                // Create a new list to avoid concurrent modification
                List<String> matches = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], completions, matches);
                Collections.sort(matches);
                return matches;
            }
        }
        return Collections.emptyList();
    }
}