package com.erosmari.lumen.connections;

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
        // Intenta obtener el plugin CoreProtect
        CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");

        if (coreProtect != null && coreProtect.isEnabled()) {
            coreProtectAPI = coreProtect.getAPI();
            if (coreProtectAPI != null && coreProtectAPI.isEnabled()) {
                plugin.getLogger().info("CoreProtect detectado e integrado correctamente.");
            } else {
                coreProtectAPI = null;
                plugin.getLogger().warning("La API de CoreProtect está deshabilitada. La integración no estará disponible.");
            }
        } else {
            plugin.getLogger().warning("CoreProtect no encontrado o está deshabilitado. La integración no estará disponible.");
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null && coreProtectAPI.isEnabled();
    }

    public CoreProtectAPI getAPI() {
        return coreProtectAPI;
    }
}