package com.erosmari.lumen.lights.integrations;

import com.fastasyncworldedit.bukkit.FaweBukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;

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
    public static void placeLightBlocks(List<Location> locations, int lightLevel) {
        if (locations == null || locations.isEmpty()) {
            throw new IllegalArgumentException("Locations list is empty or null.");
        }
        if (lightLevel < 0 || lightLevel > 15) {
            throw new IllegalArgumentException("Light level must be between 0 and 15.");
        }

        // Adapt the Bukkit world to a FaweBukkitWorld
        org.bukkit.World bukkitWorld = locations.getFirst().getWorld();
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("Bukkit world is null.");
        }
        FaweBukkitWorld faweWorld = FaweBukkitWorld.of(bukkitWorld);

        // Create an EditSession using the new EditSessionBuilder
        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(faweWorld)
                .build()) {

            BlockType lightType = BlockTypes.LIGHT;
            if (lightType == null) {
                throw new IllegalStateException("BlockType LIGHT is not supported.");
            }

            // Create a BlockState with the specified light level
            BlockState lightState = lightType.getDefaultState();
            Property<Integer> levelProperty = lightType.getProperty("level");
            BlockState customLightState = lightState.with(levelProperty, lightLevel);

            // Loop through each location and set the light block
            for (Location loc : locations) {
                if (loc == null) continue; // Skip null locations
                BlockVector3 position = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

                // Use setBlock with the customized light level
                boolean success = editSession.smartSetBlock(position, customLightState);
                if (!success) {
                    throw new IllegalStateException("Failed to place block at position: " + position);
                }
            }

            editSession.flushQueue(); // Ensure changes are applied
        } catch (Exception e) {
            throw new RuntimeException("Error while placing blocks with FAWE: " + e.getMessage(), e);
        }
    }
}