package com.erosmari.lumen.connections;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CoreProtectCompatibility {

    private CoreProtectAPI coreProtectAPI;
    private final org.bukkit.plugin.Plugin plugin;

    public CoreProtectCompatibility(org.bukkit.plugin.Plugin plugin) {
        this.plugin = plugin;
        setupCoreProtect();
    }

    private void setupCoreProtect() {
        // Obtén el plugin CoreProtect
        CoreProtect coreProtect = (CoreProtect) Bukkit.getPluginManager().getPlugin("CoreProtect");

        if (coreProtect != null && coreProtect.isEnabled()) {
            coreProtectAPI = coreProtect.getAPI();
            if (coreProtectAPI.isEnabled()) {
                plugin.getLogger().info("CoreProtect detectado e integrado.");
            } else {
                coreProtectAPI = null;
                plugin.getLogger().warning("CoreProtect está deshabilitado. No se puede usar la integración.");
            }
        } else {
            plugin.getLogger().warning("CoreProtect no encontrado. La integración no estará disponible.");
        }
    }

    public boolean isEnabled() {
        return coreProtectAPI != null;
    }

    public void logLightPlacement(Player player, Location location) {
        if (isEnabled()) {
            coreProtectAPI.logPlacement(player.getName(), location, Material.LIGHT, location.getBlock().getBlockData());
        }
    }

    public void logLightRemoval(Player player, Location location) {
        if (isEnabled()) {
            coreProtectAPI.logRemoval(player.getName(), location, Material.LIGHT, location.getBlock().getBlockData());
        }
    }
}