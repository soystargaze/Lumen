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
import java.util.logging.Logger;

public class RedoCommand {

    private static final Logger logger = Logger.getLogger("Lumen-RedoCommand");

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

        // Validar que sea un jugador
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        // Obtener el ID de la operación
        String operationId = context.getOrDefault("operation_id", "last");
        if (operationId.equals("last")) {
            operationId = LightRegistry.getLastSoftDeletedOperationId();
            if (operationId == null) {
                player.sendMessage("§eNo hay operaciones previas para rehacer.");
                return;
            }
        }

        // Recuperar los bloques asociados a la operación (incluyendo eliminados)
        List<Location> blocks = LightRegistry.getSoftDeletedBlocksByOperationId(operationId);

        if (blocks.isEmpty()) {
            player.sendMessage("§eNo se encontraron bloques para la operación: §b" + operationId);
            return;
        }

        logger.info("Restaurando bloques de luz para operation_id: " + operationId + ". Total bloques: " + blocks.size());

        int restoredCount = 0;
        for (Location blockLocation : blocks) {
            if (blockLocation.getWorld() == null) {
                logger.warning("El mundo para el bloque en " + blockLocation + " no está cargado.");
                continue;
            }

            // Restaurar el bloque como LIGHT
            blockLocation.getBlock().setType(Material.LIGHT);
            org.bukkit.block.data.Levelled lightData = (org.bukkit.block.data.Levelled) blockLocation.getBlock().getBlockData();

            // Recuperar el nivel de luz desde la base de datos
            int lightLevel = LightRegistry.getLightLevel(blockLocation);
            if (lightLevel < 0 || lightLevel > 15) {
                logger.warning("Nivel de luz inválido (" + lightLevel + ") para la ubicación: " + blockLocation);
                continue;
            }

            lightData.setLevel(lightLevel);
            blockLocation.getBlock().setBlockData(lightData, true);

            restoredCount++;
        }

        // Restaurar el estado de los bloques en la base de datos
        LightRegistry.restoreBlocksByOperationId(operationId);

        if (restoredCount > 0) {
            player.sendMessage("§aSe han restaurado " + restoredCount + " bloques de luz para la operación: §b" + operationId);
        } else {
            player.sendMessage("§eNo se restauraron bloques de luz en el mundo.");
        }
    }
}
