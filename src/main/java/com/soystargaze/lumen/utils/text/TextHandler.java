package com.soystargaze.lumen.utils.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextHandler {

    private static TextHandler instance;
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private final Map<String, String> translations = new HashMap<>();
    private int loadedKeys = 0;
    private String activeLanguage = "en_us";
    private final JavaPlugin plugin;

    private TextHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getLogger().info("[Lumen] Initialized TextHandler (Paper API)");
    }

    public static void init(JavaPlugin plugin) {
        if (instance == null) {
            instance = new TextHandler(plugin);
        }
    }

    public static TextHandler get() {
        if (instance == null) {
            throw new IllegalStateException("TextHandler has not been initialized.");
        }
        return instance;
    }

    public void loadTranslations(JavaPlugin plugin, String language) {
        File translationsFolder = new File(plugin.getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create the translations folder.");
            return;
        }

        File langFile = new File(translationsFolder, language + ".yml");
        if (!langFile.exists()) {
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
                plugin.getLogger().severe("Error creating translation file: " + e.getMessage());
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(langFile);
        translations.clear();
        loadedKeys = 0;
        for (String key : cfg.getKeys(true)) {
            if (cfg.isString(key)) {
                translations.put(key, cfg.getString(key));
                loadedKeys++;
            }
        }
        activeLanguage = language;
    }

    public boolean isLanguageAvailable(String language) {
        File langFile = new File(plugin.getDataFolder(), "Translations/" + language + ".yml");
        return langFile.exists();
    }

    public void setActiveLanguage(String language) {
        if (isLanguageAvailable(language)) {
            activeLanguage = language;
        }
    }

    public String getActiveLanguage() {
        return activeLanguage;
    }

    public void registerTemporaryTranslation(String key, String message) {
        translations.putIfAbsent(key, message);
    }

    public int getLoadedTranslationsCount() {
        return loadedKeys;
    }

    public void sendMessage(Player player, String key, Object... args) {
        player.sendMessage(getComponent(key, true, args));
    }

    public void logTranslated(String key, Object... args) {
        Bukkit.getConsoleSender().sendMessage(getComponent(key, true, args));
    }

    public void sendAndLog(Player player, String key, Object... args) {
        Component c = getComponent(key, true, args);
        Bukkit.getConsoleSender().sendMessage(c);
        player.sendMessage(c);
    }

    public Component getMessage(String key, Object... args) {
        return getComponent(key, true, args);
    }

    private Component getComponent(String key, boolean usePrefix, Object... args) {
        String template = translations.getOrDefault(key, "Translation not found: " + key + "!");
        String prefix = usePrefix ? translations.getOrDefault(
                "plugin.prefix",
                "<color:#d4d4d4>[</color><gradient:#21FFCE:#D3FFAD>Lumen</gradient><color:#d4d4d4>]</color> "
        ) : "";
        
        String dynamicColor = translations.getOrDefault("plugin.dynamic_color", "<color:#21FFCE>");

        for (int i = 0; i < args.length; i++) {
            String argStr = String.valueOf(args[i]);
            String coloredArg = dynamicColor + argStr + "</color>";
            template = template.replace("{" + i + "}", coloredArg);
        }

        return MINI.deserialize(prefix + template);
    }
}
