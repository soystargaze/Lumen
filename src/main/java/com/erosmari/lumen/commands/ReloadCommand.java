package com.erosmari.lumen.commands;

import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

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
            source.getSender().sendMessage(
                    TranslationHandler.getPlayerMessage("command.reload.success", loadedTranslations)
            );
            logInfo(source.getSender().getName(), loadedTranslations);
        } catch (Exception e) {
            logError(source.getSender().getName(), e);
            source.getSender().sendMessage(
                    TranslationHandler.getPlayerMessage("command.reload.error")
            );
        }
    }

    private void reloadConfig() {
        plugin.reloadConfig();
    }

    private int reloadTranslations() {
        TranslationHandler.clearTranslations();
        TranslationHandler.loadTranslations(plugin, plugin.getConfig().getString("language", "es_es"));
        return TranslationHandler.getLoadedTranslationsCount();
    }

    private void logInfo(Object... placeholders) {
        plugin.getLogger().info(TranslationHandler.getFormatted("command.reload.success_log", placeholders));
    }

    private void logError(String playerName, Exception exception) {
        plugin.getLogger().log(
                Level.SEVERE,
                TranslationHandler.getFormatted("command.reload.error_log", playerName),
                exception
        );
    }
}