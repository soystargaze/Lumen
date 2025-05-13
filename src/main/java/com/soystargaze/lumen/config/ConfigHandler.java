package com.soystargaze.lumen.config;

import com.soystargaze.lumen.Lumen;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;

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
    }

    @SuppressWarnings("unused")
    public static FileConfiguration getConfig() {
        return config;
    }

    public static void reload() {
        JavaPlugin plugin = Lumen.getInstance();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public static String getLanguage() {
        return language;
    }

    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    public static boolean isBossBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.bossbar.enabled", true);
    }

    public static String getBossBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.bossbar.message", "Progress: {progress}% completed");
    }

    public static BarColor getBossBarColor() {
        String color = Lumen.getInstance().getConfig().getString("display.bossbar.color", "GREEN");
        try {
            return BarColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }

    public static BarStyle getBossBarStyle() {
        String style = Lumen.getInstance().getConfig().getString("display.bossbar.style", "SOLID");
        try {
            return BarStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    public static boolean isActionBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.actionbar.enabled", true);
    }

    public static String getActionBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.actionbar.message", "Progreso: {progress}% completado");
    }
}