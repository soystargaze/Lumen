package com.erosmari.lumen.utils;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.Location;

import java.util.logging.Logger;

public class CoreProtectUtils {

    public static void logLightPlacement(Logger logger, CoreProtectCompatibility coreProtect, String playerName, Location location) {
        if (coreProtect == null || !coreProtect.isEnabled()) {
            logger.warning("CoreProtect is not enabled or not compatible!");
            return;
        }

        if (coreProtect.getAPI() == null) {
            logger.warning("CoreProtect API is null!");
            return;
        }

        // Log para verificar el nombre del jugador
        logger.info("Registrando colocación de luz con el jugador: " + playerName);

        // Registrar el bloque en CoreProtect con el nombre del jugador
        coreProtect.getAPI().logPlacement(playerName, location, location.getBlock().getType(), location.getBlock().getBlockData());
    }

    public static void logLightRemoval(Logger logger, CoreProtectCompatibility coreProtect, String playerName, Location location) {
        // Verifica si CoreProtect está habilitado y compatible
        if (coreProtect == null || !coreProtect.isEnabled()) {
            logger.warning("CoreProtect is not enabled or not compatible!");
            return; // Salir si no es compatible
        }

        // Verifica si la API de CoreProtect es nula
        if (coreProtect.getAPI() == null) {
            logger.warning("CoreProtect API is null!");
            return; // Salir si la API no está disponible
        }

        // Log para verificar la acción
        logger.info("Registrando eliminación de luz con el jugador: " + playerName);

        // Registrar la eliminación del bloque en CoreProtect
        coreProtect.getAPI().logRemoval(playerName, location, location.getBlock().getType(), location.getBlock().getBlockData());
    }
}