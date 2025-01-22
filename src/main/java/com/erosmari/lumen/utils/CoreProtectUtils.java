package com.erosmari.lumen.utils;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class CoreProtectUtils {

    /**
     * Registra una colocación de bloque en CoreProtect.
     *
     * @param logger                 Logger para registrar errores.
     * @param coreProtectCompatibility Instancia de CoreProtectCompatibility.
     * @param player                 Jugador que realiza la colocación.
     * @param blockLocation          Ubicación del bloque colocado.
     */
    public static void logLightPlacement(Logger logger, CoreProtectCompatibility coreProtectCompatibility, Player player, Location blockLocation) {
        if (coreProtectCompatibility != null && coreProtectCompatibility.isEnabled()) {
            try {
                coreProtectCompatibility.logLightPlacement(player, blockLocation);
            } catch (Exception e) {
                logger.warning("Failed to log light placement in CoreProtect: " + e.getMessage());
            }
        }
    }

    /**
     * Registra una eliminación de bloque en CoreProtect.
     *
     * @param logger                 Logger para registrar errores.
     * @param coreProtectCompatibility Instancia de CoreProtectCompatibility.
     * @param player                 Jugador que realiza la eliminación.
     * @param blockLocation          Ubicación del bloque eliminado.
     */
    public static void logLightRemoval(Logger logger, CoreProtectCompatibility coreProtectCompatibility, Player player, Location blockLocation) {
        if (coreProtectCompatibility != null && coreProtectCompatibility.isEnabled()) {
            try {
                coreProtectCompatibility.logLightRemoval(player, blockLocation);
            } catch (Exception e) {
                logger.warning("Failed to log light removal in CoreProtect: " + e.getMessage());
            }
        }
    }
}