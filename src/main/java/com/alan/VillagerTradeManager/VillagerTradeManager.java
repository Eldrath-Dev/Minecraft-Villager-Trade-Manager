package com.alan.VillagerTradeManager;

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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

public final class VillagerTradeManager extends JavaPlugin implements Listener, TabExecutor {

    private boolean aaplaPluginChaluAhe = true;
    private BukkitRunnable velaKamavarChalava;
    private Map<String, Integer> kimapKosha = new ConcurrentHashMap<>();
    private File kimapFile;
    private boolean gavatKoshaAhe = true;
    private boolean dakhalkoshaAhe = true;

    @Override
    public void onEnable() {
        // प्लगइन सुरू करा
        serverCapabilitiesShodha();

        getServer().getPluginManager().registerEvents(this, this);

        // कमांड नोंदवा
        this.getCommand("villagertrade").setExecutor(this);
        this.getCommand("villagertrade").setTabCompleter(this);

        // किमत फाइल तयार करा
        kimapFile = new File(getDataFolder(), "kimat.dat");
        getDataFolder().mkdirs();

        // किमत लोड करा
        kimapLoadKara();

        // व्यापार नियंत्रण सुरू करा
        vyaparNiyamnirmanSuru();

        getLogger().info("माइनक्राफ्ट गावत व्यापार व्यवस्थापक प्लगइन सक्षम!");
        getLogger().info("गावत व्यापार अर्थव्यवस्थेवर पूर्ण नियंत्रण.");
        getLogger().info("माइनक्राफ्ट आवृत्ती 1.20.x - 1.21.x शी संगत.");
    }

    @Override
    public void onDisable() {
        // प्लगइन बंद करा
        if (velaKamavarChalava != null) {
            velaKamavarChalava.cancel();
        }

        // किमत जतन करा
        kimapJatankara();
    }

    private void serverCapabilitiesShodha() {
        // गावत प्रभाव उपलब्ध आहे का ते पहा
        try {
            org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE.getClass();
        } catch (NoSuchFieldError e) {
            gavatKoshaAhe = false;
            getLogger().info("गावत प्रभाव या सर्व्हर आवृत्तीवर उपलब्ध नाही");
        }

        // दाखल घटना उपलब्ध आहे का ते पहा
        try {
            Class.forName("org.bukkit.event.raid.RaidFinishEvent");
        } catch (ClassNotFoundException e) {
            dakhalkoshaAhe = false;
            getLogger().info("दाखल घटना या सर्व्हर आवृत्तीवर उपलब्ध नाही");
        }
    }

    @EventHandler
    public void gavatSampark(PlayerInteractEntityEvent ghatna) {
        if (!aaplaPluginChaluAhe) return;

        if (ghatna.getRightClicked().getType() == EntityType.VILLAGER) {
            Player kheladi = ghatna.getPlayer();

            // गावत प्रभाव काढून टाका
            if (gavatKoshaAhe) {
                gavatPrabhavKadha(kheladi);
            }

            Villager gavat = (Villager) ghatna.getRightClicked();

            // व्यापार प्रक्रिया
            gavatVyaparPrakriya(gavat);

            // उशीरा अद्यतनासाठी अतिरिक्त प्रक्रिया
            new BukkitRunnable() {
                @Override
                public void run() {
                    gavatVyaparPrakriya(gavat);
                }
            }.runTaskLater(this, 1L);
        }
    }

    @EventHandler
    public void gavatRupantar(EntityTransformEvent ghatna) {
        if (!aaplaPluginChaluAhe) return;

        if (ghatna.getEntity().getType() == EntityType.VILLAGER) {
            if (ghatna.getTransformReason() == EntityTransformEvent.TransformReason.CURED) {
                Villager gavat = (Villager) ghatna.getEntity();
                gavatVyaparPrakriya(gavat);
            }
        }
    }

    @EventHandler
    public void dakhalsamapti(RaidFinishEvent ghatna) {
        if (!aaplaPluginChaluAhe || !dakhalkoshaAhe) return;

        // विजेत्यांकडून गावत प्रभाव काढून टाका
        for (Player kheladi : ghatna.getWinners()) {
            if (gavatKoshaAhe) {
                gavatPrabhavKadha(kheladi);
            }
        }
    }

    private void vyaparNiyamnirmanSuru() {
        velaKamavarChalava = new BukkitRunnable() {
            @Override
            public void run() {
                if (!aaplaPluginChaluAhe) return;

                // सर्व खेळाडूंकडून गावत प्रभाव काढून टाका
                if (gavatKoshaAhe) {
                    for (Player kheladi : getServer().getOnlinePlayers()) {
                        gavatPrabhavKadha(kheladi);
                    }
                }

                // सर्व गावतांचे व्यापार प्रक्रिया
                for (Player kheladi : getServer().getOnlinePlayers()) {
                    try {
                        kheladi.getNearbyEntities(32, 32, 32).stream()
                                .filter(entity -> entity instanceof Villager)
                                .map(entity -> (Villager) entity)
                                .forEach(VillagerTradeManager.this::gavatVyaparPrakriya);
                    } catch (NoSuchMethodError e) {
                        // जुन्या आवृत्तींसाठी फॉलबॅक
                        for (org.bukkit.entity.Entity entity : kheladi.getWorld().getEntities()) {
                            if (entity instanceof Villager && entity.getLocation().distance(kheladi.getLocation()) <= 32) {
                                gavatVyaparPrakriya((Villager) entity);
                            }
                        }
                    }
                }
            }
        };
        velaKamavarChalava.runTaskTimer(this, 20L, 60L);
    }

    private void gavatVyaparPrakriya(Villager gavat) {
        if (!aaplaPluginChaluAhe) return;

        List<MerchantRecipe> vyajapeksha = new ArrayList<>();
        boolean badalAhe = false;

        try {
            for (MerchantRecipe vyajapekshaA : gavat.getRecipes()) {
                ItemStack nishkarsh = vyajapekshaA.getResult();

                // एनचँटेड बुक व्यापार आहे का ते पहा
                if (nishkarsh.getType() == Material.ENCHANTED_BOOK && nishkarsh.hasItemMeta()) {
                    // सानुकूल एनचँटेड बुक व्यापार तयार करा
                    MerchantRecipe sanukulVyajapeksha = sanukulEnchantedBookVyajapeksha(vyajapekshaA);
                    vyajapeksha.add(sanukulVyajapeksha);
                    badalAhe = true;
                } else {
                    // इतर सर्व व्यापारांसाठी, सूट काढून टाका
                    MerchantRecipe tatalaVyajapeksha = tatalaVyajapeksha(vyajapekshaA);
                    vyajapeksha.add(tatalaVyajapeksha);
                    badalAhe = true;
                }
            }

            // बदल झाल्यास व्यापार अद्यतनित करा
            if (badalAhe) {
                gavat.setRecipes(vyajapeksha);
            }
        } catch (NoSuchMethodError e) {
            getLogger().warning("या सर्व्हर आवृत्तीवर गावत व्यापार प्रक्रिया करू शकलो नाही");
        }
    }

    private MerchantRecipe sanukulEnchantedBookVyajapeksha(MerchantRecipe moolVyajapeksha) {
        ItemStack nishkarsh = moolVyajapeksha.getResult();

        // एनचँटमेंट माहिती मिळवा
        String enchantKey = "enchanted_book";

        if (nishkarsh.hasItemMeta() && nishkarsh.getItemMeta() instanceof EnchantmentStorageMeta) {
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) nishkarsh.getItemMeta();
            if (meta.hasStoredEnchants()) {
                // किमत निर्धारणासाठी पहिला एनचँटमेंट मिळवा
                Map.Entry<Enchantment, Integer> pahilaEnchant = meta.getStoredEnchants().entrySet().iterator().next();
                String enchantNaav = enchKeyMila(pahilaEnchant.getKey());
                int starach = pahilaEnchant.getValue();
                enchantKey = "enchanted_book_" + enchantNaav + "_" + starach;
            }
        }

        // साठवलेली किंवा मूळ किंमत मिळवा (कमाल 64 एमराल्ड्स)
        int emeraldKharch = Math.min(kimapKosha.getOrDefault(enchantKey, 15), 64);

        // सानुकूल किमत असलेले व्यापार तयार करा
        MerchantRecipe sanukulVyajapeksha = vyajapekshaBanva(
                nishkarsh,
                upyogMila(moolVyajapeksha),
                kamaalUpyogMila(moolVyajapeksha),
                anubhavInaamMila(moolVyajapeksha),
                gavatAnubhavMila(moolVyajapeksha),
                0.0f
        );

        // एनचँटेड बुकसाठी निश्चित किंमत सेट करा
        List<ItemStack> ghatke = new ArrayList<>();

        // पहिला घटक: अचूक 1 पुस्तक
        ItemStack pustake = new ItemStack(Material.BOOK, 1);
        ghatke.add(pustake);

        // दुसरा घटक: एमराल्ड्स (सानुकूल रक्कम, कमाल 64)
        ItemStack emeralds = new ItemStack(Material.EMERALD, emeraldKharch);
        ghatke.add(emeralds);

        sanukulVyajapeksha.setIngredients(ghatke);

        // सर्व सूट तंत्र काढून टाका
        maganMila(sanukulVyajapeksha, 0);
        visheshMulyaMila(sanukulVyajapeksha, 0);
        kimatiGunakarMila(sanukulVyajapeksha, 0.0f);

        return sanukulVyajapeksha;
    }

    private MerchantRecipe tatalaVyajapeksha(MerchantRecipe moolVyajapeksha) {
        MerchantRecipe tatalaVyajapeksha = vyajapekshaBanva(
                moolVyajapeksha.getResult(),
                upyogMila(moolVyajapeksha),
                kamaalUpyogMila(moolVyajapeksha),
                anubhavInaamMila(moolVyajapeksha),
                gavatAnubhavMila(moolVyajapeksha),
                0.0f
        );

        // घटक कॉपी करा
        tatalaVyajapeksha.setIngredients(new ArrayList<>(moolVyajapeksha.getIngredients()));

        // सर्व सूट तंत्र काढून टाका
        maganMila(tatalaVyajapeksha, 0);
        visheshMulyaMila(tatalaVyajapeksha, 0);
        kimatiGunakarMila(tatalaVyajapeksha, 0.0f);

        return tatalaVyajapeksha;
    }

    private void gavatPrabhavKadha(Player kheladi) {
        if (!aaplaPluginChaluAhe || !gavatKoshaAhe) return;

        // गावत प्रभाव काढून टाका
        try {
            kheladi.removePotionEffect(org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE);
        } catch (NoSuchMethodError e) {
            // जुन्या आवृत्तींसाठी फॉलबॅक
            try {
                Method removePotionEffectMethod = kheladi.getClass().getMethod("removePotionEffect", org.bukkit.potion.PotionEffectType.class);
                removePotionEffectMethod.invoke(kheladi, org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE);
            } catch (Exception ex) {
                // पद्धत नसल्यास दुर्लक्ष करा
            }
        }
    }

    // आवृत्ती संगतता पद्धती
    private String enchKeyMila(Enchantment ench) {
        try {
            return ench.getKey().getKey();
        } catch (NoSuchMethodError e) {
            // जुन्या आवृत्तींसाठी फॉलबॅक
            return ench.getName().toLowerCase();
        }
    }

    private int upyogMila(MerchantRecipe vyajapeksha) {
        try {
            return vyajapeksha.getUses();
        } catch (NoSuchMethodError e) {
            return 0;
        }
    }

    private int kamaalUpyogMila(MerchantRecipe vyajapeksha) {
        try {
            return vyajapeksha.getMaxUses();
        } catch (NoSuchMethodError e) {
            return 99999;
        }
    }

    private boolean anubhavInaamMila(MerchantRecipe vyajapeksha) {
        try {
            return vyajapeksha.hasExperienceReward();
        } catch (NoSuchMethodError e) {
            return false;
        }
    }

    private int gavatAnubhavMila(MerchantRecipe vyajapeksha) {
        try {
            return vyajapeksha.getVillagerExperience();
        } catch (NoSuchMethodError e) {
            return 0;
        }
    }

    private void maganMila(MerchantRecipe vyajapeksha, int magan) {
        try {
            vyajapeksha.setDemand(magan);
        } catch (NoSuchMethodError e) {
            // पद्धत उपलब्ध नाही, दुर्लक्ष करा
        }
    }

    private void visheshMulyaMila(MerchantRecipe vyajapeksha, int visheshMulya) {
        try {
            vyajapeksha.setSpecialPrice(visheshMulya);
        } catch (NoSuchMethodError e) {
            // पद्धत उपलब्ध नाही, दुर्लक्ष करा
        }
    }

    private void kimatiGunakarMila(MerchantRecipe vyajapeksha, float gunakar) {
        try {
            vyajapeksha.setPriceMultiplier(gunakar);
        } catch (NoSuchMethodError e) {
            // पद्धत उपलब्ध नाही, दुर्लक्ष करा
        }
    }

    private MerchantRecipe vyajapekshaBanva(ItemStack nishkarsh, int upyog, int kamaalUpyog, boolean anubhavInaam, int gavatAnubhav, float kimatiGunakar) {
        try {
            return new MerchantRecipe(nishkarsh, upyog, kamaalUpyog, anubhavInaam, gavatAnubhav, kimatiGunakar);
        } catch (NoSuchMethodError e) {
            // जुन्या आवृत्तींसाठी फॉलबॅक
            try {
                return new MerchantRecipe(nishkarsh, upyog, kamaalUpyog, anubhavInaam, gavatAnubhav, kimatiGunakar);
            } catch (Exception ex) {
                // अंतिम फॉलबॅक
                return new MerchantRecipe(nishkarsh, kamaalUpyog);
            }
        }
    }

    private void kimapJatankara() {
        try {
            if (!kimapFile.exists()) {
                kimapFile.createNewFile();
            }

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(kimapFile))) {
                oos.writeObject(new HashMap<>(kimapKosha));
            }
        } catch (IOException e) {
            getLogger().severe("किमत फाइलमध्ये जतन करू शकलो नाही: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void kimapLoadKara() {
        if (!kimapFile.exists()) {
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(kimapFile))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                kimapKosha = new ConcurrentHashMap<>((Map<String, Integer>) obj);
            }
        } catch (IOException | ClassNotFoundException e) {
            getLogger().severe("फाइलमधून किमत लोड करू शकलो नाही: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender paddhatik, Command aadesh, String chinha, String[] baki) {
        if (aadesh.getName().equalsIgnoreCase("villagertrade")) {
            if (baki.length == 0) {
                paddhatik.sendMessage("§cवापर: /villagertrade <on|off|status|setprice>");
                paddhatik.sendMessage("§7सध्याची स्थिती: " + (aaplaPluginChaluAhe ? "§aसक्रिय" : "§cनिष्क्रिय"));
                return true;
            }

            if (baki[0].equalsIgnoreCase("on")) {
                if (aaplaPluginChaluAhe) {
                    paddhatik.sendMessage("§cगावत व्यापार व्यवस्थापन आधीच सक्रिय आहे!");
                    return true;
                }

                aaplaPluginChaluAhe = true;
                paddhatik.sendMessage("§aगावत व्यापार व्यवस्थापन सक्रिय केले!");
                return true;
            } else if (baki[0].equalsIgnoreCase("off")) {
                if (!aaplaPluginChaluAhe) {
                    paddhatik.sendMessage("§cगावत व्यापार व्यवस्थापन आधीच निष्क्रिय आहे!");
                    return true;
                }

                aaplaPluginChaluAhe = false;
                paddhatik.sendMessage("§aगावत व्यापार व्यवस्थापन निष्क्रिय केले!");
                return true;
            } else if (baki[0].equalsIgnoreCase("status")) {
                paddhatik.sendMessage("§7गावत व्यापार व्यवस्थापन सध्या: " + (aaplaPluginChaluAhe ? "§aसक्रिय" : "§cनिष्क्रिय"));
                paddhatik.sendMessage("§7सानुकूल किमत:");
                if (kimapKosha.isEmpty()) {
                    paddhatik.sendMessage("  §7कोणतीही सानुकूल किमत सेट केलेली नाही");
                } else {
                    for (Map.Entry<String, Integer> nond : kimapKosha.entrySet()) {
                        // आंतरिक की स्वरूपातून वापरकर्ता-मैत्रीपूर्ण स्वरूपात रूपांतरित करा
                        String sadharanNaav = sadharanSvarupatRupantar(nond.getKey());
                        paddhatik.sendMessage("  §7" + sadharanNaav + ": §a" + nond.getValue() + " §7एमराल्ड्स");
                    }
                }
                return true;
            } else if (baki[0].equalsIgnoreCase("setprice")) {
                if (baki.length < 4) {
                    paddhatik.sendMessage("§cवापर: /villagertrade setprice <enchant> <level> <price>");
                    paddhatik.sendMessage("§7उदाहरणे:");
                    paddhatik.sendMessage("  §7/villagertrade setprice efficiency 1 20");
                    paddhatik.sendMessage("  §7/villagertrade setprice protection 4 35");
                    paddhatik.sendMessage("  §7/villagertrade setprice mending 1 50");
                    paddhatik.sendMessage("  §7/villagertrade setprice fortune 3 40");
                    return true;
                }

                String enchNaav = baki[1].toLowerCase();
                int starach;
                int kimat;

                try {
                    starach = Integer.parseInt(baki[2]);
                    kimat = Integer.parseInt(baki[3]);

                    if (starach <= 0) {
                        paddhatik.sendMessage("§cस्तर सकारात्मक संख्या असणे आवश्यक आहे!");
                        return true;
                    }

                    if (kimat <= 0) {
                        paddhatik.sendMessage("§cकिंमत सकारात्मक संख्या असणे आवश्यक आहे!");
                        return true;
                    }

                    // किमत कमाल 64 एमराल्ड्स पर्यंत मर्यादित करा
                    kimat = Math.min(kimat, 64);

                } catch (NumberFormatException e) {
                    paddhatik.sendMessage("§cअवैध स्तर किंवा किंमत! कृपया वैध संख्या प्रविष्ट करा.");
                    return true;
                }

                // वापरकर्ता-मैत्रीपूर्ण स्वरूपातून आंतरिक स्वरूपात रूपांतरित करा
                String vastuKey = "enchanted_book_" + enchNaav + "_" + starach;

                kimapKosha.put(vastuKey, kimat);
                kimapJatankara();
                paddhatik.sendMessage("§a" + enchNaav + " " + starach + " §7साठी सानुकूल किंमत §e" + kimat + " §aएमराल्ड्सवर सेट केली.");

                return true;
            } else {
                paddhatik.sendMessage("§cवापर: /villagertrade <on|off|status|setprice>");
                paddhatik.sendMessage("§7सध्याची स्थिती: " + (aaplaPluginChaluAhe ? "§aसक्रिय" : "§cनिष्क्रिय"));
                return true;
            }
        }
        return false;
    }

    private String sadharanSvarupatRupantar(String aantaranKey) {
        // "enchanted_book_efficiency_1" ला "efficiency 1" मध्ये रूपांतरित करा
        if (aantaranKey.startsWith("enchanted_book_")) {
            return aantaranKey.substring(15).replace("_", " ");
        }
        return aantaranKey;
    }

    @Override
    public List<String> onTabComplete(CommandSender paddhatik, Command aadesh, String chinha, String[] baki) {
        if (aadesh.getName().equalsIgnoreCase("villagertrade")) {
            if (baki.length == 1) {
                List<String> poornata = new ArrayList<>();
                List<String> aadeshList = new ArrayList<>();
                aadeshList.add("on");
                aadeshList.add("off");
                aadeshList.add("status");
                aadeshList.add("setprice");

                StringUtil.copyPartialMatches(baki[0], aadeshList, poornata);
                Collections.sort(poornata);
                return poornata;
            } else if (baki.length == 2 && baki[0].equalsIgnoreCase("setprice")) {
                List<String> poornata = new ArrayList<>();

                // सर्व एनचँटमेंट नावांसाठी टॅब पूर्णता व्युत्पन्न करा
                for (Enchantment ench : Enchantment.values()) {
                    String enchNaav = enchKeyMila(ench);
                    poornata.add(enchNaav);
                }

                List<String> jod = new ArrayList<>();
                StringUtil.copyPartialMatches(baki[1], poornata, jod);
                Collections.sort(jod);
                return jod;
            } else if (baki.length == 3 && baki[0].equalsIgnoreCase("setprice")) {
                List<String> poornata = new ArrayList<>();

                // एनचँटमेंट नाव मिळवा जास्तीच्या स्तरासाठी निर्धारित करण्यासाठी
                String enchNaav = baki[1].toLowerCase();

                // कमाल स्तर मिळवण्यासाठी एनचँटमेंट शोधा
                Enchantment targetEnch = null;
                for (Enchantment ench : Enchantment.values()) {
                    if (enchKeyMila(ench).equals(enchNaav)) {
                        targetEnch = ench;
                        break;
                    }
                }

                if (targetEnch != null) {
                    // 1 पासून कमाल स्तर पर्यंत स्तर जोडा
                    try {
                        for (int i = 1; i <= targetEnch.getMaxLevel(); i++) {
                            poornata.add(String.valueOf(i));
                        }
                    } catch (NoSuchMethodError e) {
                        // जुन्या आवृत्तींसाठी फॉलबॅक
                        poornata.add("1");
                        poornata.add("2");
                        poornata.add("3");
                        poornata.add("4");
                        poornata.add("5");
                    }
                } else {
                    // एनचँटमेंट न मिळाल्यास, फॉलबॅक म्हणून स्तर 1-5 दाखवा
                    poornata.add("1");
                    poornata.add("2");
                    poornata.add("3");
                    poornata.add("4");
                    poornata.add("5");
                }

                List<String> jod = new ArrayList<>();
                StringUtil.copyPartialMatches(baki[2], poornata, jod);
                Collections.sort(jod);
                return jod;
            } else if (baki.length == 4 && baki[0].equalsIgnoreCase("setprice")) {
                // किमत सूचना प्रदान करा (1-64)
                List<String> poornata = new ArrayList<>();
                poornata.add("10");
                poornata.add("20");
                poornata.add("30");
                poornata.add("40");
                poornata.add("50");
                poornata.add("60");
                poornata.add("64");

                List<String> jod = new ArrayList<>();
                StringUtil.copyPartialMatches(baki[3], poornata, jod);
                Collections.sort(jod);
                return jod;
            }
        }
        return Collections.emptyList();
    }
}