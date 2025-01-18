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

public class RedoCommand {

    /**
     * Registra el comando `/lumen redo`.
     *
     * @param commandManager El administrador de comandos.
     * @param parentBuilder  El constructor del comando principal.
     */
    public static void register(CommandManager<CommandSender> commandManager, Command.Builder<CommandSender> parentBuilder) {
        commandManager.command(
                parentBuilder.literal("redo")
                        .argument(StringArgument.optional("operation_id")) // ID de la operación (opcional)
                        .permission("lumen.redo")
                        .handler(RedoCommand::handleRedoCommand)
        );
    }

    /**
     * Maneja el comando `/lumen redo`.
     */
    private static void handleRedoCommand(CommandContext<CommandSender> context) {
        CommandSender sender = context.getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        String operationId = context.getOrDefault("operation_id", "last");
        if (operationId.equals("last")) {
            operationId = LightRegistry.getLastSoftDeletedOperationId();
            if (operationId == null) {
                player.sendMessage("§eNo hay operaciones previas para rehacer.");
                return;
            }
        }

        // Obtener los bloques marcados como eliminados
        List<Location> blocks = LightRegistry.getSoftDeletedBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage("§eNo se encontraron bloques para la operación: §b" + operationId);
            return;
        }

        int restoredCount = 0;
        for (Location blockLocation : blocks) {
            if (blockLocation.getWorld() != null) {
                // Coloca el bloque de luz
                blockLocation.getBlock().setType(Material.LIGHT);

                // Ajusta el nivel de luz
                org.bukkit.block.data.Levelled lightData = (org.bukkit.block.data.Levelled) blockLocation.getBlock().getBlockData();
                int lightLevel = LightRegistry.getLightLevel(blockLocation);
                lightData.setLevel(lightLevel);
                blockLocation.getBlock().setBlockData(lightData, true);

                restoredCount++;
            } else {
                player.sendMessage("§eEl mundo para el bloque en " + blockLocation + " no está cargado.");
            }
        }

        // Restaurar el estado de los bloques en la base de datos
        LightRegistry.restoreBlocksByOperationId(operationId);

        player.sendMessage("§aSe han restaurado " + restoredCount + " bloques de luz para la operación: §b" + operationId);
    }
}