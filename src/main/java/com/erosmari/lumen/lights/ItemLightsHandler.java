package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.lights.integrations.ItemFAWEHandler;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.AsyncExecutor;
import com.erosmari.lumen.utils.DisplayUtil;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ItemLightsHandler {

    private final Lumen plugin;
    private final Executor executor = AsyncExecutor.getExecutor();


    public ItemLightsHandler(Lumen plugin) {
        this.plugin = plugin;
    }

    public void placeLights(Player player, Location center, int operationId) {
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage(TranslationHandler.getPlayerMessage("light.error.no_world"));
            return;
        }

        int radius = ConfigHandler.getInt("settings.default_torch_radius", 20);
        int lightLevel = 15;
        int lightsPerTick = ConfigHandler.getInt("settings.torch_lights_per_tick", 10);
        int tickInterval = ConfigHandler.getInt("settings.torch_tick_interval", 10);

        // Registrar tarea en TaskManager antes de iniciar
        TaskManager.addTask(player.getUniqueId(), null);

        // Calcular posiciones de bloques de luz de manera asíncrona
        CompletableFuture.supplyAsync(() -> calculateLightPositions(center, radius), executor)
                .thenAcceptAsync(blocksToLight -> {
                    // Mostrar BossBar inicial
                    DisplayUtil.showBossBar(player, 0);
                    DisplayUtil.showActionBar(player, 0);

                    // Registrar correctamente la tarea
                    processBlocksAsync(player, blocksToLight, lightLevel, lightsPerTick, tickInterval, operationId);
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Error calculating positions: " + ex.getMessage());
                    return null;
                });
    }

    private List<Location> calculateLightPositions(Location center, int radius) {
        List<Location> positions = new LinkedList<>();
        World world = center.getWorld();
        if (world == null) return positions;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location location = center.clone().add(x, y, z);

                    if (isValidLightPosition(location)) {
                        positions.add(location);
                    }
                }
            }
        }
        plugin.getLogger().info(TranslationHandler.getFormatted("light.info.calculated_blocks", positions.size()));
        return positions;
    }

    private boolean isValidLightPosition(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        Block block = location.getBlock();
        if (!block.getType().isAir()) return false;
        return isAdjacentToSolidBlock(location);
    }

    private boolean isAdjacentToSolidBlock(Location location) {
        int[][] offsets = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
        };

        for (int[] offset : offsets) {
            Location adjacent = location.clone().add(offset[0], offset[1], offset[2]);
            if (!adjacent.getBlock().getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    public void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, int lightsPerTick, int tickInterval, int operationId) {
        // Si FAWE está disponible, delegar la colocación de bloques al manejado de FAWE
        if (isFAWEAvailable()) {
            plugin.getLogger().info(TranslationHandler.getFormatted("light.info.fawe_found"));
            CompletableFuture.runAsync(() -> ItemFAWEHandler.placeLightsWithFAWE(plugin, player, blocks, lightLevel, operationId), executor)
                    .thenRun(() -> {
                        player.sendMessage(TranslationHandler.getPlayerMessage("light.success.placed", operationId));
                        plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                        DisplayUtil.hideBossBar(player);
                        TaskManager.cancelTask(player.getUniqueId());
                    })
                    .exceptionally(ex -> {
                        player.sendMessage(TranslationHandler.getPlayerMessage("light.error", ex.getMessage()));
                        plugin.getLogger().severe("Error during FAWE block placement: " + ex.getMessage());
                        return null;
                    });
            return;
        }

        // Lógica original si FAWE no está disponible
        plugin.getLogger().info(TranslationHandler.getFormatted("light.info.fawe_not_found"));
        Queue<Location> blockQueue = new LinkedList<>(blocks);
        final BukkitTask[] taskHolder = new BukkitTask[1];

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!TaskManager.hasActiveTask(player.getUniqueId())) {
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.operation_cancelled", operationId));
                DisplayUtil.hideBossBar(player);
                TaskManager.cancelTask(player.getUniqueId());
                taskHolder[0].cancel();
                return;
            }

            int processed = 0;
            int totalBlocks = blocks.size();

            while (!blockQueue.isEmpty() && processed < lightsPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    placeLight(blockLocation, lightLevel, operationId);
                    processed++;
                }
            }

            int completed = totalBlocks - blockQueue.size();
            double progress = (double) completed / totalBlocks;

            DisplayUtil.showBossBar(player, progress);
            DisplayUtil.showActionBar(player, progress);

            if (blockQueue.isEmpty()) {
                player.sendMessage(TranslationHandler.getPlayerMessage("light.success.placed", operationId));
                DisplayUtil.hideBossBar(player);
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                TaskManager.cancelTask(player.getUniqueId());
                taskHolder[0].cancel();
            }
        }, 0L, tickInterval);

        TaskManager.addTask(player.getUniqueId(), taskHolder[0]);
    }

    private boolean isFAWEAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    private void placeLight(Location location, int lightLevel, int operationId) {
        Block block = location.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                if (lightLevel >= 0 && lightLevel <= 15) {
                    LightRegistry.addBlockAsync(location, lightLevel, operationId);
                } else {
                    plugin.getLogger().warning("Invalid block data for operation: " + operationId + " at location: " + location);
                }
            } catch (ClassCastException e) {
                plugin.getLogger().warning(TranslationHandler.getFormatted("light.error.setting_level_torch", location, e.getMessage()));
            }
        }
    }

    public void removeLights(Player player, int operationId) {
        CompletableFuture.supplyAsync(() -> LightRegistry.getBlocksByOperationId(operationId), executor)
                .thenAcceptAsync(blocksToRemove -> {
                    if (blocksToRemove.isEmpty()) {
                        player.sendMessage(TranslationHandler.getPlayerMessage("light.error.no_lights_to_remove", operationId));
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        blocksToRemove.forEach(location -> {
                            Block block = location.getBlock();
                            if (block.getType() == Material.LIGHT) {
                                block.setType(Material.AIR, false);
                            }
                        });

                        LightRegistry.removeBlocksByOperationId(operationId);
                        player.sendMessage(TranslationHandler.getPlayerMessage("light.success.removed", operationId));
                    });
                });
    }
    public void cancelOperation(Player player, int operationId) {
        // Cancelar la tarea activa (si existe)
        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
        }

        // Eliminar luces asociadas a la operación
        removeLights(player, operationId);

        // Mensajes de feedback
        DisplayUtil.hideBossBar(player);
        player.sendMessage(TranslationHandler.getPlayerMessage("light.success.removed", operationId));
    }
}