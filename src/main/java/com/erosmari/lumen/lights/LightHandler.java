package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class LightHandler {

    private final Lumen plugin;

    public LightHandler(Lumen plugin) {
        this.plugin = plugin;
    }

    /**
     * Coloca bloques de luz invisible en un área de forma asincrónica por lotes.
     *
     * @param player          El jugador que ejecutó el comando.
     * @param areaBlocks      Tamaño del área en bloques.
     * @param lightLevel      Nivel de luz.
     * @param includeSkylight Si incluye skylight.
     * @param operationId     Identificador único de la operación.
     */
    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight, String operationId) {
        Location center = player.getLocation();
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage("§cError: No se pudo determinar el mundo.");
            return;
        }

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

    /**
     * Determina si un bloque debería ser iluminado.
     */
    private boolean shouldPlaceLight(Location location, boolean includeSkylight) {
        if (!location.getBlock().getType().isAir()) {
            return false;
        }

        return includeSkylight || location.getWorld().getHighestBlockYAt(location) > location.getBlockY();
    }

    /**
     * Procesa los bloques de luz en lotes, respetando el máximo configurado por tick.
     *
     * @param player      El jugador que ejecutó el comando.
     * @param blocks      Lista de ubicaciones de bloques a iluminar.
     * @param lightLevel  Nivel de luz.
     * @param operationId Identificador único de la operación.
     */
    private void processBlocksAsync(Player player, List<Location> blocks, int lightLevel, String operationId) {
        // Configuración del máximo de bloques por tick
        int maxBlocksPerTick = ConfigHandler.getInt("settings.light_per_tick_with_command", 1000);

        // Cola para manejar los bloques pendientes
        Queue<Location> blockQueue = new LinkedList<>(blocks);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
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
                Bukkit.getScheduler().cancelTask(task.getTaskId());
            }
        }, 0L, 1L); // Ejecutar cada tick
    }

    /**
     * Procesa un solo bloque, colocándolo y registrándolo en la base de datos.
     *
     * @param blockLocation Ubicación del bloque.
     * @param lightLevel    Nivel de luz.
     * @param operationId   Identificador único de la operación.
     */
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
