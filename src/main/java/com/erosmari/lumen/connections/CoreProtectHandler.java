package com.erosmari.lumen.connections;

import com.erosmari.lumen.utils.TranslationHandler;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class CoreProtectHandler {

    private final Plugin plugin;
    private CoreProtectAPI coreProtectAPI;

    public CoreProtectHandler(Plugin plugin) {
        this.plugin = plugin;
        setupCoreProtect();
    }

    public void setupCoreProtect() {
        CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");

        if (coreProtect != null && coreProtect.isEnabled() && coreProtect.getAPI().isEnabled()) {
            coreProtectAPI = coreProtect.getAPI();
            plugin.getLogger().info(TranslationHandler.get("coreprotect.integration.success"));
        } else {
            coreProtectAPI = null;
            plugin.getLogger().warning(TranslationHandler.get("coreprotect.integration.not_found_or_disabled"));
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null && coreProtectAPI.isEnabled();
    }

    public void logLightPlacement(String playerName, List<Location> locations, Material material) {
        if (locations == null || locations.isEmpty()) {
            plugin.getLogger().warning(TranslationHandler.get("coreprotect.no_locations_provided"));
            return;
        }

        int successCount = processLocations(playerName, locations, material, true);

        if (successCount > 0) {
            plugin.getLogger().info(TranslationHandler.getFormatted(
                    "coreprotect.placement.success",
                    successCount,
                    playerName
            ));
        } else {
            plugin.getLogger().warning(TranslationHandler.getFormatted(
                    "coreprotect.placement.none",
                    playerName
            ));
        }
    }

    public void logRemoval(String playerName, List<Location> locations, Material forcedMaterial) {
        if (locations == null || locations.isEmpty()) {
            plugin.getLogger().warning(TranslationHandler.get("coreprotect.no_locations_provided"));
            return;
        }

        int successCount = processLocations(playerName, locations, forcedMaterial, false);

        if (successCount > 0) {
            plugin.getLogger().info(TranslationHandler.getFormatted(
                    "coreprotect.removal.success",
                    successCount,
                    playerName
            ));
        } else {
            plugin.getLogger().warning(TranslationHandler.getFormatted(
                    "coreprotect.removal.none",
                    playerName
            ));
        }
    }

    private int processLocations(String playerName, List<Location> locations, Material material, boolean isPlacement) {
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
                plugin.getLogger().warning(TranslationHandler.getFormatted(
                        isPlacement ? "coreprotect.placement.error" : "coreprotect.removal.error",
                        location,
                        e.getMessage()
                ));
            }
        }

        return successCount;
    }
}