package com.erosmari.lumen.commands;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.lights.integrations.RedoFAWEHandler;
import com.erosmari.lumen.utils.BatchProcessor;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.DisplayUtil;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class RedoCommand {

    private final Lumen plugin;

    public RedoCommand(Lumen plugin) {
        this.plugin = plugin;
    }

    private CoreProtectHandler getCoreProtectHandler() {
        return plugin.getCoreProtectHandler();
    }

    public LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("redo")
                .requires(source -> source.getSender().hasPermission("lumen.redo"))
                .executes(ctx -> handleRedoCommand(ctx.getSource()));
    }

    private int handleRedoCommand(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage(TranslationHandler.getPlayerMessage("command.only_players"));
            LoggingUtils.logTranslated("command.only_players");
            return 0;
        }

        Integer operationId = LightRegistry.getLastSoftDeletedOperationId();
        if (operationId == null) {
            LoggingUtils.sendAndLog(player,"command.redo.no_previous_operations");
            return 0;
        }

        Map<Location, Integer> blocksWithLightLevels = LightRegistry.getSoftDeletedBlocksWithLightLevelByOperationId(operationId);
        if (blocksWithLightLevels.isEmpty()) {
            LoggingUtils.sendAndLog(player,"command.redo.no_blocks_found", operationId);
            return 0;
        }

        // Si FAWE está disponible, usar RedoFAWEHandler
        if (isFAWEAvailable()) {
            RedoFAWEHandler.handleRedoWithFAWE(plugin, player, blocksWithLightLevels, operationId);
        } else {
            // Si FAWE no está disponible, usar processBlock
            Queue<Map.Entry<Location, Integer>> blockQueue = new LinkedList<>(blocksWithLightLevels.entrySet());
            Queue<Map.Entry<Location, Integer>> failedQueue = new LinkedList<>();
            List<Location> processedBlocks = new ArrayList<>();
            int maxBlocksPerTick = ConfigHandler.getInt("settings.command_lights_per_tick", 1000);
            int totalBlocks = blockQueue.size();

            DisplayUtil.showBossBar(player, 0.0);

            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                int processedCount = 0;
                while (!blockQueue.isEmpty() && processedCount < maxBlocksPerTick) {
                    Map.Entry<Location, Integer> entry = blockQueue.poll();
                    if (entry != null) {
                        boolean success = processBlock(entry.getKey(), entry.getValue(), operationId, processedBlocks);
                        if (!success) {
                            failedQueue.add(entry);
                        }
                        processedCount++;
                    }
                }

                int remainingBlocks = blockQueue.size() + failedQueue.size();
                double progress = 1.0 - (double) remainingBlocks / totalBlocks;
                DisplayUtil.showBossBar(player, progress);
                DisplayUtil.showActionBar(player, progress);

                if (blockQueue.isEmpty() && failedQueue.isEmpty()) {
                    // Registrar todos los bloques procesados en CoreProtect al final
                    CoreProtectHandler coreProtectHandler = getCoreProtectHandler();
                    if (!processedBlocks.isEmpty() && coreProtectHandler != null) {
                        coreProtectHandler.logLightPlacement(player.getName(), processedBlocks, Material.LIGHT);
                    }

                    LightRegistry.restoreSoftDeletedBlocksByOperationId(operationId);
                    LoggingUtils.sendAndLog(player,"command.redo.restoration_completed", operationId);
                    DisplayUtil.hideBossBar(player);
                    task.cancel();
                } else if (blockQueue.isEmpty()) {
                    LoggingUtils.logTranslated("command.redo.retrying_failed_blocks");
                    blockQueue.addAll(failedQueue);
                    failedQueue.clear();
                }
            }, 0L, 1L);
        }

        LoggingUtils.sendAndLog(player,"command.redo.restoration_started", blocksWithLightLevels.size());
        return 1;
    }

    private boolean isFAWEAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    private boolean processBlock(Location blockLocation, int lightLevel, int operationId, List<Location> processedBlocks) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                // Agregar bloque procesado a la lista
                processedBlocks.add(blockLocation);

                // Registrar en la base de datos
                BatchProcessor.addBlockToBatch(blockLocation, lightLevel, operationId);
                return true;
            } catch (ClassCastException e) {
                LoggingUtils.logTranslated("command.redo.light_level_error", blockLocation, e.getMessage());
                return false;
            }
        } else {
            LoggingUtils.logTranslated("command.redo.cannot_set_light", blockLocation);
            return false;
        }
    }
}