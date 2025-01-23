package com.erosmari.lumen.utils;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;
import java.util.logging.Logger;

public class CoreProtectUtils {

    /**
     * Registra la colocación de bloques de luz en CoreProtect y genera un log final.
     *
     * @param logger      Logger del plugin para registrar errores y acciones.
     * @param coreProtect Instancia de CoreProtectCompatibility.
     * @param playerName  Nombre del jugador que colocó los bloques.
     * @param locations   Lista de ubicaciones de bloques colocados.
     */
    public static void logLightPlacement(Logger logger, CoreProtectCompatibility coreProtect, String playerName, List<Location> locations) {
        // Validar CoreProtect antes de continuar
        if (validateCoreProtect(logger, coreProtect)) {
            int successCount = 0;

            // Procesar cada ubicación de bloque
            for (Location location : locations) {
                try {
                    Material blockMaterial = location.getBlock().getType();
                    coreProtect.getAPI().logPlacement(playerName, location, blockMaterial, location.getBlock().getBlockData());
                    successCount++;
                } catch (Exception e) {
                    logger.warning(TranslationHandler.getFormatted(
                            "coreprotect.placement.error",
                            location,
                            e.getMessage()
                    ));
                }
            }

            // Log final con el total de bloques procesados
            if (successCount > 0) {
                logger.info(TranslationHandler.getFormatted(
                        "coreprotect.placement.success",
                        successCount,
                        playerName
                ));
            } else {
                logger.warning(TranslationHandler.getFormatted(
                        "coreprotect.placement.none",
                        playerName
                ));
            }
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
        // Validar CoreProtect antes de continuar
        if (validateCoreProtect(logger, coreProtect)) {
            int successCount = 0;

            // Procesar cada ubicación de bloque
            for (Location location : locations) {
                try {
                    coreProtect.getAPI().logRemoval(playerName, location, forcedMaterial, location.getBlock().getBlockData());
                    successCount++;
                } catch (Exception e) {
                    logger.warning(TranslationHandler.getFormatted(
                            "coreprotect.removal.error",
                            location,
                            e.getMessage()
                    ));
                }
            }

            // Log final con el total de bloques procesados
            if (successCount > 0) {
                logger.info(TranslationHandler.getFormatted(
                        "coreprotect.removal.success",
                        successCount,
                        playerName
                ));
            } else {
                logger.warning(TranslationHandler.getFormatted(
                        "coreprotect.removal.none",
                        playerName
                ));
            }
        }
    }

    /**
     * Valida si CoreProtect está habilitado y disponible antes de procesar las acciones.
     *
     * @param logger      Logger del plugin para registrar errores y advertencias.
     * @param coreProtect Instancia de CoreProtectCompatibility.
     * @return `true` si CoreProtect está habilitado y disponible, de lo contrario `false`.
     */
    private static boolean validateCoreProtect(Logger logger, CoreProtectCompatibility coreProtect) {
        if (coreProtect == null || !coreProtect.isEnabled()) {
            logger.warning(TranslationHandler.get("coreprotect.disabled"));
            return false;
        }

        if (coreProtect.getAPI() == null) {
            logger.warning(TranslationHandler.get("coreprotect.api_null"));
            return false;
        }

        return true;
    }
}