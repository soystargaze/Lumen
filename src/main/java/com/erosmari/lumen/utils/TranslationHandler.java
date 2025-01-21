package com.erosmari.lumen.utils;

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

    /**
     * Loads translations from the specified language file.
     *
     * @param plugin   Plugin instance for accessing the logger.
     * @param language The language to load (e.g., en_us, es_es).
     */
    public static void loadTranslations(JavaPlugin plugin, String language) {
        File translationsFolder = new File(plugin.getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create the translations folder.");
            return;
        }

        File langFile = new File(translationsFolder, language + ".yml");

        // Create default file if it doesn't exist
        if (!langFile.exists()) {
            createDefaultTranslationFile(plugin, langFile, language);
        }

        // Load translations from the file
        FileConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        loadedKeys = 0; // Reinicia el contador antes de cargar nuevas traducciones

        for (String key : langConfig.getKeys(true)) {
            if (langConfig.isString(key)) {
                translations.put(key, langConfig.getString(key));
                loadedKeys++;
            }
        }
    }

    /**
     * Creates the default translation file if it doesn't exist.
     *
     * @param plugin   Plugin instance for accessing the resource.
     * @param langFile Translation file to create.
     * @param language The language being created.
     */
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

    /**
     * Retrieves a translation by key.
     *
     * @param key The translation key.
     * @return The translated text, or an error message if not found.
     */
    public static String get(String key) {
        return translations.getOrDefault(key, "Translation not found: " + key + "!");
    }

    /**
     * Retrieves a formatted translation with dynamic placeholders.
     *
     * @param key  The translation key.
     * @param args The dynamic values to insert into the translation.
     * @return The translated text with placeholders replaced.
     */
    public static String getFormatted(String key, Object... args) {
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");
        for (int i = 0; i < args.length; i++) {
            template = template.replace("{" + i + "}", args[i].toString());
        }
        return template;
    }

    /**
     * Clears all loaded translations (useful for reloading).
     */
    public static void clearTranslations() {
        translations.clear();
    }

    /**
     * Displays the total number of loaded translations.
     *
     * @return The number of loaded translations.
     */
    public static int getLoadedTranslationsCount() {
        return translations.size();
    }
}