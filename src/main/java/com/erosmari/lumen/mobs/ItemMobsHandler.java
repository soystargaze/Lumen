package com.erosmari.lumen.mobs;

import com.erosmari.lumen.database.MobRegistry;
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
    private final Map<Location, Integer> protectedAreas = new HashMap<>(); // Áreas protegidas en memoria
    private final Set<Location> recentlyCancelled = new HashSet<>(); // Controlar duplicados en eventos recientes
    private final NamespacedKey mobTorchKey; // Clave para identificar la antorcha de mobs

    public ItemMobsHandler(Plugin plugin) {
        this.plugin = plugin;
        this.mobTorchKey = new NamespacedKey(plugin, "mob_torch");
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Cargar áreas protegidas desde la base de datos
        loadProtectedAreasFromDatabase();
    }

    private void loadProtectedAreasFromDatabase() {
        protectedAreas.putAll(MobRegistry.getProtectedAreas());
        plugin.getLogger().info("Áreas protegidas cargadas desde la base de datos.");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();

        if (isMobTorch(event.getItemInHand())) {
            Location placedLocation = placedBlock.getLocation();
            int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 15); // Radio configurable desde el config.yml

            // Registrar el área protegida en memoria y en la base de datos
            protectedAreas.put(placedLocation, radius);
            MobRegistry.addProtectedArea(placedLocation, radius);

            plugin.getLogger().info("Área protegida contra mobs hostiles creada en: " + placedLocation + " con un radio de " + radius);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location brokenLocation = event.getBlock().getLocation();

        if (protectedAreas.containsKey(brokenLocation)) {
            // Eliminar el área protegida de memoria y de la base de datos
            protectedAreas.remove(brokenLocation);
            MobRegistry.removeProtectedArea(brokenLocation);

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

    private boolean isMobTorch(ItemStack item) {
        if (item.getType() != Material.PLAYER_HEAD || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return "mob".equals(container.get(mobTorchKey, PersistentDataType.STRING));
    }

    public boolean isMobTorch(Block block) {
        if (block.getType() != Material.PLAYER_HEAD || !(block.getState() instanceof Skull skull)) {
            return false;
        }

        PersistentDataContainer container = skull.getPersistentDataContainer();
        return "mob".equals(container.get(mobTorchKey, PersistentDataType.STRING));
    }

    public void registerAntiMobArea(Player player, Location location) {
        int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 15); // Radio configurable
        protectedAreas.put(location, radius);
        MobRegistry.addProtectedArea(location, radius);
        plugin.getLogger().info("Área protegida contra mobs hostiles creada en: " + location + " por " + player.getName());
    }

    public void unregisterAntiMobArea(Location location) {
        if (protectedAreas.remove(location) != null) {
            MobRegistry.removeProtectedArea(location);
            plugin.getLogger().info("Área protegida contra mobs hostiles eliminada en: " + location);
        }
    }
}
