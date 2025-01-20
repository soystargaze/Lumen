package com.erosmari.lumen.listeners;

import com.erosmari.lumen.database.MobRegistry;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getItemMeta() != null) {
            PersistentDataContainer itemContainer = itemInHand.getItemMeta().getPersistentDataContainer();
            NamespacedKey idKey = new NamespacedKey(plugin, "lumen_id"); // Clave única para identificar mob_torch

            if (itemContainer.has(idKey, PersistentDataType.STRING)) {
                String id = itemContainer.get(idKey, PersistentDataType.STRING);

                // Comprueba si el ID es "anti_mob"
                if ("anti_mob".equals(id)) {
                    Block placedBlock = event.getBlock();
                    Location placedLocation = placedBlock.getLocation();
                    Player player = event.getPlayer();

                    // Transfiere el ID al bloque colocado si es compatible con TileState
                    if (placedBlock.getState() instanceof TileState tileState) {
                        PersistentDataContainer blockContainer = tileState.getPersistentDataContainer();
                        blockContainer.set(idKey, PersistentDataType.STRING, id);
                        tileState.update(); // Aplica los cambios al bloque
                    } else {
                        plugin.getLogger().warning("El bloque colocado no soporta TileState.");
                    }

                    // Registra el área protegida
                    itemMobsHandler.registerAntiMobArea(player, placedLocation);

                    // Efecto visual y sonoro
                    ItemEffectUtil.playEffect(placedLocation, "mob_torch");

                    plugin.getLogger().info(TranslationHandler.getFormatted(
                            "torch.mob_torch_placed", placedLocation));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // Verifica si el bloque tiene un valor de ID almacenado en su PersistentDataContainer
        if (brokenBlock.getState() instanceof TileState tileState) {
            PersistentDataContainer container = tileState.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "lumen_id"); // Clave registrada en LumenItems

            if (container.has(key, PersistentDataType.STRING)) {
                String id = container.get(key, PersistentDataType.STRING);

                // Comprueba que el ID sea exactamente "anti_mob"
                if ("anti_mob".equals(id)) {
                    Location brokenLocation = brokenBlock.getLocation();

                    // Elimina el área protegida
                    try {
                        itemMobsHandler.unregisterAntiMobArea(brokenLocation);
                        plugin.getLogger().info(TranslationHandler.getFormatted("torch.mob_torch_broken", brokenLocation));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error al eliminar el área protegida: " + e.getMessage());
                    }
                }
            }
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
        int searchRange = plugin.getConfig().getInt("settings.mob_torch_search_range", 100);

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
