package com.erosmari.lumen.mobs;

import com.erosmari.lumen.database.MobRegistry;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemMobsHandler implements Listener {

    private final Plugin plugin;
    private final Map<Location, Integer> protectedAreas = new HashMap<>();
    private final Set<Location> recentlyCancelled = new HashSet<>();
    private final NamespacedKey mobTorchKey;

    public ItemMobsHandler(Plugin plugin) {
        this.plugin = plugin;
        this.mobTorchKey = new NamespacedKey(plugin, "mob_torch");
        Bukkit.getPluginManager().registerEvents(this, plugin);

        loadProtectedAreasFromDatabase();
    }

    private void loadProtectedAreasFromDatabase() {
        protectedAreas.putAll(MobRegistry.getProtectedAreas());
        plugin.getLogger().info(TranslationHandler.get("mobs.protected_areas_loaded"));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();

        if (isMobTorch(event.getItemInHand())) {
            Location placedLocation = placedBlock.getLocation();
            Player player = event.getPlayer();

            // Registra el área protegida
            registerAntiMobArea(player, placedLocation);

            // Efecto visual y sonoro
            ItemEffectUtil.playEffect(placedBlock.getLocation(), "mob_torch");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Location brokenLocation = brokenBlock.getLocation();

        if (isMobTorch(brokenBlock)) {
            // Elimina el área protegida
            unregisterAntiMobArea(brokenLocation);
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }

        Location spawnLocation = event.getLocation();
        World world = spawnLocation.getWorld();

        if (world == null) return;

        if (recentlyCancelled.contains(spawnLocation)) {
            return;
        }

        for (Map.Entry<Location, Integer> entry : protectedAreas.entrySet()) {
            Location center = entry.getKey();
            int radius = entry.getValue();

            if (isWithinRadius(center, spawnLocation, radius)) {
                event.setCancelled(true);
                plugin.getLogger().info(TranslationHandler.getFormatted("mobs.spawn_cancelled", spawnLocation));

                recentlyCancelled.add(spawnLocation);

                Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyCancelled.remove(spawnLocation), 20L);

                break;
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

    private boolean isMobTorch(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return "mob".equals(container.get(mobTorchKey, PersistentDataType.STRING));
    }

    private boolean isMobTorch(Block block) {
        if (block.getType() != Material.PLAYER_HEAD || !(block.getState() instanceof Skull skull)) {
            return false;
        }

        PersistentDataContainer container = skull.getPersistentDataContainer();
        return "mob".equals(container.get(mobTorchKey, PersistentDataType.STRING));
    }

    private void registerAntiMobArea(Player player, Location location) {
        int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 15);
        protectedAreas.put(location, radius);
        MobRegistry.addProtectedArea(location, radius);
        plugin.getLogger().info(TranslationHandler.getFormatted("mobs.area_created_by_player", location, player.getName()));
    }

    private void unregisterAntiMobArea(Location location) {
        if (protectedAreas.remove(location) != null) {
            MobRegistry.removeProtectedArea(location);
            plugin.getLogger().info(TranslationHandler.getFormatted("mobs.area_removed", location));
        }
    }
}