package com.erosmari.lumen.commands;

import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
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
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.reload")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        try {
            reloadConfig();
            int loadedTranslations = reloadTranslations();
            LoggingUtils.sendMessage(player,"command.reload.success", loadedTranslations);
        } catch (Exception e) {
            LoggingUtils.sendMessage(player,"command.reload.error");
            LoggingUtils.logTranslated("command.reload.error", e.getMessage());
        }
        return true;
    }

    private void reloadConfig() {
        plugin.reloadConfig();
        ConfigHandler.reload();
        ItemLightsHandler.reloadSettings();
    }

    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "es_es"));
        return TranslationHandler.getLoadedTranslationsCount();
    }
}