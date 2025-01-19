package com.erosmari.lumen.mobs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ItemMobsHandler implements Listener {

    private final Plugin plugin;
    private final Map<Location, Integer> protectedAreas = new HashMap<>(); // Áreas protegidas
    private final Set<Location> recentlyCancelled = new HashSet<>(); // Controlar duplicados en eventos recientes

    public ItemMobsHandler(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand().getType() == Material.PLAYER_HEAD &&
                event.getItemInHand().getItemMeta() != null &&
                event.getItemInHand().getItemMeta().displayName() != null &&
                Objects.requireNonNull(event.getItemInHand().getItemMeta().displayName()).toString().contains("Lumen Torch Mob")) {

            Location placedLocation = event.getBlock().getLocation();
            int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 10); // Radio configurable desde el config.yml

            // Registrar el área protegida
            protectedAreas.put(placedLocation, radius);

            plugin.getLogger().info("Área protegida contra mobs hostiles creada en: " + placedLocation + " con un radio de " + radius);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location brokenLocation = event.getBlock().getLocation();

        if (protectedAreas.containsKey(brokenLocation)) {
            // Eliminar el área protegida al romper la antorcha
            protectedAreas.remove(brokenLocation);
            plugin.getLogger().info("Área protegida contra mobs hostiles eliminada en: " + brokenLocation);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return; // Solo aplica a mobs hostiles
        }

        Location spawnLocation = event.getLocation();
        World world = spawnLocation.getWorld();

        if (world == null) return;

        // Evitar mensajes duplicados para spawns recientes
        if (recentlyCancelled.contains(spawnLocation)) {
            return;
        }

        // Verificar si la ubicación del spawn está dentro de un área protegida
        for (Map.Entry<Location, Integer> entry : protectedAreas.entrySet()) {
            Location center = entry.getKey();
            int radius = entry.getValue();

            if (isWithinRadius(center, spawnLocation, radius)) {
                // Cancelar el spawn
                event.setCancelled(true);
                plugin.getLogger().info("Spawn de mob hostil cancelado en: " + spawnLocation);

                // Añadir la ubicación a las recientemente canceladas
                recentlyCancelled.add(spawnLocation);

                // Eliminar después de un tiempo configurable (1 segundo por defecto)
                Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyCancelled.remove(spawnLocation), 20L);

                break;
            }
        }
    }

    private boolean isWithinRadius(Location center, Location target, int radius) {
        if (!center.getWorld().equals(target.getWorld())) {
            return false; // Diferentes mundos
        }

        double distanceSquared = center.distanceSquared(target);
        return distanceSquared <= (radius * radius);
    }
}
