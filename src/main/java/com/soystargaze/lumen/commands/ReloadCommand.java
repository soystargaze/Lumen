package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.config.ConfigHandler;
import com.soystargaze.lumen.lights.ItemLightsHandler;
import com.soystargaze.lumen.utils.text.TextHandler;
import com.soystargaze.lumen.utils.text.legacy.LegacyTranslationHandler;
import com.soystargaze.lumen.utils.text.modern.ModernTranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextHandler.get().logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.reload")) {
            TextHandler.get().sendMessage(player,"command.no_permission");
            return true;
        }

        try {
            reloadConfig();
            int loadedTranslations = reloadTranslations();
            TextHandler.get().sendMessage(player,"command.reload.success", loadedTranslations);
        } catch (Exception e) {
            TextHandler.get().sendMessage(player,"command.reload.error");
            TextHandler.get().logTranslated("command.reload.error", e.getMessage());
        }
        return true;
    }

    private void reloadConfig() {
        plugin.reloadConfig();
        ConfigHandler.reload();
        ItemLightsHandler.reloadSettings();
    }

    private int reloadTranslations() {
        String language = plugin.getConfig().getString("language", "en_us");
        if (Bukkit.getServer().getName().equalsIgnoreCase("Paper")) {
            ModernTranslationHandler.clearTranslations();
            ModernTranslationHandler.loadTranslations(plugin, language);
            return ModernTranslationHandler.getLoadedTranslationsCount();
        } else {
            LegacyTranslationHandler.clearTranslations();
            LegacyTranslationHandler.loadTranslations(plugin, language);
            return LegacyTranslationHandler.getLoadedTranslationsCount();
        }
    }
}