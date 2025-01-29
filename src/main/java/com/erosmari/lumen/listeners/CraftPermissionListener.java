package com.erosmari.lumen.listeners;

import com.erosmari.lumen.Lumen;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class CraftPermissionListener implements Listener {

    private final Lumen plugin;

    public CraftPermissionListener(Lumen plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        ItemStack result = Objects.requireNonNull(event.getRecipe()).getResult();
        if (!result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, "lumen_id");

        if (container.has(key, PersistentDataType.STRING)) {
            String itemId = container.get(key, PersistentDataType.STRING);

            if ("torch".equals(itemId) && !player.hasPermission("lumen.craft.torch")) {
                event.getInventory().setResult(null);
                TranslationHandler.getPlayerMessage("items.torch.no-permission");
            } else if ("guard".equals(itemId) && !player.hasPermission("lumen.craft.guard")) {
                event.getInventory().setResult(null);
                TranslationHandler.getPlayerMessage("items.guard.no-permission");
            }
        }
    }
}