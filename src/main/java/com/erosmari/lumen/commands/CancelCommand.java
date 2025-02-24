package com.erosmari.lumen.commands;

import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CancelCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("cancel")
                .requires(source -> source.getSender().hasPermission("lumen.cancel"))
                .executes(ctx -> handleCancelCommand(ctx.getSource()));
    }

    private static int handleCancelCommand(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.only_players"));
            LoggingUtils.logTranslated("command.only_players");
            return 0;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            LoggingUtils.sendAndLog(player,"command.cancel.success");
        } else {
            LoggingUtils.sendAndLog(player,"command.cancel.no_task");
        }

        return 1;
    }
}