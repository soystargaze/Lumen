package com.erosmari.lumen.connections;

import com.erosmari.lumen.utils.TranslationHandler;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;

public class CoreProtectCompatibility {

    private CoreProtectAPI coreProtectAPI;
    private final org.bukkit.plugin.Plugin plugin;

    public CoreProtectCompatibility(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        setupCoreProtect();
    }

    private void setupCoreProtect() {
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

    public CoreProtectAPI getAPI() {
        return coreProtectAPI;
    }
}