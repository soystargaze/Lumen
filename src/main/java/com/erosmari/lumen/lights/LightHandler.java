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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LightHandler {

    private final Lumen plugin;

    public LightHandler(Lumen plugin) {
        this.plugin = plugin;
    }

    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight, String operationId) {
        Location center = player.getLocation();
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage("§cError: No se pudo determinar el mundo.");
            return;
        }

        // Ejecutar la recopilación y procesamiento de bloques asíncronamente
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> blocksToLight = new ArrayList<>();

            // Recopilar bloques que deben iluminarse
            for (int x = -areaBlocks; x <= areaBlocks; x++) {
                for (int y = -areaBlocks; y <= areaBlocks; y++) {
                    for (int z = -areaBlocks; z <= areaBlocks; z++) {
                        Location blockLocation = center.clone().add(x, y, z);
                        if (shouldPlaceLight(blockLocation, includeSkylight)) {
                            blocksToLight.add(blockLocation);
                        }
                    }
                }
            }

            // Procesar los bloques en lotes
            processBlocksAsync(player, blocksToLight, lightLevel, operationId);
        });
    }

    private boolean shouldPlaceLight(Location location, boolean includeSkylight) {
        if (!location.getBlock().getType().isAir()) {
            return false;
        }
        return includeSkylight || location.getWorld().getHighestBlockYAt(location) > location.getBlockY();
    }

    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, String operationId) {
        int maxBlocksPerTick = ConfigHandler.getInt("settings.light_per_tick_with_command", 1000);
        Queue<Location> blockQueue = new LinkedList<>(blocks);

        // Usar un array para referencia mutable de la tarea
        final BukkitTask[] taskHolder = new BukkitTask[1];

        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processedCount = 0;

            while (!blockQueue.isEmpty() && processedCount < maxBlocksPerTick) {
                Location blockLocation = blockQueue.poll();
                if (blockLocation != null) {
                    processSingleBlock(blockLocation, lightLevel, operationId);
                    processedCount++;
                }
            }

            if (blockQueue.isEmpty()) {
                player.sendMessage("§aSe completó la colocación de bloques de luz con nivel " + lightLevel + ".");
                player.sendMessage("§aID de operación: " + operationId);
                plugin.getLogger().info("Colocación completada para operación: " + operationId);

                // Cancelar la tarea utilizando el array mutable
                taskHolder[0].cancel();

                // Limpiar la tarea en el TaskManager
                TaskManager.cancelTask(player.getUniqueId());
            }
        }, 0L, 1L);

        // Registrar la tarea en el TaskManager
        TaskManager.addTask(player.getUniqueId(), taskHolder[0]);
    }

    private void processSingleBlock(Location blockLocation, int lightLevel, String operationId) {
        Block block = blockLocation.getBlock();
        block.setType(Material.LIGHT, false);

        if (block.getType() == Material.LIGHT) {
            try {
                Levelled lightData = (Levelled) block.getBlockData();
                lightData.setLevel(lightLevel);
                block.setBlockData(lightData, false);

                LightRegistry.addBlock(blockLocation, lightLevel, operationId);
            } catch (ClassCastException e) {
                plugin.getLogger().warning("Error al configurar el nivel de luz para el bloque en " + blockLocation + ": " + e.getMessage());
            }
        }
    }
}
