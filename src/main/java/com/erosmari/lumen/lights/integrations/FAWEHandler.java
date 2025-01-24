package com.erosmari.lumen.lights.integrations;

import com.erosmari.lumen.utils.CoreProtectUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.fastasyncworldedit.bukkit.FaweBukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class FAWEHandler {

    /**
     * Verifies if FAWE is available.
     *
     * @return true if FAWE is available, false otherwise.
     */
    public static boolean isFAWEAvailable() {
        try {
            Class.forName("com.fastasyncworldedit.core.FaweAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Places light blocks using FAWE.
     *
     * @param locations List of locations where blocks should be placed.
     */
    public static void placeLightBlocks(List<Location> locations, int lightLevel, Player player, JavaPlugin plugin, Object coreProtectCompatibility) {
        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException("Locations list is empty or null.");
        }
        if (lightLevel < 0 || lightLevel > 15) {
            throw new IllegalArgumentException("Light level must be between 0 and 15.");
        }

        org.bukkit.World bukkitWorld = locations.getFirst().getWorld();
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("Bukkit world is null.");
        }

        FaweBukkitWorld faweWorld = FaweBukkitWorld.of(bukkitWorld);

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(faweWorld)
                .build()) {

            BlockType lightType = BlockTypes.LIGHT;
            if (lightType == null) {
                throw new IllegalStateException("BlockType LIGHT is not supported.");
            }

            // Crear un BlockState con el nivel de luz personalizado
            BlockState lightState = lightType.getDefaultState();
            Property<Integer> levelProperty = lightType.getProperty("level");
            BlockState customLightState = lightState.with(levelProperty, lightLevel);

            // Usar el nuevo methods para procesar las ubicaciones
            List<Location> placedLocations = processLocations(editSession, locations, customLightState);

            // Registrar todas las ubicaciones colocadas en CoreProtect
            if (coreProtectCompatibility instanceof com.erosmari.lumen.connections.CoreProtectCompatibility compatibility) {
                try {
                    CoreProtectUtils.logLightPlacement(plugin.getLogger(), compatibility, player.getName(), placedLocations, Material.LIGHT);
                    plugin.getLogger().info("Logged " + placedLocations.size() + " light blocks in CoreProtect.");
                } catch (Exception ex) {
                    plugin.getLogger().severe("Error while logging light placement in CoreProtect: " + ex.getMessage());
                }
            } else {
                plugin.getLogger().warning("CoreProtectCompatibility is not available or invalid.");
                plugin.getLogger().warning(TranslationHandler.get("coreprotect.integration.not_found"));
            }

            // Finalizar operaciones
            editSession.flushQueue();

            // Enviar el mensaje al jugador
            player.sendMessage(TranslationHandler.get("light.success.completed"));
        } catch (Exception e) {
            player.sendMessage(TranslationHandler.getFormatted("light.error", e.getMessage()));
        }
    }
    /**
     * Procesa una lista de ubicaciones y coloca bloques usando FAWE.
     *
     * @param editSession    La sesión de edición de WorldEdit.
     * @param locations      Lista de ubicaciones donde se colocarán los bloques.
     * @param customLightState Estado personalizado del bloque a colocar.
     * @return Lista de ubicaciones donde se colocaron bloques.
     */
    public static List<Location> processLocations(EditSession editSession, List<Location> locations, BlockState customLightState) {
        List<Location> placedLocations = new ArrayList<>();
        for (Location loc : locations) {
            if (loc == null) continue;
            BlockVector3 position = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            editSession.smartSetBlock(position, customLightState);
            placedLocations.add(loc);
        }
        return placedLocations;
    }
}