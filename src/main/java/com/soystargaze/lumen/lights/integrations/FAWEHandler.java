package com.soystargaze.lumen.lights.integrations;

import com.soystargaze.lumen.connections.CoreProtectHandler;
import com.fastasyncworldedit.bukkit.FaweBukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class FAWEHandler {

    public static boolean isFAWEAvailable() {
        try {
            Class.forName("com.fastasyncworldedit.core.FaweAPI");
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    public static void placeLightBlocks(List<Location> locations, int lightLevel, Player player, CoreProtectHandler coreProtectHandler) {
        if (isFAWEAvailable()) {
            TextHandler.get().logTranslated("light.error.fawe_not_found");
            return;
        }

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
                TextHandler.get().logTranslated("light.error.blocktype_not_supported");
                return;
            }

            BlockState lightState = lightType.getDefaultState();
            Property<Integer> levelProperty = lightType.getProperty("level");
            BlockState customLightState = lightState.with(levelProperty, lightLevel);

            List<Location> placedLocations = processLocations(editSession, locations, customLightState);

            if (coreProtectHandler != null) {
                try {
                    coreProtectHandler.logLightPlacement(player.getName(), placedLocations, Material.LIGHT);
                    TextHandler.get().sendAndLog(player, "light.success.fawe", placedLocations.size());
                } catch (Exception ex) {
                    TextHandler.get().logTranslated("coreprotect.placement.error", ex.getMessage());
                }
            } else {
                TextHandler.get().logTranslated("coreprotect.integration.not_found");
            }

            editSession.flushQueue();

        } catch (Exception e) {
            TextHandler.get().logTranslated("light.error.fawe_failed", e.getMessage());
        }
    }

    public static List<Location> processLocations(EditSession editSession, List<Location> locations, BlockState customLightState) {
        List<Location> placedLocations = new ArrayList<>();
        for (Location loc : locations) {
            if (loc == null) continue;

            try {
                BlockVector3 position = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                editSession.smartSetBlock(position, customLightState);
                placedLocations.add(loc);
            } catch (Exception e) {
                TextHandler.get().logTranslated("light.error.fawe_failed_location", loc, e.getMessage());
            }
        }
        return placedLocations;
    }
}