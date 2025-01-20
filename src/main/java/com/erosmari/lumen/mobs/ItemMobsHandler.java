package com.erosmari.lumen.mobs;

import com.erosmari.lumen.database.MobRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class ItemMobsHandler {

    private final Plugin plugin;
    private final Map<Location, Integer> protectedAreas = new HashMap<>();

    public ItemMobsHandler(Plugin plugin) {
        this.plugin = plugin;
        loadProtectedAreasFromDatabase();
    }

    private void loadProtectedAreasFromDatabase() {
        protectedAreas.putAll(MobRegistry.getProtectedAreas());
        plugin.getLogger().info(TranslationHandler.get("mobs.protected_areas_loaded"));
    }

    public void registerAntiMobArea(Player player, Location location) {
        int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 20);
        protectedAreas.put(location, radius);
        MobRegistry.addProtectedArea(location, radius);
        plugin.getLogger().info(TranslationHandler.getFormatted("mobs.area_created_by_player", location, player.getName()));
    }

    public void unregisterAntiMobArea(Location location) {
        if (protectedAreas.remove(location) != null) {
            MobRegistry.removeProtectedArea(location);
            plugin.getLogger().info(TranslationHandler.getFormatted("mobs.area_removed", location));
        }
    }

    public boolean isMobTorch(ItemStack item, String mobTorchKey) {
        if (item.getType() != Material.PLAYER_HEAD || item.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        return "mob".equals(container.get(new NamespacedKey(plugin, mobTorchKey), PersistentDataType.STRING));
    }

    public boolean isMobTorch(Block block, String mobTorchKey) {
        if (block.getType() != Material.PLAYER_HEAD || !(block.getState() instanceof Skull skull)) {
            return false;
        }

        PersistentDataContainer container = skull.getPersistentDataContainer();
        return "mob".equals(container.get(new NamespacedKey(plugin, mobTorchKey), PersistentDataType.STRING));
    }
}
