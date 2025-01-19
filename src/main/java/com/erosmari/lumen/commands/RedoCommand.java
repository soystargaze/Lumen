package com.erosmari.lumen.commands;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
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

        final String operationId = context.getOrDefault("operation_id", "last").equals("last")
                ? LightRegistry.getLastSoftDeletedOperationId()
                : context.get("operation_id");

        if (operationId == null) {
            player.sendMessage("§eNo hay operaciones previas para rehacer.");
            return;
        }

        // Recuperar bloques y niveles de luz desde la base de datos
        Map<Location, Integer> blocksWithLightLevels = LightRegistry.getSoftDeletedBlocksWithLightLevelByOperationId(operationId);

        if (blocksWithLightLevels.isEmpty()) {
            player.sendMessage("§eNo se encontraron bloques para la operación: §b" + operationId);
            return;
        }

        logger.info("Restaurando bloques de luz para operation_id: " + operationId + ". Total bloques: " + blocksWithLightLevels.size());

        Queue<Map.Entry<Location, Integer>> blockQueue = new LinkedList<>(blocksWithLightLevels.entrySet());
        int maxBlocksPerTick = ConfigHandler.getInt("settings.command_lights_per_tick", 1000);

        Bukkit.getScheduler().runTaskTimer(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Lumen")),
                () -> {
                    int processedCount = 0;

                    while (!blockQueue.isEmpty() && processedCount < maxBlocksPerTick) {
                        Map.Entry<Location, Integer> entry = blockQueue.poll();
                        if (entry != null) {
                            processBlock(entry.getKey(), entry.getValue(), operationId);
                            processedCount++;
                        }
                    }

                    if (blockQueue.isEmpty()) {
                        logger.info("Restauración completada para operation_id: " + operationId);
                        LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
                        Bukkit.getScheduler().cancelTasks(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Lumen")));
                    }
                },
                0L, 1L
        );

        player.sendMessage("§aRestauración iniciada para " + blocksWithLightLevels.size() + " bloques. Procesando en lotes de hasta " + maxBlocksPerTick + " bloques por tick...");
    }

    /**
     * Procesa un solo bloque en el mundo.
     *
     * @param blockLocation Ubicación del bloque.
     * @param operationId   ID de la operación.
     */
    private static void processBlock(Location blockLocation, int lightLevel, String operationId) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                LightRegistry.addBlock(blockLocation, lightLevel, operationId);
            } catch (ClassCastException e) {
                logger.warning("Error al configurar el nivel de luz para el bloque en " + blockLocation + ": " + e.getMessage());
            }
        } else {
            logger.warning("No se pudo establecer el bloque como LIGHT en la ubicación: " + blockLocation);
        }
    }
}
