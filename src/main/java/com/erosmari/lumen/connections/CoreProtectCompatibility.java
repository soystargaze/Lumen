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

    public void logLightPlacement(Player player, Location location) {
        if (isEnabled() && player != null && location != null) {
            try {
                coreProtectAPI.logPlacement(player.getName(), location, Material.LIGHT, location.getBlock().getBlockData());
                plugin.getLogger().info(String.format("Registrado el lugar de luz por el jugador %s en %s.", player.getName(), location));
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("Error al registrar el lugar de luz en %s: %s", location, e.getMessage()));
            }
        } else {
            plugin.getLogger().warning("No se pudo registrar el lugar de luz. CoreProtect no está habilitado o los parámetros son inválidos.");
        }
    }

    public void logRemoval(Player player, Location location) {
        if (isEnabled() && player != null && location != null) {
            try {
                coreProtectAPI.logRemoval(player.getName(), location, Material.LIGHT, location.getBlock().getBlockData());
                plugin.getLogger().info(String.format("Registrada la eliminación de luz por el jugador %s en %s.", player.getName(), location));
            } catch (Exception e) {
                plugin.getLogger().warning(String.format("Error al registrar la eliminación de luz en %s: %s", location, e.getMessage()));
            }
        } else {
            plugin.getLogger().warning("No se pudo registrar la eliminación de luz. CoreProtect no está habilitado o los parámetros son inválidos.");
        }
    }
}