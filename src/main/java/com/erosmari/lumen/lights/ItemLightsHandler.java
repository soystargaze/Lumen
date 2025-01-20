package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.tasks.TaskManager;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ItemLightsHandler {

    private final Lumen plugin;
    private final NamespacedKey lightKey;

    public ItemLightsHandler(Lumen plugin) {
        this.plugin = plugin;
        this.lightKey = new NamespacedKey(plugin, "lumen_light");
    }

    public void placeLights(Player player, Location center, String operationId) {
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage(TranslationHandler.get("light.error.no_world"));
            return;
        }

        int radius = ConfigHandler.getInt("settings.default_torch_radius", 20);
        int lightLevel = 15;
        int lightsPerTick = ConfigHandler.getInt("settings.torch_lights_per_tick", 10);
        int tickInterval = ConfigHandler.getInt("settings.torch_tick_interval", 10);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> blocksToLight = calculateLightPositions(center, radius);

            processBlocksAsync(player, blocksToLight, lightLevel, lightsPerTick, tickInterval, operationId);
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

    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, int lightsPerTick, int tickInterval, String operationId) {
        Queue<Location> blockQueue = new LinkedList<>(blocks);
        final BukkitTask[] taskHolder = new BukkitTask[1];

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!TaskManager.hasActiveTask(player.getUniqueId())) {
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.operation_cancelled", operationId));
                taskHolder[0].cancel();
                return;
            }

            int processed = 0;

            while (!blockQueue.isEmpty() && processed < lightsPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    placeLight(blockLocation, lightLevel, operationId);
                    processed++;
                }
            }

            if (blockQueue.isEmpty()) {
                player.sendMessage(TranslationHandler.getFormatted("light.success.placed", operationId));
                plugin.getLogger().info(TranslationHandler.getFormatted("light.info.completed_operation", operationId));
                taskHolder[0].cancel();
                TaskManager.cancelTask(player.getUniqueId());
            }
        }, 0L, tickInterval);

        TaskManager.addTask(player.getUniqueId(), taskHolder[0]);
    }

    private void placeLight(Location location, int lightLevel, String operationId) {
        Block block = location.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                LightRegistry.addBlock(location, lightLevel, operationId);
            } catch (ClassCastException e) {
                plugin.getLogger().warning(TranslationHandler.getFormatted("light.error.setting_level_torch", location, e.getMessage()));
            }
        }
    }

    public void removeLights(Player player, String operationId) {
        List<Location> blocksToRemove = LightRegistry.getBlocksByOperationId(operationId);

        if (blocksToRemove.isEmpty()) {
            player.sendMessage(TranslationHandler.getFormatted("light.error.no_lights_to_remove", operationId));
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
            player.sendMessage(TranslationHandler.getFormatted("light.success.removed", operationId));
            plugin.getLogger().info(TranslationHandler.getFormatted("light.info.removed_lights", operationId));
        });
    }

    public void cancelOperation(Player player, String operationId) {
        TaskManager.cancelTask(player.getUniqueId());
        removeLights(player, operationId);
        plugin.getLogger().info(TranslationHandler.getFormatted("light.info.cancelled_and_removed", operationId));
    }

    public boolean isLightTorch(Block block) {
        if (block.getType() == Material.PLAYER_HEAD) {
            if (block.getState() instanceof BlockStateMeta meta) {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                return "light".equals(container.get(lightKey, PersistentDataType.STRING));
            }
        }
        return false;
    }
}
