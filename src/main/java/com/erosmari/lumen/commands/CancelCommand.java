package com.erosmari.lumen.commands;

import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                .requires(source -> source.getSender().hasPermission("lumen.cancel")) // Validar permisos
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
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
            player.sendMessage(Component.text(TranslationHandler.get("command.cancel.success")).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text(TranslationHandler.get("command.cancel.no_task")).color(NamedTextColor.RED));
        }

        return 1; // Comando ejecutado con éxito
    }
}