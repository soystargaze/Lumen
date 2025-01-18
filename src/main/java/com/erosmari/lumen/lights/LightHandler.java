package com.erosmari.lumen.lights;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class LightHandler {

    /**
     * Coloca bloques de luz invisible en un área de forma asincrónica.
     *
     * @param player          El jugador que ejecutó el comando.
     * @param areaBlocks      Tamaño del área en bloques.
     * @param lightLevel      Nivel de luz.
     * @param includeSkylight Si incluye skylight.
     */
    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight, String operationId) {
        Location center = player.getLocation();
        World world = center.getWorld();

        if (world == null) {
            player.sendMessage("§cError: No se pudo determinar el mundo.");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Lumen.getInstance(), () -> {
            List<Location> blocksToLight = new ArrayList<>();

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

            processBlocks(player, blocksToLight, lightLevel, operationId);
        });
    }

    private boolean shouldPlaceLight(Location location, boolean includeSkylight) {
        if (!location.getBlock().getType().isAir()) {
            return false;
        }

        return includeSkylight || location.getWorld().getHighestBlockYAt(location) > location.getBlockY();
    }

    private void processBlocks(Player player, List<Location> blocks, int lightLevel, String operationId) {
        Bukkit.getScheduler().runTask(Lumen.getInstance(), () -> {
            int placedCount = 0;

            for (Location location : blocks) {
                if (location.getBlock().getType() != Material.LIGHT) {
                    location.getBlock().setType(Material.LIGHT);
                }

                org.bukkit.block.data.Levelled lightData = (org.bukkit.block.data.Levelled) location.getBlock().getBlockData();
                lightData.setLevel(lightLevel);
                location.getBlock().setBlockData(lightData, true);

                LightRegistry.addBlock(location, lightLevel, operationId);
                placedCount++;
            }

            player.sendMessage("§aSe colocaron " + placedCount + " bloques de luz con nivel " + lightLevel + ".");
            player.sendMessage("§aID de operación: " + operationId);
        });
    }
}
