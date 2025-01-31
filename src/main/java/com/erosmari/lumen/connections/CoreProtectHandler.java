package com.erosmari.lumen.connections;

import com.erosmari.lumen.utils.LoggingUtils;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.List;

public class CoreProtectHandler {

    private CoreProtectAPI coreProtectAPI;

    public CoreProtectHandler() {
        if (isCoreProtectAvailable()) {
            setupCoreProtect();
        } else {
            coreProtectAPI = null;
            LoggingUtils.logTranslated("plugin.separator");
            LoggingUtils.logTranslated("coreprotect.integration.not_found_or_disabled");
        }
    }

    private boolean isCoreProtectAvailable() {
        try {
            Class.forName("net.coreprotect.CoreProtect");
            return Bukkit.getPluginManager().getPlugin("CoreProtect") != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public void setupCoreProtect() {
        CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");

        if (coreProtect != null && coreProtect.isEnabled() && coreProtect.getAPI().isEnabled()) {
            coreProtectAPI = coreProtect.getAPI();
            LoggingUtils.logTranslated("plugin.separator");
            LoggingUtils.logTranslated("coreprotect.integration.success");
        } else {
            coreProtectAPI = null;
            LoggingUtils.logTranslated("plugin.separator");
            LoggingUtils.logTranslated("coreprotect.integration.not_found_or_disabled");
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null && coreProtectAPI.isEnabled();
    }

    public void logLightPlacement(String playerName, List<Location> locations, Material material) {
        if (!isEnabled() || locations == null || locations.isEmpty()) {
            LoggingUtils.logTranslated("coreprotect.no_locations_provided");
            return;
        }

        int successCount = processLocations(playerName, locations, material, true);

        if (successCount > 0) {
            LoggingUtils.logTranslated("coreprotect.placement.success", successCount, playerName);
        } else {
            LoggingUtils.logTranslated("coreprotect.placement.none", playerName);
        }
    }

    public void logRemoval(String playerName, List<Location> locations, Material forcedMaterial) {
        if (!isEnabled() || locations == null || locations.isEmpty()) {
            LoggingUtils.logTranslated("coreprotect.no_locations_provided");
            return;
        }

        int successCount = processLocations(playerName, locations, forcedMaterial, false);

        if (successCount > 0) {
            LoggingUtils.logTranslated("coreprotect.removal.success", successCount, playerName);
        } else {
            LoggingUtils.logTranslated("coreprotect.removal.none", playerName);
        }
    }

    private int processLocations(String playerName, List<Location> locations, Material material, boolean isPlacement) {
        if (!isEnabled()) return 0; // Evita llamadas si CoreProtect no está disponible

        int successCount = 0;

        for (Location location : locations) {
            try {
                if (isPlacement) {
                    coreProtectAPI.logPlacement(playerName, location, material, null);
                } else {
                    coreProtectAPI.logRemoval(playerName, location, material, location.getBlock().getBlockData());
                }
                successCount++;
            } catch (Exception e) {
                LoggingUtils.logTranslated(isPlacement ? "coreprotect.placement.error" : "coreprotect.removal.error", location, e.getMessage());
            }
        }
        return successCount;
    }
}