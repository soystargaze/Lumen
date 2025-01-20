package com.erosmari.lumen.listeners;

import com.erosmari.lumen.database.MobRegistry;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MobListener implements Listener {

    private final Plugin plugin;
    private final ItemMobsHandler itemMobsHandler;
    private final Set<Location> recentlyCancelled = new HashSet<>();

    public MobListener(Plugin plugin, ItemMobsHandler itemMobsHandler) {
        this.plugin = plugin;
        this.itemMobsHandler = itemMobsHandler;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (itemMobsHandler.isMobTorch(event.getItemInHand(), "mob_torch")) {
            Location placedLocation = event.getBlock().getLocation();
            Player player = event.getPlayer();

            // Registra el área protegida
            itemMobsHandler.registerAntiMobArea(player, placedLocation);

            // Efecto visual y sonoro
            ItemEffectUtil.playEffect(placedLocation, "mob_torch");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (itemMobsHandler.isMobTorch(event.getBlock(), "mob_torch")) {
            // Elimina el área protegida
            itemMobsHandler.unregisterAntiMobArea(event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return; // Ignora mobs que no sean hostiles
        }

        Location spawnLocation = event.getLocation();

        if (recentlyCancelled.contains(spawnLocation)) {
            return; // Salta si ya se procesó esta ubicación
        }

        // Define el rango de búsqueda desde la configuración
        int searchRange = plugin.getConfig().getInt("settings.mob_torch_search_range", 50);

        // Obtén solo las áreas cercanas desde MobRegistry
        Map<Location, Integer> nearbyAreas = MobRegistry.getNearbyProtectedAreas(spawnLocation, searchRange);

        for (Map.Entry<Location, Integer> entry : nearbyAreas.entrySet()) {
            Location center = entry.getKey();
            int radius = entry.getValue();

            if (isWithinRadius(center, spawnLocation, radius)) {
                event.setCancelled(true);
                plugin.getLogger().info(TranslationHandler.getFormatted("mobs.spawn_cancelled", spawnLocation));

                recentlyCancelled.add(spawnLocation);
                Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyCancelled.remove(spawnLocation), 20L);
                return;
            }
        }
    }

    private boolean isWithinRadius(Location center, Location target, int radius) {
        if (!center.getWorld().equals(target.getWorld())) {
            return false;
        }

        double distanceSquared = center.distanceSquared(target);
        return distanceSquared <= (radius * radius);
    }
}
