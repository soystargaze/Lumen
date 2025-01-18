package com.erosmari.lumen.lights;

import com.erosmari.lumen.database.LightRegistry;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LightHandler {

    /**
     * Coloca bloques de luz invisible en un área.
     *
     * @param player          El jugador que ejecutó el comando.
     * @param areaBlocks      Tamaño del área en bloques.
     * @param lightLevel      Nivel de luz.
     * @param includeSkylight Si incluye skylight (actualmente no se usa).
     */
    public void placeLights(Player player, int areaBlocks, int lightLevel, boolean includeSkylight) {
        Location center = player.getLocation();
        World world = center.getWorld(); // Obtiene el mundo del jugador

        for (int x = -areaBlocks; x <= areaBlocks; x++) {
            for (int y = -areaBlocks; y <= areaBlocks; y++) {
                for (int z = -areaBlocks; z <= areaBlocks; z++) {
                    Location blockLocation = center.clone().add(x, y, z);

                    if (shouldPlaceLight(blockLocation, includeSkylight)) {
                        placeLightBlock(world, blockLocation, lightLevel); // Pasa el parámetro 'world'
                    }
                }
            }
        }

        player.sendMessage("Se han colocado bloques de luz invisible con nivel " + lightLevel + " en el área.");
    }

    /**
     * Verifica si un bloque puede recibir luz.
     *
     * @param location La ubicación del bloque.
     * @return Verdadero si se puede colocar luz.
     */
    private boolean shouldPlaceLight(Location location, boolean includeSkylight) {
        if (!location.getBlock().getType().isAir()) {
            return false; // Solo se permite colocar luz en bloques de aire
        }

        // Si no se permite skylight, verifica si el bloque está expuesto al cielo
        return includeSkylight || location.getWorld().getHighestBlockYAt(location) > location.getBlockY(); // Bloque iluminado naturalmente por el cielo
        // Bloque elegible para colocación de luz
    }

    /**
     * Coloca un bloque de luz invisible y lo registra.
     *
     * @param location   Ubicación del bloque.
     * @param lightLevel Nivel de luz.
     */
    private void placeLightBlock(World world, Location location, int lightLevel) {
        // Verifica que el bloque esté en el mundo correcto
        if (!location.getWorld().equals(world)) {
            return; // Salir si no es el mundo esperado
        }

        // Verifica y coloca un bloque de luz en el mundo
        if (location.getBlock().getType() != org.bukkit.Material.LIGHT) {
            location.getBlock().setType(org.bukkit.Material.LIGHT);
        }

        // Ajusta el nivel de luz
        org.bukkit.block.data.Levelled lightData = (org.bukkit.block.data.Levelled) location.getBlock().getBlockData();
        lightData.setLevel(lightLevel);
        location.getBlock().setBlockData(lightData, true);

        // Registra el bloque en la base de datos
        LightRegistry.addBlock(location, lightLevel);
    }
}
