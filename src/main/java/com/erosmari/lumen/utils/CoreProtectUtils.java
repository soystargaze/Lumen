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
}