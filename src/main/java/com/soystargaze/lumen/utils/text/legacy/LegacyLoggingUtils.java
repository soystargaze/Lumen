package com.soystargaze.lumen.utils.text.legacy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class LegacyLoggingUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    private static String serialize(Component comp) {
        String withAmps = LEGACY_SERIALIZER.serialize(comp);
        return ChatColor.translateAlternateColorCodes('&', withAmps);
    }

    public static String getMessage(String key, Object... args) {
        Component c = LegacyTranslationHandler.getPlayerMessage(key, args);
        return serialize(c);
    }

    public static void logTranslated(String key, Object... args) {
        Component c = LegacyTranslationHandler.getLogMessage(key, args);
        Bukkit.getConsoleSender().sendMessage(serialize(c));
    }

    public static void sendMessage(Player player, String key, Object... args) {
        Component c = LegacyTranslationHandler.getPlayerMessage(key, args);
        player.sendMessage(serialize(c));
    }

    public static void sendAndLog(Player player, String key, Object... args) {
        Component c = LegacyTranslationHandler.getLogMessage(key, args);
        Bukkit.getConsoleSender().sendMessage(serialize(c));
        player.sendMessage(serialize(c));
    }
}