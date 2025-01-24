package com.erosmari.lumen.lights.integrations;

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
import org.bukkit.entity.Player;

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
    public static void placeLightBlocks(List<Location> locations, int lightLevel, Player player) {
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

            // Establecer bloques
            for (Location loc : locations) {
                if (loc == null) continue;
                BlockVector3 position = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                editSession.smartSetBlock(position, customLightState);
            }

            // Finalizar operaciones
            editSession.flushQueue();

            // Enviar el mensaje al jugador
            player.sendMessage(TranslationHandler.get("light.success.completed"));
        } catch (Exception e) {
            player.sendMessage(TranslationHandler.getFormatted("light.error", e.getMessage()));
        }
    }
}