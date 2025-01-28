package com.erosmari.lumen.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LoggingUtils {

    public static void logTranslated(String key, Object... args) {
        // Obtener el mensaje traducido como componente
        Component translatedMessage = TranslationHandler.getLogMessage(key, args);

        // Mostrar gradientes en la consola con Adventure
        Bukkit.getConsoleSender().sendMessage(translatedMessage);
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component message = TranslationHandler.getPlayerMessage(key, args);

        player.sendMessage(message);
        logTranslated(key, args);
    }
}