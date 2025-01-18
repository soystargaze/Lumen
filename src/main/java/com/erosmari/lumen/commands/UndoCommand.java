package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
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
            sender.sendMessage("§cSolo los jugadores pueden usar este comando."); // Mensaje en rojo
            return;
        }

        String operationId = context.getOrDefault("operation_id", "last");
        if (operationId.equals("last")) {
            // Obtener el último operation_id
            operationId = LightRegistry.getLastOperationId();
            if (operationId == null) {
                sender.sendMessage("§eNo hay operaciones previas para deshacer."); // Mensaje en amarillo
                return;
            }
        }

        int removedBlocks = removeLightBlocksByOperation(operationId);

        if (removedBlocks > 0) {
            sender.sendMessage("§aSe han eliminado " + removedBlocks + " bloques de luz para la operación: " + operationId); // Mensaje en verde
        } else {
            sender.sendMessage("§eNo se encontraron bloques para la operación: " + operationId); // Mensaje en amarillo
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