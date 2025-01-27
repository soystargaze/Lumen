package com.erosmari.lumen.lights.integrations;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.utils.TranslationHandler;
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

    public ItemFAWEHandler(Lumen plugin) {
        coreProtectHandler = new CoreProtectHandler(plugin);
    }

    public static void setCoreProtectHandler(CoreProtectHandler handler) {
        coreProtectHandler = handler;
    }

    /**
     * Coloca bloques de luz usando FAWE.
     *
     * @param plugin      Instancia del plugin Lumen.
     * @param player      Jugador que ejecuta la acción.
     * @param locations   Lista de ubicaciones donde se colocarán los bloques.
     * @param lightLevel  Nivel de luz de los bloques.
     * @param operationId Identificador de la operación.
     */
    public static void placeLightsWithFAWE(Lumen plugin, Player player, List<Location> locations, int lightLevel, int operationId) {
        if (locations == null || locations.isEmpty()) {
            player.sendMessage(TranslationHandler.get("light.error.no_locations"));
            return;
        }

        if (lightLevel < 0 || lightLevel > 15) {
            player.sendMessage(TranslationHandler.getFormatted("light.error.invalid_light_level", lightLevel));
            return;
        }

        org.bukkit.World bukkitWorld = locations.getFirst().getWorld();
        if (bukkitWorld == null) {
            player.sendMessage(TranslationHandler.get("light.error.null_world"));
            return;
        }

        FaweBukkitWorld faweWorld = FaweBukkitWorld.of(bukkitWorld);

        try (EditSession editSession = WorldEdit.getInstance()
                .newEditSessionBuilder()
                .world(faweWorld)
                .build()) {

            BlockType lightType = BlockTypes.LIGHT;
            if (lightType == null) {
                player.sendMessage(TranslationHandler.get("light.error.light_type_not_supported"));
                return;
            }

            // Crear un BlockState con el nivel de luz personalizado
            BlockState lightState = lightType.getDefaultState();
            Property<Integer> levelProperty = lightType.getProperty("level");
            if (levelProperty != null) {
                lightState = lightState.with(levelProperty, lightLevel);
            }

            // Establecer bloques usando FAWE
            List<Location> placedLocations = FAWEHandler.processLocations(editSession, locations, lightState);

            // Registrar bloques en la base de datos
            LightRegistry.addBlocksAsync(placedLocations, lightLevel, operationId);

            // Registrar todas las ubicaciones colocadas en CoreProtect
            if (plugin.getCoreProtectHandler() != null) {
                coreProtectHandler.logLightPlacement(
                        player.getName(),
                        placedLocations,
                        Material.LIGHT
                );
                plugin.getLogger().info("Logged " + placedLocations.size() + " light blocks in CoreProtect.");
            } else {
                plugin.getLogger().warning(TranslationHandler.get("coreprotect.integration.not_found"));
            }

            // Finalizar operaciones
            editSession.flushQueue();

            // Notificar al jugador
            player.sendMessage(TranslationHandler.getFormatted("light.success.fawe", placedLocations.size()));
            plugin.getLogger().info("FAWE placed " + placedLocations.size() + " light blocks for operation: " + operationId);
        } catch (Exception e) {
            player.sendMessage(TranslationHandler.getFormatted("light.error", e.getMessage()));
            plugin.getLogger().severe("Error using FAWE for light placement: " + e.getMessage());
        }
    }
}