package com.erosmari.lumen.lights.integrations;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.LoggingUtils;
import com.fastasyncworldedit.bukkit.FaweBukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class ItemFAWEHandler {

    private static CoreProtectHandler coreProtectHandler;

    public ItemFAWEHandler() {
        coreProtectHandler = null;
    }

    public static void setCoreProtectHandler(CoreProtectHandler handler) {
        coreProtectHandler = handler;
    }

    /**
     * Verifica si FAWE y WorldEdit están disponibles en el servidor.
     */
    public static boolean isFAWEAvailable() {
        try {
            Class.forName("com.fastasyncworldedit.core.FaweAPI");
            Class.forName("com.sk89q.worldedit.WorldEdit");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Coloca bloques de luz usando FAWE.
     *
     * @param player      Jugador que ejecuta la acción.
     * @param locations   Lista de ubicaciones donde se colocarán los bloques.
     * @param lightLevel  Nivel de luz de los bloques.
     * @param operationId Identificador de la operación.
     */
    public static void placeLightsWithFAWE(Player player, List<Location> locations, int lightLevel, int operationId) {
        if (!isFAWEAvailable()) {
            LoggingUtils.sendAndLog(player, "light.error.fawe_not_found");
            return;
        }

        if (locations == null || locations.isEmpty()) {
            LoggingUtils.sendAndLog(player, "light.error.no_locations");
            return;
        }

        if (lightLevel < 0 || lightLevel > 15) {
            LoggingUtils.sendAndLog(player, "light.error.invalid_light_level", lightLevel);
            return;
        }

        org.bukkit.World bukkitWorld = locations.getFirst().getWorld();
        if (bukkitWorld == null) {
            LoggingUtils.sendAndLog(player, "light.error.null_world");
            return;
        }

        FaweBukkitWorld faweWorld = FaweBukkitWorld.of(bukkitWorld);

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(faweWorld)
                .build()) {

            BlockType lightType = BlockTypes.LIGHT;
            if (lightType == null) {
                LoggingUtils.sendAndLog(player, "light.error.light_type_not_supported");
                return;
            }

            // Create a BlockState with the custom light level
            BlockState lightState = lightType.getDefaultState();
            Property<Integer> levelProperty = lightType.getProperty("level");
            BlockState customLightState = lightState.with(levelProperty, lightLevel);

            // Establecer bloques usando FAWE
            List<Location> placedLocations = FAWEHandler.processLocations(editSession, locations, customLightState);

            // Registrar bloques en la base de datos
            LightRegistry.addBlocksAsync(placedLocations, lightLevel, operationId);

            // Registrar en CoreProtect solo si la integración está habilitada
            if (coreProtectHandler != null && coreProtectHandler.isEnabled()) {
                try {
                    coreProtectHandler.logLightPlacement(player.getName(), placedLocations, Material.LIGHT);
                } catch (Exception ex) {
                    LoggingUtils.logTranslated("coreprotect.placement.error", ex.getMessage());
                }
            } else {
                LoggingUtils.logTranslated("coreprotect.integration.not_found");
            }

            try {
                editSession.flushQueue();
            } catch (Exception e) {
                LoggingUtils.logTranslated("light.error.flush_failed", e.getMessage());
            }

            LoggingUtils.sendAndLog(player, "light.success.fawe", placedLocations.size());
        } catch (Exception e) {
            LoggingUtils.sendAndLog(player, "light.error.fawe_failed", e.getMessage());
        }
    }
}