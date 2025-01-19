package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.tasks.TaskManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ItemLightsHandler {

    private final Lumen plugin;

    public ItemLightsHandler(Lumen plugin) {
        this.plugin = plugin;
    }

    public void placeLights(Player player, Location center, String operationId) {
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage("§cError: No se pudo determinar el mundo.");
            return;
        }

        int radius = ConfigHandler.getInt("settings.default_torch_radius", 15); // Radio configurable
        int lightLevel = 15; // Nivel de luz fijo
        int lightsPerTick = ConfigHandler.getInt("settings.torch_lights_per_tick", 5);
        int tickInterval = ConfigHandler.getInt("settings.torch_tick_interval", 5);

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
        plugin.getLogger().info("Se calcularon " + positions.size() + " bloques para iluminar.");
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
            int processed = 0;

            while (!blockQueue.isEmpty() && processed < lightsPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    placeLight(blockLocation, lightLevel, operationId);
                    processed++;
                }
            }

            if (blockQueue.isEmpty()) {
                player.sendMessage("§aLuces colocadas con éxito. ID de operación: " + operationId);
                plugin.getLogger().info("Colocación completada para operación: " + operationId);
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
                plugin.getLogger().warning("Error al configurar el nivel de luz en " + location + ": " + e.getMessage());
            }
        }
    }

    public void removeLights(Player player, String operationId) {
        List<Location> blocksToRemove = LightRegistry.getBlocksByOperationId(operationId);

        if (blocksToRemove.isEmpty()) {
            player.sendMessage("§cNo se encontraron luces para eliminar con la operación: " + operationId);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            blocksToRemove.forEach(location -> {
                Block block = location.getBlock();
                if (block.getType() == Material.LIGHT) {
                    block.setType(Material.AIR, false);
                }
            });

            LightRegistry.removeBlocksByOperationId(operationId);
            player.sendMessage("§aSe eliminaron todas las luces generadas por la operación: " + operationId);
        });
    }
}
