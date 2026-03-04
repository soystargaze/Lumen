package com.soystargaze.lumen.config;

import com.soystargaze.lumen.Lumen;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;
    private static Set<Material> excludedBlocks;
    private static int safetyMargin;

    public static void setup(JavaPlugin plugin) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (!created) {
                plugin.getLogger().severe("Could not create plugin data folder at " +
                        dataFolder.getAbsolutePath());
                return;
            }
        }

        File configFile = new File(dataFolder, "config.yml");

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
        Map<String, Object> userValues = new HashMap<>();

        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) {
            plugin.getLogger().severe("Default config.yml not found in plugin JAR!");
            return;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        for (String key : defaultConfig.getKeys(true)) {
            if (oldConfig.contains(key)) {
                userValues.put(key, oldConfig.get(key));
            }
        }

        plugin.saveResource("config.yml", true);

        FileConfiguration mergedConfig = YamlConfiguration.loadConfiguration(configFile);
        for (Map.Entry<String, Object> entry : userValues.entrySet()) {
            mergedConfig.set(entry.getKey(), entry.getValue());
        }

        try {
            mergedConfig.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error saving config.yml: " + e.getMessage());
        }

        config = mergedConfig;
        language = config.getString("language", "en_us");
        loadExcludedBlocks();
        safetyMargin = config.getInt("settings.safety.margin", 1);
    }

    private static void loadExcludedBlocks() {
        excludedBlocks = new HashSet<>();
        List<String> rawList = config.getStringList("settings.safety.excluded_blocks");
        for (String materialName : rawList) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                excludedBlocks.add(material);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @SuppressWarnings("unused")
    public static FileConfiguration getConfig() {
        return config;
    }

    public static void reload() {
        JavaPlugin plugin = Lumen.getInstance();
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadExcludedBlocks();
        safetyMargin = config.getInt("settings.safety.margin", 1);
    }

    public static Set<Material> getExcludedBlocks() {
        return excludedBlocks;
    }

    public static int getSafetyMargin() {
        return safetyMargin;
    }

    public static String getLanguage() {
        return language;
    }

    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public static String getString(String path, String def) {
        return config.getString(path, def);
    }

    public static boolean isBossBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.bossbar.enabled", true);
    }

    public static String getBossBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.bossbar.message", "Progress: {progress}% completed");
    }

    public static BossBar.Color getBossBarColor() {
        String color = Lumen.getInstance().getConfig().getString("display.bossbar.color", "GREEN");
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.GREEN;
        }
    }

    public static BossBar.Overlay getBossBarStyle() {
        String style = Lumen.getInstance().getConfig().getString("display.bossbar.style", "PROGRESS");
        // Adventure uses NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20, PROGRESS. 
        // Bukkit style SOLID maps to PROGRESS.
        if (style.equalsIgnoreCase("SOLID")) {
            return BossBar.Overlay.PROGRESS;
        }
        try {
            return BossBar.Overlay.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    public static boolean isActionBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.actionbar.enabled", true);
    }

    public static String getActionBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.actionbar.message", "Progreso: {progress}% completado");
    }

    public static boolean isSmartLightingEnabled() {
        return config.getBoolean("settings.smart_lighting.enabled", true);
    }

    public static int getSmartLightingSpacingFactor() {
        return config.getInt("settings.smart_lighting.spacing_factor", 5);
    }
}
