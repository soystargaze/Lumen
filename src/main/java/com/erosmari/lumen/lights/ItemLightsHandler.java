package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.lights.integrations.ItemFAWEHandler;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.AsyncExecutor;
import com.erosmari.lumen.utils.DisplayUtil;
import com.erosmari.lumen.utils.LoggingUtils;
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
    private final CoreProtectHandler coreProtectHandler;
    private static int radius;
    private static int lightsPerTick;
    private static int tickInterval;

    public ItemLightsHandler(Lumen plugin) {
        this.plugin = plugin;
        this.coreProtectHandler = plugin.getCoreProtectHandler();
        reloadSettings();
    }

    public static void reloadSettings() {
        radius = ConfigHandler.getInt("settings.default_torch_radius", 20);
        lightsPerTick = ConfigHandler.getInt("settings.torch_lights_per_tick", 10);
        tickInterval = ConfigHandler.getInt("settings.torch_tick_interval", 10);
    }

    public void placeLights(Player player, Location center, int operationId, int lightLevel) {
        World world = center.getWorld();

        if (world == null) {
            LoggingUtils.sendAndLog(player, "light.error.no_world");
            return;
        }

        CompletableFuture.supplyAsync(() -> calculateLightPositions(center, radius), executor)
                .thenAcceptAsync(blocksToLight -> {
                    if (blocksToLight.isEmpty()) {
                        LoggingUtils.sendAndLog(player, "light.error.no_blocks_found");
                        return;
                    }

                    DisplayUtil.showBossBar(player, 0);
                    DisplayUtil.showActionBar(player, 0);

                    processBlocksAsync(player, blocksToLight, lightLevel, lightsPerTick, tickInterval, operationId);
                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> {
                    LoggingUtils.logTranslated("light.error.calculating_positions", ex.getMessage());
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
        LoggingUtils.logTranslated("light.info.calculated_blocks", positions.size());
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

    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, int lightsPerTick, int tickInterval, int operationId) {
        if (isFAWEAvailable()) {
            LoggingUtils.logTranslated("light.info.fawe_found");
            CompletableFuture.runAsync(() -> ItemFAWEHandler.placeLightsWithFAWE(player, blocks, lightLevel, operationId), executor)
                    .thenRun(() -> {
                        LoggingUtils.sendAndLog(player, "light.success.placed", operationId);
                        DisplayUtil.hideBossBar(player);
                        TaskManager.cancelTask(player.getUniqueId());
                    })
                    .exceptionally(ex -> {
                        LoggingUtils.sendAndLog(player, "light.error.fawe_failed", ex.getMessage());
                        return null;
                    });
            return;
        }

        LoggingUtils.logTranslated("light.info.fawe_not_found");
        Queue<Location> blockQueue = new LinkedList<>(blocks);
        final BukkitTask[] taskHolder = new BukkitTask[1];

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!TaskManager.hasActiveTask(player.getUniqueId())) {
                LoggingUtils.logTranslated("light.info.operation_cancelled", operationId);
                DisplayUtil.hideBossBar(player);
                TaskManager.cancelTask(player.getUniqueId());
                taskHolder[0].cancel();
                return;
            }

            int processed = 0;
            int totalBlocks = blocks.size();
            List<Location> placedBlocks = new LinkedList<>();

            while (!blockQueue.isEmpty() && processed < lightsPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    int validLightLevel = Math.max(0, Math.min(15, lightLevel)); // Limitar a 0-15
                    if (placeLight(blockLocation, validLightLevel, operationId)) {
                        placedBlocks.add(blockLocation);
                        processed++;
                    }
                }
            }

            int completed = totalBlocks - blockQueue.size();
            double progress = (double) completed / totalBlocks;

            DisplayUtil.showBossBar(player, progress);
            DisplayUtil.showActionBar(player, progress);

            if (blockQueue.isEmpty()) {
                if (coreProtectHandler != null && coreProtectHandler.isEnabled()) {
                    coreProtectHandler.logLightPlacement(player.getName(), placedBlocks, Material.LIGHT);
                }
                LoggingUtils.sendAndLog(player, "light.success.placed", operationId);
                DisplayUtil.hideBossBar(player);
                TaskManager.cancelTask(player.getUniqueId());
                taskHolder[0].cancel();
            }
        }, 0L, tickInterval);

        TaskManager.addTask(player.getUniqueId(), taskHolder[0]);
    }

    private boolean isFAWEAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    private boolean placeLight(Location location, int lightLevel, int operationId) {
        Block block = location.getBlock();
        if (!block.getType().isAir()) return false;

        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel); // Se usa el nivel de luz correcto
                block.setBlockData(lightData, false);
                LightRegistry.addBlockAsync(location, lightLevel, operationId);
                return true;
            } catch (ClassCastException e) {
                LoggingUtils.logTranslated("light.error.setting_level_torch", location, e.getMessage());
            }
        }
        return false;
    }

    public void removeLights(Player player, int operationId) {
        CompletableFuture.supplyAsync(() -> LightRegistry.getBlocksByOperationId(operationId), executor)
                .thenAcceptAsync(blocksToRemove -> {
                    if (blocksToRemove.isEmpty()) {
                        LoggingUtils.sendAndLog(player, "light.error.no_lights_to_remove", operationId);
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Location location : blocksToRemove) {
                            Block block = location.getBlock();
                            if (block.getType() == Material.LIGHT) {
                                block.setType(Material.AIR, false);
                            }
                        }
                        LightRegistry.removeBlocksByOperationId(operationId);
                    });

                    if (coreProtectHandler != null && coreProtectHandler.isEnabled()) {
                        coreProtectHandler.logRemoval(player.getName(), blocksToRemove, Material.LIGHT);
                    }
                });
    }

    public void cancelOperation(Player player, int operationId) {
        if (TaskManager.hasActiveTask(player.getUniqueId())) {
            TaskManager.cancelTask(player.getUniqueId());
        }

        removeLights(player, operationId);

        DisplayUtil.hideBossBar(player);
        LoggingUtils.sendAndLog(player,"light.success.removed", operationId);
    }
}