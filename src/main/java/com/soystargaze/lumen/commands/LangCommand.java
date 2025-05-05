package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.utils.LoggingUtils;
import com.soystargaze.lumen.utils.TranslationHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LangCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;

    public LangCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            LoggingUtils.logTranslated("command.only_players");
            return true;
        }

        if (!sender.hasPermission("lumen.lang")) {
            LoggingUtils.sendMessage(player,"command.no_permission");
            return true;
        }

        if (args.length != 1) {
            LoggingUtils.sendMessage(player,"command.lang.usage");
            return true;
        }

        String language = args[0];
        if (!TranslationHandler.isLanguageAvailable(language)) {
            LoggingUtils.sendMessage(player,"command.lang.file_not_found", language);
            return true;
        }

        try {
            TranslationHandler.setActiveLanguage(language);
            TranslationHandler.loadTranslations(plugin, language);

            plugin.getConfig().set("language", language);
            plugin.saveConfig();

            int loadedTranslations = TranslationHandler.getLoadedTranslationsCount();
            LoggingUtils.sendMessage(player,"command.lang.success", language, loadedTranslations);
        } catch (Exception e) {
            LoggingUtils.sendMessage(player,"command.lang.error", language);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            File translationsFolder = new File(plugin.getDataFolder(), "Translations");
            if (translationsFolder.exists() && translationsFolder.isDirectory()) {
                for (File file : Objects.requireNonNull(translationsFolder.listFiles())) {
                    if (file.isFile() && file.getName().endsWith(".yml")) {
                        suggestions.add(file.getName().replace(".yml", ""));
                    }
                }
            }
            return suggestions;
        }
        return null;
    }
}