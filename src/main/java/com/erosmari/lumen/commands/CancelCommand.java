package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CancelCommand {

    /**
     * Registra el comando `/lumen cancel`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("cancel")
                        .permission("lumen.cancel")
                        .handler(CancelCommand::handleCancelCommand)
        );
    }

    /**
     * Maneja el comando `/lumen cancel`.
     *
     * @param context Contexto del comando.
     */
    public static void handleCancelCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TranslationHandler.get("command.only_players"));
            return;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            player.sendMessage(TranslationHandler.get("command.cancel.success"));
        } else {
            player.sendMessage(TranslationHandler.get("command.cancel.no_task"));
        }
    }
}
