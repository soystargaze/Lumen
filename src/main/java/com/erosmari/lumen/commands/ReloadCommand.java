package com.erosmari.lumen.commands;

import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                .requires(source -> {
                    if (!source.getSender().hasPermission("lumen.reload")) {
                        source.getSender().sendMessage(Component.text("No tienes permiso para ejecutar este comando.").color(NamedTextColor.RED));
                        return false;
                    }
                    return true;
                })
                .executes(context -> {
                    new ReloadCommand(plugin).execute(context.getSource());
                    return 1; // Comando ejecutado con éxito
                });
    }

    public void execute(CommandSourceStack source) {
        try {
            reloadConfig();
            int loadedTranslations = reloadTranslations();
            sendMessage(source, "command.reload.success", NamedTextColor.GREEN, loadedTranslations);
            logInfo(source.getSender().getName(), loadedTranslations);
        } catch (Exception e) {
            logError(source.getSender().getName(), e);
            sendMessage(source, "command.reload.error", NamedTextColor.RED);
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

    private void sendMessage(CommandSourceStack source, String key, NamedTextColor color, Object... placeholders) {
        source.getSender().sendMessage(
                Component.text(TranslationHandler.getFormatted(key, placeholders)).color(color)
        );
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