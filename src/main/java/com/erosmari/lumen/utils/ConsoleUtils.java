package com.erosmari.lumen.utils;

import static com.erosmari.lumen.utils.TranslationHandler.loadedKeys;

public class ConsoleUtils {

    public static void displayAsciiArt() {

        final String LOCAL_TEST_MESSAGE_KEY = "plugin.logo";
        TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, """
                
                  _                                  \s
                 | |                                 \s
                 | |    _   _ _ __ ___   ___ _ __    \s
                 | |   | | | | '_ ` _ \\ / _ \\ '_ \\\s
                 | |___| |_| | | | | | |  __/ | | |  \s
                 |______\\__,_|_| |_| |_|\\___|_| |_|\s
                """);
        LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
    }

    public static void displaySuccessMessage() {

        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.enabled");
        LoggingUtils.logTranslated("plugin.language_loaded", TranslationHandler.getActiveLanguage(), loadedKeys);
        LoggingUtils.logTranslated("database.initialized");
        LoggingUtils.logTranslated("items.registered");
        LoggingUtils.logTranslated("mobs.protected_areas_loaded");
        LoggingUtils.logTranslated("command.registered");
        LoggingUtils.logTranslated("events.registered");
        LoggingUtils.logTranslated("plugin.separator");
    }
}