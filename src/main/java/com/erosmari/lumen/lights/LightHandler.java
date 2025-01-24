package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.connections.CoreProtectCompatibility; // Integración con CoreProtect
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.lights.integrations.FAWEHandler;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class LightHandler {

    private final Lumen plugin;
    private final CoreProtectCompatibility coreProtectCompatibility;

    public LightHandler(Lumen plugin) {
        this.plugin = plugin;
        this.coreProtectCompatibility = plugin.getCoreProtectCompatibility(); // Integración de CoreProtect
    }

    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight, String operationId) {
        Location center = player.getLocation();
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage(TranslationHandler.get("light.error.no_world"));
            return;
        }

        // Usamos CompletableFuture para calcular posiciones asíncronamente
        CompletableFuture.supplyAsync(() -> calculateLightPositions(center, areaBlocks, includeSkylight), AsyncExecutor.getExecutor())
                .thenAccept(blocksToLight -> {
                    if (blocksToLight.isEmpty()) {
                        player.sendMessage(TranslationHandler.get("light.error.no_blocks_found"));
                        return;
                    }
                    processBlocksAsync(player, blocksToLight, lightLevel, operationId);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().severe("Error al calcular posiciones: " + ex.getMessage());
                    return null;
                });
    }

    private List<Location> calculateLightPositions(Location center, int areaBlocks, boolean includeSkylight) {
        List<Location> positions = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning(TranslationHandler.get("light.warning.no_world"));
            return positions;
        }

        for (int x = -areaBlocks; x <= areaBlocks; x++) {
            for (int y = Math.max(center.getBlockY() - areaBlocks, world.getMinHeight());
                 y <= Math.min(center.getBlockY() + areaBlocks, world.getMaxHeight());
                 y++) {
                for (int z = -areaBlocks; z <= areaBlocks; z++) {
                    Location location = new Location(world, center.getBlockX() + x, y, center.getBlockZ() + z);

                    // Verificar si la posición es válida
                    if (isValidLightPosition(location, center, areaBlocks, includeSkylight)) {
                        positions.add(location);
                    }
                }
            }
        }

        plugin.getLogger().info(TranslationHandler.getFormatted("light.info.calculated_blocks", positions.size()));
        return positions;
    }

    private boolean isValidLightPosition(Location location, Location center, int maxDistance, boolean includeSkylight) {
        World world = location.getWorld();
        if (world == null) return false;

        Block block = location.getBlock();

        if (!block.getType().isAir()) {
            return false;
        }

        // Respetar el rango cúbico definido por "maxDistance"
        if (location.getBlockX() < center.getBlockX() - maxDistance
                || location.getBlockX() > center.getBlockX() + maxDistance
                || location.getBlockY() < center.getBlockY() - maxDistance
                || location.getBlockY() > center.getBlockY() + maxDistance
                || location.getBlockZ() < center.getBlockZ() - maxDistance
                || location.getBlockZ() > center.getBlockZ() + maxDistance) {
            return false;
        }

        if (!isAdjacentToSolidBlock(location)) {
            return false;
        }

        if (includeSkylight) {
            return block.getLightFromSky() > 0 && world.getHighestBlockYAt(location) <= location.getY();
        }

        return true;
    }

    private boolean isAdjacentToSolidBlock(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        int[][] offsets = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
        };

        for (int[] offset : offsets) {
            Location adjacent = location.clone().add(offset[0], offset[1], offset[2]);
            Block adjacentBlock = adjacent.getBlock();
            Material type = adjacentBlock.getType();

            if (!type.isAir() || type == Material.WATER || type == Material.LAVA) {
                return true;
            }
        }
        return false;
    }

    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, String operationId) {
        // Si FAWE está disponible, delegar la colocación de bloques a FAWE
        if (isFAWEAvailable()) {
            plugin.getLogger().info("FAWE detected. Delegating block placement to FAWE.");
            CompletableFuture.runAsync(() -> {
                try {
                    FAWEHandler.placeLightBlocks(blocks, lightLevel, player, plugin, coreProtectCompatibility);
                    LightRegistry.addBlocksAsync(blocks, lightLevel, operationId);
                } catch (Exception e) {
                    throw new RuntimeException("Error during FAWE block placement: " + e.getMessage(), e);
                }
            }).thenRun(() -> {
                player.sendMessage(TranslationHandler.getFormatted("light.success.completed", lightLevel, operationId));
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                DisplayUtil.hideBossBar(player);
                TaskManager.cancelTask(player.getUniqueId());
            }).exceptionally(ex -> {
                plugin.getLogger().severe(ex.getMessage());
                player.sendMessage(TranslationHandler.get("light.error.fawe_failed"));
                return null;
            });
            return;
        }

        plugin.getLogger().info("FAWE not detected. Using default block placement method.");
        int maxBlocksPerTick = ConfigHandler.getInt("settings.command_lights_per_tick", 1000);
        Queue<Location> blockQueue = new LinkedList<>(blocks);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processedCount = 0;
            int totalBlocks = blocks.size();

            while (!blockQueue.isEmpty() && processedCount < maxBlocksPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    processSingleBlock(player, blockLocation, lightLevel, operationId);
                    processedCount++;
                }
            }

            double progress = (double) (totalBlocks - blockQueue.size()) / totalBlocks;
            DisplayUtil.showBossBar(player, progress);
            DisplayUtil.showActionBar(player, progress);

            if (blockQueue.isEmpty()) {
                player.sendMessage(TranslationHandler.getFormatted("light.success.completed", lightLevel, operationId));
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                DisplayUtil.hideBossBar(player);
                TaskManager.cancelTask(player.getUniqueId());
            }
        }, 0L, 1L);

        TaskManager.addTask(player.getUniqueId(), task);
    }

    private boolean isFAWEAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    private void processSingleBlock(Player player, Location blockLocation, int lightLevel, String operationId) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                CoreProtectUtils.logLightPlacement(
                        plugin.getLogger(),
                        coreProtectCompatibility,
                        player.getName(),
                        List.of(blockLocation),
                        Material.LIGHT
                );

                BatchProcessor.addBlockToBatch(blockLocation, lightLevel, operationId);
            } catch (ClassCastException e) {
                plugin.getLogger().warning(TranslationHandler.getFormatted("light.error.setting_level", blockLocation, e.getMessage()));
            }
        }
    }
}