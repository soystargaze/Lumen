package com.erosmari.lumen.commands;

import com.erosmari.lumen.utils.TranslationHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public class LangCommand {

    private final JavaPlugin plugin;

    public LangCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register(JavaPlugin plugin) {
        return Commands.literal("lang")
                .requires(source -> source.getSender().hasPermission("lumen.lang"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("file", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            File translationsFolder = new File(plugin.getDataFolder(), "Translations");
                            if (translationsFolder.exists() && translationsFolder.isDirectory()) {
                                for (File file : Objects.requireNonNull(translationsFolder.listFiles())) {
                                    if (file.isFile() && file.getName().endsWith(".yml")) {
                                        builder.suggest(file.getName().replace(".yml", ""));
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String language = StringArgumentType.getString(context, "file");
                            new LangCommand(plugin).execute(context.getSource(), language);
                            return 1;
                        })
                );
    }

    public void execute(CommandSourceStack source, String language) {
        if (!TranslationHandler.isLanguageAvailable(language)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.lang.file_not_found", language));
            return;
        }

        try {
            TranslationHandler.setActiveLanguage(language);

            TranslationHandler.loadTranslations(plugin, language);

            plugin.getConfig().set("language", language);
            plugin.saveConfig();

            int loadedTranslations = TranslationHandler.getLoadedTranslationsCount();
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.lang.success", language, loadedTranslations));
        } catch (Exception e) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.lang.error", language));
        }
    }
}