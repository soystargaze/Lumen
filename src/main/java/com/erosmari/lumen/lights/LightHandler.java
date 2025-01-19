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
            List<Location> blocksToLight = calculateLightPositions(center, areaBlocks, lightLevel, includeSkylight);

            // Procesar los bloques en lotes
            processBlocksAsync(player, blocksToLight, lightLevel, operationId);
        });
    }

    /**
     * Calcula las posiciones óptimas para colocar bloques de luz, teniendo en cuenta la propagación y bloques sólidos.
     */
    private List<Location> calculateLightPositions(Location center, int areaBlocks, int lightLevel, boolean includeSkylight) {
        List<Location> positions = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            plugin.getLogger().warning("El mundo del jugador no pudo ser determinado.");
            return positions;
        }

        // Máxima distancia que puede cubrir el nivel de luz
        int maxDistance = lightLevel;

        for (int x = -areaBlocks; x <= areaBlocks; x++) {
            for (int y = -areaBlocks; y <= areaBlocks; y++) {
                for (int z = -areaBlocks; z <= areaBlocks; z++) {
                    Location location = center.clone().add(x, y, z);

                    // Verificar si es un bloque válido para colocar luz
                    if (isValidLightPosition(location, center, maxDistance, includeSkylight)) {
                        positions.add(location);
                    }
                }
            }
        }

        plugin.getLogger().info("Se calcularon " + positions.size() + " bloques para iluminar.");
        return positions;
    }

    /**
     * Determina si un bloque es válido para colocar luz, basado en la propagación de luz y su vecindad.
     */
    private boolean isValidLightPosition(Location location, Location center, int maxDistance, boolean includeSkylight) {
        World world = location.getWorld();
        if (world == null) return false;

        Block block = location.getBlock();

        // Asegurarse de que el bloque actual sea aire
        if (!block.getType().isAir()) {
            plugin.getLogger().info("Bloque no es aire en " + location + " (tipo: " + block.getType() + ")");
            return false;
        }

        // Verificar distancia máxima desde el centro
        double taxicabDistance = Math.abs(location.getBlockX() - center.getBlockX())
                + Math.abs(location.getBlockY() - center.getBlockY())
                + Math.abs(location.getBlockZ() - center.getBlockZ());
        if (taxicabDistance > maxDistance) {
            plugin.getLogger().info("Bloque fuera de rango en " + location + " (distancia: " + taxicabDistance + ")");
            return false;
        }

        // Verificar si hay un bloque sólido adyacente
        if (!isAdjacentToSolidBlock(location)) {
            plugin.getLogger().info("No hay bloques sólidos adyacentes en " + location);
            return false;
        }

        // Si se requiere skylight, verificar que el bloque esté bajo luz natural
        if (includeSkylight) {
            int highestY = world.getHighestBlockYAt(location);
            if (location.getBlockY() < highestY) {
                plugin.getLogger().info("Bloque no está bajo luz natural en " + location);
                return false;
            }
        }

        // Si pasa todas las verificaciones, es válido
        return true;
    }

    /**
     * Verifica si un bloque de aire está en contacto con al menos un bloque sólido.
     */
    private boolean isAdjacentToSolidBlock(Location location) {
        World world = location.getWorld();
        if (world == null) return false;

        // Coordenadas relativas para bloques adyacentes
        int[][] offsets = {
                {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
        };

        for (int[] offset : offsets) {
            Location adjacent = location.clone().add(offset[0], offset[1], offset[2]);
            Block adjacentBlock = adjacent.getBlock();

            // Verificar si el bloque adyacente no es aire (es sólido)
            if (!adjacentBlock.getType().isAir()) {
                return true;
            }
        }
        return false;
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
