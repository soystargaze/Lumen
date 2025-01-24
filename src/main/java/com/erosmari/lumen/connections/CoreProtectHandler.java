package com.erosmari.lumen.connections;

import com.erosmari.lumen.utils.TranslationHandler;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.logging.Logger;

public class CoreProtectHandler {

    private CoreProtectAPI coreProtectAPI;

    public CoreProtectHandler(Plugin plugin) {
        setupCoreProtect(plugin);
    }

    public void setupCoreProtect(Plugin plugin) {
        CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");

        if (coreProtect != null && coreProtect.isEnabled()) {
            coreProtectAPI = coreProtect.getAPI();
            if (coreProtectAPI != null && coreProtectAPI.isEnabled()) {
                plugin.getLogger().info(TranslationHandler.get("coreprotect.integration.success"));
            } else {
                coreProtectAPI = null;
                plugin.getLogger().warning(TranslationHandler.get("coreprotect.integration.api_disabled"));
            }
        } else {
            plugin.getLogger().warning(TranslationHandler.get("coreprotect.integration.not_found"));
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null && coreProtectAPI.isEnabled();
    }

    public void logLightPlacement(Logger logger, String playerName, List<Location> locations, Material material) {
        if (isEnabled()) {
            int successCount = 0;

            for (Location location : locations) {
                try {
                    coreProtectAPI.logPlacement(playerName, location, material, null);
                    successCount++;
                } catch (Exception e) {
                    logger.warning(TranslationHandler.getFormatted(
                            "coreprotect.placement.error",
                            location,
                            e.getMessage()
                    ));
                }
            }

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
        } else {
            logger.warning(TranslationHandler.get("coreprotect.disabled"));
        }
    }

    public void logRemoval(Logger logger, String playerName, List<Location> locations, Material forcedMaterial) {
        if (isEnabled()) {
            int successCount = 0;

            for (Location location : locations) {
                try {
                    coreProtectAPI.logRemoval(playerName, location, forcedMaterial, location.getBlock().getBlockData());
                    successCount++;
                } catch (Exception e) {
                    logger.warning(TranslationHandler.getFormatted(
                            "coreprotect.removal.error",
                            location,
                            e.getMessage()
                    ));
                }
            }

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
        } else {
            logger.warning(TranslationHandler.get("coreprotect.disabled"));
        }
    }
}