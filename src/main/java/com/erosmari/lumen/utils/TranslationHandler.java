package com.erosmari.lumen.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TranslationHandler {

    private static final Map<String, String> translations = new HashMap<>();
    public static int loadedKeys = 0;

    public static void loadTranslations(JavaPlugin plugin, String language) {
        File translationsFolder = new File(plugin.getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create the translations folder.");
            return;
        }

        File langFile = new File(translationsFolder, language + ".yml");

        if (!langFile.exists()) {
            createDefaultTranslationFile(plugin, langFile, language);
        }

        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        loadedKeys = 0;

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                translations.put(key, langConfig.getString(key));
                loadedKeys++;
            }
        }
    }

    private static void createDefaultTranslationFile(JavaPlugin plugin, File langFile, String language) {
        try {
            if (langFile.createNewFile()) {
                String resourcePath = "Translations/" + language + ".yml";
                if (plugin.getResource(resourcePath) != null) {
                    plugin.saveResource(resourcePath, false);
                    plugin.getLogger().info("Default translation file '" + language + ".yml' created.");
                } else {
                    plugin.getLogger().warning("Default resource not found for '" + language + ".yml'.");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create the translation file: " + langFile.getName());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Error creating the translation file", e);
        }
    }

    public static String get(String key) {
        return translations.getOrDefault(key, "Translation not found: " + key + "!");
    }

    public static String getFormatted(String key, Object... args) {
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i].toString());
        }
        return template;
    }

    public static void clearTranslations() {
        translations.clear();
    }

    public static int getLoadedTranslationsCount() {
        return translations.size();
    }

    public static Component getPlayerMessage(String key, Object... args) {
        String prefix = "<color:#d4d4d4>[</color><gradient:#21FFCE:#D3FFAD>Lumen</gradient><color:#d4d4d4>]</color> ";
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");

        for (int i = 0; i < args.length; i++) {
            String coloredArg = "<color:#21FFCE>" + args[i].toString() + "</color>";
            template = template.replace("{" + i + "}", coloredArg);
        }

        String fullMessage = prefix + template;

        // Reemplazar códigos '&' por '§' para procesar como códigos legacy
        if (fullMessage.contains("&")) {
            fullMessage = fullMessage.replace("&", "§");
        }

        // Detectar códigos legacy (§) y procesar con LegacyComponentSerializer
        if (fullMessage.contains("§")) {
            return LegacyComponentSerializer.legacySection().deserialize(fullMessage);
        }

        // Si no hay códigos legacy, procesar con MiniMessage
        return MiniMessage.miniMessage().deserialize(fullMessage);
    }
}