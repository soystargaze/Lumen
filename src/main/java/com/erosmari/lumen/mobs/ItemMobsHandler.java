package com.erosmari.lumen.mobs;

import com.erosmari.lumen.database.MobRegistry;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
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
    }

    public void registerAntiMobArea(Player player, Location location) {
        int radius = plugin.getConfig().getInt("settings.mob_torch_radius", 35);
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
}
