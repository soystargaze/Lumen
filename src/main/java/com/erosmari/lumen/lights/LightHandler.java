package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.connections.CoreProtectCompatibility; // Importar integración con CoreProtect
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.CoreProtectUtils;
import com.erosmari.lumen.utils.TranslationHandler;
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

public class LightHandler {

    private final Lumen plugin;
    private final CoreProtectCompatibility coreProtectCompatibility;

    public LightHandler(Lumen plugin) {
        this.plugin = plugin;
        this.coreProtectCompatibility = plugin.getCoreProtectCompatibility(); // Obtener integración de CoreProtect
    }

    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight, String operationId) {
        Location center = player.getLocation();
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage(TranslationHandler.get("light.error.no_world"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> blocksToLight = calculateLightPositions(center, areaBlocks, lightLevel, includeSkylight);
            processBlocksAsync(player, blocksToLight, lightLevel, operationId);
        });
    }

    private List<Location> calculateLightPositions(Location center, int areaBlocks, int lightLevel, boolean includeSkylight) {
        List<Location> positions = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning(TranslationHandler.get("light.warning.no_world"));
            return positions;
        }

        for (int x = -areaBlocks; x <= areaBlocks; x++) {
            for (int y = -areaBlocks; y <= areaBlocks; y++) {
                for (int z = -areaBlocks; z <= areaBlocks; z++) {
                    Location location = center.clone().add(x, y, z);

                    if (isValidLightPosition(location, center, lightLevel, includeSkylight)) {
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
            plugin.getLogger().info(TranslationHandler.getFormatted("light.info.not_air", location, block.getType()));
            return false;
        }

        double taxicabDistance = Math.abs(location.getBlockX() - center.getBlockX())
                + Math.abs(location.getBlockY() - center.getBlockY())
                + Math.abs(location.getBlockZ() - center.getBlockZ());
        if (taxicabDistance > maxDistance) {
            plugin.getLogger().info(TranslationHandler.getFormatted("light.info.out_of_range", location, taxicabDistance));
            return false;
        }

        if (!isAdjacentToSolidBlock(location)) {
            plugin.getLogger().info(TranslationHandler.getFormatted("light.info.no_adjacent_blocks", location));
            return false;
        }

        if (includeSkylight) {
            int highestY = world.getHighestBlockYAt(location);
            if (location.getBlockY() < highestY) {
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.not_sky_light", location));
                return false;
            }
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

            if (!adjacentBlock.getType().isAir()) {
                return true;
            }
        }
        return false;
    }

    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, String operationId) {
        int maxBlocksPerTick = ConfigHandler.getInt("settings.command_lights_per_tick", 1000);
        Queue<Location> blockQueue = new LinkedList<>(blocks);

        final BukkitTask[] taskHolder = new BukkitTask[1];

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                int processedCount = 0;

                while (!blockQueue.isEmpty() && processedCount < maxBlocksPerTick) {
                    Location blockLocation = blockQueue.poll();
                    if (blockLocation != null) {
                        processSingleBlock(player, blockLocation, lightLevel, operationId);
                        processedCount++;
                    }
                }

                if (blockQueue.isEmpty()) {
                    player.sendMessage(TranslationHandler.getFormatted("light.success.completed", lightLevel, operationId));
                    plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                    taskHolder[0].cancel();
                    TaskManager.cancelTask(player.getUniqueId());
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error while processing blocks asynchronously: " + e.getMessage());
            }
        }, 0L, 1L);

        TaskManager.addTask(player.getUniqueId(), taskHolder[0]);
    }

    private void processSingleBlock(Player player, Location blockLocation, int lightLevel, String operationId) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                // Registro en CoreProtect utilizando el utilitario
                CoreProtectUtils.logLightPlacement(plugin.getLogger(), coreProtectCompatibility, player.getName(), blockLocation);

                if (lightLevel >= 0 && lightLevel <= 15) {
                    LightRegistry.addBlock(blockLocation, lightLevel, operationId);
                } else {
                    plugin.getLogger().warning("Invalid block data for operation: " + operationId);
                }
            } catch (ClassCastException e) {
                plugin.getLogger().warning(TranslationHandler.getFormatted("light.error.setting_level", blockLocation, e.getMessage()));
            }
        }
    }
}