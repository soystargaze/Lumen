package com.erosmari.lumen.commands;

import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("UnstableApiUsage")
public class ReloadCommand {

    private final JavaPlugin plugin;

    public ReloadCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(JavaPlugin plugin) {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("lumen.reload"))
                .executes(context -> {
                    new ReloadCommand(plugin).execute(context.getSource());
                    return 1;
                });
    }

    public void execute(CommandSourceStack source) {
        try {
            reloadConfig();
            int loadedTranslations = reloadTranslations();
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.reload.success", loadedTranslations));
        } catch (Exception e) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.reload.error"));
            LoggingUtils.logTranslated("command.reload.error", e.getMessage());
        }
    }

    private void reloadConfig() {
        plugin.reloadConfig();

        // Forzar la recarga de valores en otras clases que usan la configuración
        ConfigHandler.reload();
        ItemLightsHandler.reloadSettings();
    }

    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "es_es"));
        return TranslationHandler.getLoadedTranslationsCount();
    }
}