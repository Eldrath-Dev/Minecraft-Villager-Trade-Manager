package com.alan.VillagerTradeManager.commands;

import com.alan.VillagerTradeManager.VillagerTradeManager;
import com.alan.VillagerTradeManager.services.RestockService;
import com.alan.VillagerTradeManager.services.SettingsService;
import com.alan.VillagerTradeManager.services.TradeService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.*;

/**
 * Command handler for villager trade management commands
 */
public class VillagerTradeCommand implements CommandExecutor, TabCompleter {

    private final VillagerTradeManager plugin;
    private final SettingsService settingsService;
    private final TradeService tradeService;
    private final RestockService restockService;

    public VillagerTradeCommand(VillagerTradeManager plugin, SettingsService settingsService,
                                TradeService tradeService, RestockService restockService) {
        this.plugin = plugin;
        this.settingsService = settingsService;
        this.tradeService = tradeService;
        this.restockService = restockService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("villagertrade")) {
            if (args.length == 0) {
                sender.sendMessage("§cUsage: /villagertrade <on|off|status|setprice|restock|restocktime|restocklimit|unlimitedrestock|instantrestock>");
                sender.sendMessage("§7Current status: " + (tradeService.isTradeManagementEnabled() ? "§aActive" : "§cInactive"));
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "on":
                    return handleOnCommand(sender);
                case "off":
                    return handleOffCommand(sender);
                case "status":
                    return handleStatusCommand(sender);
                case "setprice":
                    return handleSetPriceCommand(sender, args);
                case "restock":
                    return handleRestockCommand(sender, args);
                case "restocktime":
                    return handleRestockTimeCommand(sender, args);
                case "restocklimit":
                    return handleRestockLimitCommand(sender, args);
                case "unlimitedrestock":
                    return handleUnlimitedRestockCommand(sender, args);
                case "instantrestock":
                    return handleInstantRestockCommand(sender, args);
                default:
                    sender.sendMessage("§cUsage: /villagertrade <on|off|status|setprice|restock|restocktime|restocklimit|unlimitedrestock|instantrestock>");
                    sender.sendMessage("§7Current status: " + (tradeService.isTradeManagementEnabled() ? "§aActive" : "§cInactive"));
                    return true;
            }
        }
        return false;
    }

    private boolean handleOnCommand(CommandSender sender) {
        if (tradeService.isTradeManagementEnabled()) {
            sender.sendMessage("§cVillager trade management is already active!");
            return true;
        }

        tradeService.setTradeManagementEnabled(true);
        sender.sendMessage("§aVillager trade management activated!");
        return true;
    }

    private boolean handleOffCommand(CommandSender sender) {
        if (!tradeService.isTradeManagementEnabled()) {
            sender.sendMessage("§cVillager trade management is already inactive!");
            return true;
        }

        tradeService.setTradeManagementEnabled(false);
        sender.sendMessage("§aVillager trade management deactivated!");
        return true;
    }

    private boolean handleStatusCommand(CommandSender sender) {
        sender.sendMessage("§7Villager trade management is currently: " + (tradeService.isTradeManagementEnabled() ? "§aActive" : "§cInactive"));
        sender.sendMessage("§7Custom prices:");
        if (tradeService.getCustomPrices().isEmpty()) {
            sender.sendMessage("  §7No custom prices set");
        } else {
            for (Map.Entry<String, Integer> entry : tradeService.getCustomPrices().entrySet()) {
                String userFriendlyKey = tradeService.convertToUserFriendlyFormat(entry.getKey());
                sender.sendMessage("  §7" + userFriendlyKey + ": §a" + entry.getValue() + " §7emeralds");
            }
        }
        sender.sendMessage("§7Restock Settings:");
        sender.sendMessage("  §7Custom Restock: " + (restockService.isCustomRestockEnabled() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("  §7Restock Interval: §a" + restockService.getRestockIntervalMinutes() + " §7minutes");
        sender.sendMessage("  §7Restock Limit: §a" + restockService.getRestockLimitPerDay() + " §7per day");
        sender.sendMessage("  §7Unlimited Restock: " + (restockService.isUnlimitedRestock() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("  §7Instant Restock: " + (restockService.isInstantRestockEnabled() ? "§aEnabled" : "§cDisabled"));
        sender.sendMessage("§7Platform Info:");
        sender.sendMessage("  §7Platform: §a" + plugin.getPlatformService().getDetectedPlatform());
        sender.sendMessage("  §7Scheduler: §a" + plugin.getTaskScheduler().getSchedulerName());
        return true;
    }

    private boolean handleSetPriceCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /villagertrade setprice <enchant> <level> <price>");
            sender.sendMessage("§7Examples:");
            sender.sendMessage("  §7/villagertrade setprice efficiency 1 20");
            sender.sendMessage("  §7/villagertrade setprice protection 4 35");
            sender.sendMessage("  §7/villagertrade setprice mending 1 50");
            sender.sendMessage("  §7/villagertrade setprice fortune 3 40");
            return true;
        }

        String enchantName = args[1].toLowerCase();
        int level;
        int price;

        try {
            level = Integer.parseInt(args[2]);
            price = Integer.parseInt(args[3]);

            if (level <= 0) {
                sender.sendMessage("§cLevel must be a positive number!");
                return true;
            }

            if (price <= 0) {
                sender.sendMessage("§cPrice must be a positive number!");
                return true;
            }

            // Limit price to max 64 emeralds
            price = Math.min(price, 64);

        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid level or price! Please enter valid numbers.");
            return true;
        }

        // Convert user-friendly format to internal format
        String itemKey = "enchanted_book_" + enchantName + "_" + level;

        tradeService.getCustomPrices().put(itemKey, price);
        tradeService.saveCustomPricesToFile(plugin.getPricesFile());
        sender.sendMessage("§aSet custom price for §7" + enchantName + " " + level + " §ato §e" + price + " §aemeralds.");

        return true;
    }

    private boolean handleRestockCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /villagertrade restock <on|off>");
            return true;
        }

        if (args[1].equalsIgnoreCase("on")) {
            restockService.setCustomRestockEnabled(true);
            settingsService.saveSettingToDatabase("custom_restock_enabled", "true");
            sender.sendMessage("§aCustom villager restock system enabled!");
            return true;
        } else if (args[1].equalsIgnoreCase("off")) {
            restockService.setCustomRestockEnabled(false);
            settingsService.saveSettingToDatabase("custom_restock_enabled", "false");
            sender.sendMessage("§aCustom villager restock system disabled!");
            return true;
        } else {
            sender.sendMessage("§cUsage: /villagertrade restock <on|off>");
            return true;
        }
    }

    private boolean handleRestockTimeCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /villagertrade restocktime <minutes>");
            sender.sendMessage("§7Current restock time: §a" + restockService.getRestockIntervalMinutes() + " §7minutes");
            return true;
        }

        try {
            int minutes = Integer.parseInt(args[1]);
            if (minutes <= 0) {
                sender.sendMessage("§cRestock time must be a positive number!");
                return true;
            }

            restockService.setRestockIntervalMinutes(minutes);
            settingsService.saveSettingToDatabase("restock_interval_minutes", String.valueOf(minutes));
            sender.sendMessage("§aSet villager restock interval to §e" + minutes + " §aminutes.");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number! Please enter a valid number of minutes.");
            return true;
        }
    }

    private boolean handleRestockLimitCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /villagertrade restocklimit <count>");
            sender.sendMessage("§7Current restock limit: §a" + restockService.getRestockLimitPerDay() + " §7per day");
            return true;
        }

        try {
            int limit = Integer.parseInt(args[1]);
            if (limit <= 0) {
                sender.sendMessage("§cRestock limit must be a positive number!");
                return true;
            }

            restockService.setRestockLimitPerDay(limit);
            settingsService.saveSettingToDatabase("restock_limit_per_day", String.valueOf(limit));
            sender.sendMessage("§aSet villager restock limit to §e" + limit + " §aper day.");
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number! Please enter a valid restock limit.");
            return true;
        }
    }

    private boolean handleUnlimitedRestockCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /villagertrade unlimitedrestock <on|off>");
            return true;
        }

        if (args[1].equalsIgnoreCase("on")) {
            restockService.setUnlimitedRestock(true);
            settingsService.saveSettingToDatabase("unlimited_restock", "true");
            sender.sendMessage("§aUnlimited villager restock enabled!");
            return true;
        } else if (args[1].equalsIgnoreCase("off")) {
            restockService.setUnlimitedRestock(false);
            settingsService.saveSettingToDatabase("unlimited_restock", "false");
            sender.sendMessage("§aUnlimited villager restock disabled!");
            return true;
        } else {
            sender.sendMessage("§cUsage: /villagertrade unlimitedrestock <on|off>");
            return true;
        }
    }

    private boolean handleInstantRestockCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /villagertrade instantrestock <on|off>");
            return true;
        }

        if (args[1].equalsIgnoreCase("on")) {
            restockService.setInstantRestockEnabled(true);
            settingsService.saveSettingToDatabase("instant_restock_enabled", "true");
            sender.sendMessage("§aInstant villager restock enabled!");
            return true;
        } else if (args[1].equalsIgnoreCase("off")) {
            restockService.setInstantRestockEnabled(false);
            settingsService.saveSettingToDatabase("instant_restock_enabled", "false");
            sender.sendMessage("§aInstant villager restock disabled!");
            return true;
        } else {
            sender.sendMessage("§cUsage: /villagertrade instantrestock <on|off>");
            return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("villagertrade")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                List<String> commands = Arrays.asList(
                        "on", "off", "status", "setprice", "restock", "restocktime",
                        "restocklimit", "unlimitedrestock", "instantrestock"
                );

                StringUtil.copyPartialMatches(args[0], commands, completions);
                Collections.sort(completions);
                return completions;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("setprice")) {
                List<String> completions = new ArrayList<>();

                // Generate tab completions for all enchantment names
                for (org.bukkit.enchantments.Enchantment enchantment : org.bukkit.enchantments.Enchantment.values()) {
                    String enchantName = tradeService.getEnchantmentKey(enchantment);
                    completions.add(enchantName);
                }

                List<String> matches = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], completions, matches);
                Collections.sort(matches);
                return matches;
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("restock") ||
                    args[0].equalsIgnoreCase("unlimitedrestock") ||
                    args[0].equalsIgnoreCase("instantrestock"))) {
                List<String> completions = Arrays.asList("on", "off");
                List<String> matches = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], completions, matches);
                Collections.sort(matches);
                return matches;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setprice")) {
                List<String> completions = new ArrayList<>();

                // Get the enchantment name from args[1] to determine max level
                String enchantName = args[1].toLowerCase();

                // Find the enchantment to get its max level
                org.bukkit.enchantments.Enchantment targetEnchant = null;
                for (org.bukkit.enchantments.Enchantment enchantment : org.bukkit.enchantments.Enchantment.values()) {
                    if (tradeService.getEnchantmentKey(enchantment).equals(enchantName)) {
                        targetEnchant = enchantment;
                        break;
                    }
                }

                if (targetEnchant != null) {
                    // Add levels from 1 to max level
                    try {
                        for (int i = 1; i <= targetEnchant.getMaxLevel(); i++) {
                            completions.add(String.valueOf(i));
                        }
                    } catch (NoSuchMethodError e) {
                        // Fallback for older versions
                        completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
                    }
                } else {
                    // If enchantment not found, show levels 1-5 as fallback
                    completions.addAll(Arrays.asList("1", "2", "3", "4", "5"));
                }

                List<String> matches = new ArrayList<>();
                StringUtil.copyPartialMatches(args[2], completions, matches);
                Collections.sort(matches);
                return matches;
            } else if (args.length == 4 && args[0].equalsIgnoreCase("setprice")) {
                // Provide price suggestions (1-64)
                List<String> completions = Arrays.asList("10", "20", "30", "40", "50", "60", "64");

                List<String> matches = new ArrayList<>();
                StringUtil.copyPartialMatches(args[3], completions, matches);
                Collections.sort(matches);
                return matches;
            }
        }
        return Collections.emptyList();
    }
}