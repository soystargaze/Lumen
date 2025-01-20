package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class UndoCommand {

    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("undo")
                        .argument(StringArgument.optional("operation_id")) // Argumento opcional para el identificador
                        .permission("lumen.undo")
                        .handler(UndoCommand::handleUndoCommand)
        );
    }

    private static void handleUndoCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player)) {
            sender.sendMessage(TranslationHandler.get("command.undo.only_players")); // Mensaje traducido
            return;
        }

        String operationId = context.getOrDefault("operation_id", "last");
        if (operationId.equals("last")) {
            // Obtener el último operation_id
            operationId = LightRegistry.getLastOperationId();
            if (operationId == null) {
                sender.sendMessage(TranslationHandler.get("command.undo.no_previous_operations")); // Mensaje traducido
                return;
            }
        }

        int removedBlocks = removeLightBlocksByOperation(operationId);

        if (removedBlocks > 0) {
            sender.sendMessage(TranslationHandler.getFormatted("command.undo.success", removedBlocks, operationId)); // Mensaje traducido
        } else {
            sender.sendMessage(TranslationHandler.getFormatted("command.undo.no_blocks", operationId)); // Mensaje traducido
        }
    }

    private static int removeLightBlocksByOperation(String operationId) {
        List<Location> blocks = LightRegistry.getBlocksByOperationId(operationId);
        if (blocks.isEmpty()) {
            return 0;
        }

        int removedCount = 0;
        for (Location location : blocks) {
            if (location.getWorld() != null && location.getBlock().getType() == Material.LIGHT) {
                location.getBlock().setType(Material.AIR);
                removedCount++;
            }
        }

        // En lugar de eliminar los registros, los marcamos como "soft delete" para poder rehacer la operación
        LightRegistry.softDeleteBlocksByOperationId(operationId);

        return removedCount;
    }
}
