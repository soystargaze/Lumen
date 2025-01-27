package com.erosmari.lumen.commands;

import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CancelCommand {

    /**
     * Registra el subcomando `/lumen cancel` en el sistema nativo de Paper.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("cancel")
                .requires(source -> source.getSender().hasPermission("lumen.cancel"))
                .executes(ctx -> handleCancelCommand(ctx.getSource()));
    }

    /**
     * Maneja el comando `/lumen cancel`.
     *
     * @param source Fuente del comando.
     * @return Código de éxito.
     */
    private static int handleCancelCommand(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.only_players"));
            return 0;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            player.sendMessage(TranslationHandler.getPlayerMessage("command.cancel.success"));
        } else {
            player.sendMessage(TranslationHandler.getPlayerMessage("command.cancel.no_task"));
        }

        return 1;
    }
}