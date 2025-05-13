package com.soystargaze.lumen.connections;

import com.soystargaze.lumen.utils.text.TextHandler;
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
            TextHandler.get().logTranslated("plugin.separator");
            TextHandler.get().logTranslated("coreprotect.integration.not_found_or_disabled");
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
            TextHandler.get().logTranslated("plugin.separator");
            TextHandler.get().logTranslated("coreprotect.integration.success");
        } else {
            coreProtectAPI = null;
            TextHandler.get().logTranslated("plugin.separator");
            TextHandler.get().logTranslated("coreprotect.integration.not_found_or_disabled");
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null && coreProtectAPI.isEnabled();
    }

    public void logLightPlacement(String playerName, List<Location> locations, Material material) {
        if (!isEnabled() || locations == null || locations.isEmpty()) {
            TextHandler.get().logTranslated("coreprotect.no_locations_provided");
            return;
        }

        int successCount = processLocations(playerName, locations, material, true);

        if (successCount > 0) {
            TextHandler.get().logTranslated("coreprotect.placement.success", successCount, playerName);
        } else {
            TextHandler.get().logTranslated("coreprotect.placement.none", playerName);
        }
    }

    public void logRemoval(String playerName, List<Location> locations, Material forcedMaterial) {
        if (!isEnabled() || locations == null || locations.isEmpty()) {
            TextHandler.get().logTranslated("coreprotect.no_locations_provided");
            return;
        }

        int successCount = processLocations(playerName, locations, forcedMaterial, false);

        if (successCount > 0) {
            TextHandler.get().logTranslated("coreprotect.removal.success", successCount, playerName);
        } else {
            TextHandler.get().logTranslated("coreprotect.removal.none", playerName);
        }
    }

    private int processLocations(String playerName, List<Location> locations, Material material, boolean isPlacement) {
        if (!isEnabled()) return 0;

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
                TextHandler.get().logTranslated(isPlacement ? "coreprotect.placement.error" : "coreprotect.removal.error", location, e.getMessage());
            }
        }
        return successCount;
    }
}