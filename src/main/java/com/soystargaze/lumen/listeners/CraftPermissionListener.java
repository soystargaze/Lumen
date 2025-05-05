package com.soystargaze.lumen.listeners;

import com.soystargaze.lumen.utils.LoggingUtils;
import com.soystargaze.lumen.utils.LumenConstants;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class CraftPermissionListener implements Listener {

    public CraftPermissionListener() {
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;

        if (event.getRecipe() == null) return;

        ItemStack result = event.getRecipe().getResult();
        if (result.getType().isAir() || !result.hasItemMeta()) return;

        ItemMeta meta = result.getItemMeta();
        assert meta != null;
        PersistentDataContainer container = meta.getPersistentDataContainer();
        NamespacedKey key = LumenConstants.getLumenIdKey();

        if (container.has(key, PersistentDataType.STRING)) {
            String itemId = container.get(key, PersistentDataType.STRING);

            if ("torch".equals(itemId) && !player.hasPermission("lumen.craft.torch")) {
                event.getInventory().setResult(null);
                LoggingUtils.sendMessage(player,"items.torch.no-permission");
            } else if ("guard".equals(itemId) && !player.hasPermission("lumen.craft.guard")) {
                event.getInventory().setResult(null);
                LoggingUtils.sendMessage(player,"items.guard.no-permission");
            }
        }
    }
}