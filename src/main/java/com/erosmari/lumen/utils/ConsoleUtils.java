package com.erosmari.lumen.utils;

import com.erosmari.lumen.config.ConfigHandler;
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
                " |______\\__,_|_| |_| |_|\\___|_| |_| \n"
        );
    }

    /**
     * Muestra el mensaje de éxito en la consola.
     *
     * @param plugin El plugin que ejecuta el mensaje.
     * */
    public static void displaySuccessMessage(JavaPlugin plugin) {

        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.enabled");
        LoggingUtils.logTranslated("plugin.language_loaded", ConfigHandler.getLanguage(), loadedKeys);
        LoggingUtils.logTranslated("database.initialized");
        LoggingUtils.logTranslated("items.registered");
        LoggingUtils.logTranslated("mobs.protected_areas_loaded");
        LoggingUtils.logTranslated("command.registered");
        LoggingUtils.logTranslated("events.registered");
        LoggingUtils.logTranslated("plugin.separator");
    }
}