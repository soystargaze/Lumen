package com.erosmari.lumen.config;

import com.erosmari.lumen.Lumen;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigHandler {

    private static FileConfiguration config;
    private static String language;

    public static void setup(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

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