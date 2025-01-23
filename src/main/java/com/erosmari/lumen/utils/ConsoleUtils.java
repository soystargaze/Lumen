package com.erosmari.lumen.utils;

import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.plugin.java.JavaPlugin;

import static com.erosmari.lumen.utils.TranslationHandler.loadedKeys;

@SuppressWarnings("ALL")
public class ConsoleUtils {

    /**
     * Muestra el arte ASCII personalizado en la consola.
     *
     * @param plugin El plugin que ejecuta el mensaje.
     */
    public static void displayAsciiArt(JavaPlugin plugin) {
        plugin.getLogger().info("\n" +
                "  _                                   \n" +
                " | |                                  \n" +
                " | |    _   _ _ __ ___   ___ _ __     \n" +
                " | |   | | | | '_ ` _ \\ / _ \\ '_ \\ \n" +
                " | |___| |_| | | | | | |  __/ | | |   \n" +
                " |______\\__,_|_| |_| |_|\\___|_| |_| \n" +
                "                                      \n"
        );
    }

    /**
     * Muestra el mensaje de éxito en la consola.
     *
     * @param plugin El plugin que ejecuta el mensaje.
     * */
    public static void displaySuccessMessage(JavaPlugin plugin, CoreProtectCompatibility coreProtectCompatibility) {

        plugin.getLogger().info("--------------------------------------------");
        plugin.getLogger().info(TranslationHandler.get("plugin.enabled"));
        plugin.getLogger().info(TranslationHandler.getFormatted("plugin.language_loaded", ConfigHandler.getLanguage(), loadedKeys));
        plugin.getLogger().info(TranslationHandler.get("database.initialized"));
        plugin.getLogger().info(TranslationHandler.get("items.registered"));
        plugin.getLogger().info(TranslationHandler.get("mobs.protected_areas_loaded"));
        plugin.getLogger().info(TranslationHandler.get("command.registered"));
        plugin.getLogger().info(TranslationHandler.get("events.registered"));
        if (coreProtectCompatibility != null && coreProtectCompatibility.isEnabled()) {
            plugin.getLogger().info(TranslationHandler.get("coreprotect.enabled"));
        } else {
            plugin.getLogger().warning(TranslationHandler.get("coreprotect.unavailable"));
        }
        plugin.getLogger().info("--------------------------------------------");
    }
}