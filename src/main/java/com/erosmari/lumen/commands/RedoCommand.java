package com.erosmari.lumen.commands;

import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.connections.CoreProtectCompatibility;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.utils.CoreProtectUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@SuppressWarnings("UnstableApiUsage")
public class RedoCommand {

    private static final Logger logger = Logger.getLogger("Lumen-RedoCommand");
    private final CoreProtectCompatibility coreProtectCompatibility;

    public RedoCommand(CoreProtectCompatibility coreProtectCompatibility) {
        this.coreProtectCompatibility = coreProtectCompatibility;
    }

    /**
     * Registra el subcomando `/lumen redo` en el sistema nativo de Paper.
     *
     * @return Nodo literal del comando para registrarlo en el comando principal.
     */
    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("redo")
                .requires(source -> source.getSender().hasPermission("lumen.redo"))
                .then(
                        Commands.argument("operation_id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String operationId = ctx.getArgument("operation_id", String.class);
                                    return handleRedoCommand(ctx.getSource(), operationId);
                                })
                )
                .executes(ctx -> handleRedoCommand(ctx.getSource(), "last"));
    }

    /**
     * Maneja el comando `/lumen redo`.
     *
     * @param source      Fuente del comando.
     * @param operationId Identificador de la operación.
     * @return Código de éxito (1 para éxito, 0 para fallo).
     */
    private int handleRedoCommand(CommandSourceStack source, String operationId) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(Component.text(TranslationHandler.get("command.only_players")).color(NamedTextColor.RED));
            return 0;
        }

        final String finalOperationId = operationId.equals("last")
                ? LightRegistry.getLastSoftDeletedOperationId()
                : operationId;

        if (finalOperationId == null) {
            player.sendMessage(Component.text(TranslationHandler.get("command.redo.no_previous_operations")).color(NamedTextColor.RED));
            return 0;
        }

        Map<Location, Integer> blocksWithLightLevels = LightRegistry.getSoftDeletedBlocksWithLightLevelByOperationId(finalOperationId);

        if (blocksWithLightLevels.isEmpty()) {
            player.sendMessage(Component.text(TranslationHandler.getFormatted("command.redo.no_blocks_found", finalOperationId)).color(NamedTextColor.RED));
            return 0;
        }

        logger.info(TranslationHandler.getFormatted("command.redo.restoring_blocks_log", finalOperationId, blocksWithLightLevels.size()));

        // Encapsular blockQueue en un wrapper mutable
        final Queue<Map.Entry<Location, Integer>> blockQueue = new LinkedList<>(blocksWithLightLevels.entrySet());
        final AtomicInteger processedCount = new AtomicInteger(0); // Contador mutable
        final int maxBlocksPerTick = ConfigHandler.getInt("settings.command_lights_per_tick", 1000);

        Bukkit.getScheduler().runTaskTimer(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Lumen")),
                task -> {
                    while (!blockQueue.isEmpty() && processedCount.get() < maxBlocksPerTick) {
                        Map.Entry<Location, Integer> entry = blockQueue.poll();
                        if (entry != null) {
                            processBlock(player, entry.getKey(), entry.getValue(), finalOperationId);
                            processedCount.incrementAndGet();
                        }
                    }

                    if (blockQueue.isEmpty()) {
                        logger.info(TranslationHandler.getFormatted("command.redo.restoration_completed_log", finalOperationId));
                        LightRegistry.restoreSoftDeletedBlocksByOperationId(finalOperationId);
                        task.cancel();
                    }
                },
                0L, 1L
        );

        player.sendMessage(Component.text(TranslationHandler.getFormatted("command.redo.restoration_started", blocksWithLightLevels.size(), maxBlocksPerTick))
                .color(NamedTextColor.GREEN));
        return 1;
    }

    /**
     * Procesa un solo bloque en el mundo.
     *
     * @param player       Jugador que ejecuta el comando.
     * @param blockLocation Ubicación del bloque.
     * @param lightLevel    Nivel de luz.
     * @param operationId   ID de la operación.
     */
    private void processBlock(Player player, Location blockLocation, int lightLevel, String operationId) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                // Usar el utilitario para registrar en CoreProtect
                CoreProtectUtils.logLightPlacement(logger, coreProtectCompatibility, player, blockLocation);

                LightRegistry.addBlock(blockLocation, lightLevel, operationId);
            } catch (ClassCastException e) {
                logger.warning(TranslationHandler.getFormatted("command.redo.light_level_error", blockLocation, e.getMessage()));
            }
        } else {
            logger.warning(TranslationHandler.getFormatted("command.redo.cannot_set_light", blockLocation));
        }
    }
}