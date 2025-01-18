package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return;
        }

        // Obtener el ID de la operación
        final String operationId = context.getOrDefault("operation_id", "last");
        final String resolvedOperationId = operationId.equals("last")
                ? LightRegistry.getLastSoftDeletedOperationId()
                : operationId;

        if (resolvedOperationId == null) {
            player.sendMessage("§eNo hay operaciones previas para rehacer.");
            return;
        }

        // Recuperar los bloques asociados a la operación
        List<Location> blocks = LightRegistry.getSoftDeletedBlocksByOperationId(resolvedOperationId);

        if (blocks.isEmpty()) {
            player.sendMessage("§eNo se encontraron bloques para la operación: §b" + resolvedOperationId);
            return;
        }

        logger.info("Restaurando bloques de luz para operation_id: " + resolvedOperationId + ". Total bloques: " + blocks.size());

        // Dividir bloques en lotes de 50
        int batchSize = 50;
        int totalBatches = (int) Math.ceil((double) blocks.size() / batchSize);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            int start = batchIndex * batchSize;
            int end = Math.min(start + batchSize, blocks.size());
            List<Location> batch = blocks.subList(start, end);

            // Programar el procesamiento de cada lote con retraso
            int delay = batchIndex * 10; // 10 ticks de retraso entre lotes
            Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Lumen")), () -> {
                int restoredCount = processBatch(batch, resolvedOperationId);
                logger.info("Lote restaurado: " + restoredCount + " bloques.");
            }, delay);
        }

        player.sendMessage("§aRestauración iniciada para " + blocks.size() + " bloques. Procesando en lotes...");
        logger.info("Restauración iniciada en " + totalBatches + " lotes.");
    }


    /**
     * Procesa un lote de bloques y los restaura en el mundo.
     *
     * @param batch       Lista de ubicaciones del lote.
     * @param operationId ID de la operación.
     * @return Número de bloques restaurados.
     */
    private static int processBatch(List<Location> batch, String operationId) {
        int restoredCount = 0;
        String newOperationId = UUID.randomUUID().toString();

        for (Location blockLocation : batch) {
            if (blockLocation.getWorld() == null) {
                logger.warning("El mundo para el bloque en " + blockLocation + " no está cargado.");
                continue;
            }

            Block block = blockLocation.getBlock();
            block.setType(Material.LIGHT, false);

            if (block.getType() == Material.LIGHT) {
                try {
                    Levelled lightData = (Levelled) block.getBlockData();
                    int lightLevel = LightRegistry.getLightLevel(blockLocation);
                    lightData.setLevel(lightLevel);
                    block.setBlockData(lightData, false);

                    LightRegistry.addBlock(blockLocation, lightLevel, newOperationId);
                    restoredCount++;
                } catch (ClassCastException e) {
                    logger.warning("Error al configurar el nivel de luz para el bloque en " + blockLocation + ": " + e.getMessage());
                }
            } else {
                logger.warning("No se pudo establecer el bloque como LIGHT en la ubicación: " + blockLocation);
            }
        }

        LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
        return restoredCount;
    }
}