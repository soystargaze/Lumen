package com.soystargaze.lumen.utils;

import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class ConsoleUtils {

    public static void displayAsciiArt(JavaPlugin plugin) {

        final String LOCAL_TEST_MESSAGE_KEY = "plugin.logo";
        TextHandler.get().registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, """
                
                  _                                  \s
                 | |                                 \s
                 | |    _   _ _ __ ___   ___ _ __    \s
                 | |   | | | | '_ ` _ \\ / _ \\ '_ \\\s
                 | |___| |_| | | | | | |  __/ | | |  \s
                 |______\\__,_|_| |_| |_|\\___|_| |_|\s
                """);
        TextHandler.get().logTranslated(LOCAL_TEST_MESSAGE_KEY);
        TextHandler.get().logTranslated("plugin.separator");
        TextHandler.get().logTranslated("plugin.name");
        TextHandler.get().logTranslated("plugin.version", plugin.getDescription().getVersion());
        TextHandler.get().logTranslated("plugin.author", plugin.getDescription().getAuthors());
        TextHandler.get().logTranslated("plugin.separator");
    }

    public static void displaySuccessMessage(JavaPlugin plugin) {

        TextHandler.get().logTranslated("plugin.separator");
        TextHandler.get().logTranslated("plugin.enabled");
        TextHandler.get().logTranslated("plugin.language_loaded", TextHandler.get().getActiveLanguage(), TextHandler.get().getLoadedTranslationsCount());
        TextHandler.get().logTranslated("database.initialized");
        TextHandler.get().logTranslated("items.registered");
        TextHandler.get().logTranslated("mobs.protected_areas_loaded");
        TextHandler.get().logTranslated("command.registered");
        TextHandler.get().logTranslated("events.registered");
        TextHandler.get().logTranslated("plugin.separator");
    }
}