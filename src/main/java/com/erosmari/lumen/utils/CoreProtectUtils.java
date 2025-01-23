package com.erosmari.lumen.utils;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.logging.Logger;

public class CoreProtectUtils {

    /**
     * Registra la colocación de un bloque de luz en CoreProtect.
     *
     * @param logger      Logger del plugin para registrar errores y acciones.
     * @param coreProtect Instancia de CoreProtectCompatibility.
     * @param playerName  Nombre del jugador que colocó el bloque.
     * @param location    Ubicación del bloque colocado.
     */
    public static void logLightPlacement(Logger logger, CoreProtectCompatibility coreProtect, String playerName, Location location) {
        if (coreProtect == null || !coreProtect.isEnabled()) {
            logger.warning("CoreProtect no está habilitado o no es compatible.");
            return;
        }

        if (coreProtect.getAPI() == null) {
            logger.warning("CoreProtect API es nula.");
            return;
        }

        try {
            Material blockMaterial = location.getBlock().getType();
            coreProtect.getAPI().logPlacement(playerName, location, blockMaterial, location.getBlock().getBlockData());
            logger.info(String.format("Registrada la colocación de %s por %s en %s.", blockMaterial, playerName, location));
        } catch (Exception e) {
            logger.warning(String.format("Error al registrar la colocación de luz en %s: %s", location, e.getMessage()));
        }
    }

    /**
     * Registra la eliminación de bloques en CoreProtect, forzando un material específico.
     *
     * @param logger        Logger del plugin para registrar errores y acciones.
     * @param coreProtect   Instancia de CoreProtectCompatibility.
     * @param playerName    Nombre del jugador que eliminó los bloques.
     * @param locations     Lista de ubicaciones de bloques eliminados.
     * @param forcedMaterial Material a registrar para las eliminaciones (forzado).
     */
    public static void logRemoval(Logger logger, CoreProtectCompatibility coreProtect, String playerName, List<Location> locations, Material forcedMaterial) {
        if (coreProtect == null || !coreProtect.isEnabled()) {
            logger.warning("CoreProtect no está habilitado o no es compatible.");
            return;
        }

        if (coreProtect.getAPI() == null) {
            logger.warning("CoreProtect API es nula.");
            return;
        }

        int successCount = 0;
        for (Location location : locations) {
            try {
                // Forzar el registro del material especificado
                coreProtect.getAPI().logRemoval(playerName, location, forcedMaterial, location.getBlock().getBlockData());
                logger.info(String.format("Registrada la eliminación de %s por %s en %s.", forcedMaterial, playerName, location));
                successCount++;
            } catch (Exception e) {
                logger.warning(String.format("Error al registrar la eliminación en %s: %s", location, e.getMessage()));
            }
        }

        logger.info(String.format("Se registraron %d bloques eliminados en CoreProtect.", successCount));
    }
}