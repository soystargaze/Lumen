package com.erosmari.lumen.listeners;

import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.LumenConstants;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class MobListener implements Listener {

    private final ItemMobsHandler mobsHandler;
    private final LumenItems lumenItems;
    private final NamespacedKey lumenIdKey;

    public MobListener(ItemMobsHandler mobsHandler, LumenItems lumenItems) {
        this.mobsHandler = mobsHandler;
        this.lumenItems = lumenItems;
        this.lumenIdKey = LumenConstants.getLumenIdKey();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getItemMeta() != null) {
            PersistentDataContainer itemContainer = itemInHand.getItemMeta().getPersistentDataContainer();

            if (itemContainer.has(lumenIdKey, PersistentDataType.STRING)) {
                String id = itemContainer.get(lumenIdKey, PersistentDataType.STRING);

                if ("guard".equals(id)) {
                    Block placedBlock = event.getBlock();
                    Player player = event.getPlayer();
                    Location placedLocation = placedBlock.getLocation();

                    if (placedBlock.getState() instanceof TileState tileState) {
                        transferPersistentData(itemContainer, tileState.getPersistentDataContainer());
                        tileState.update();
                    }

                    mobsHandler.registerAntiMobArea(player, placedLocation);

                    ItemEffectUtil.playEffect(placedLocation, "guard");

                    LoggingUtils.sendAndLog(player,"torch.guard_placed", placedLocation);
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        if (brokenBlock.getState() instanceof TileState tileState) {
            PersistentDataContainer blockContainer = tileState.getPersistentDataContainer();

            if (blockContainer.has(lumenIdKey, PersistentDataType.STRING)) {
                String id = blockContainer.get(lumenIdKey, PersistentDataType.STRING);

                if ("guard".equals(id)) {
                    Location brokenLocation = brokenBlock.getLocation();

                    mobsHandler.unregisterAntiMobArea(brokenLocation);

                    ItemStack customItem = lumenItems.getLumenItem(id);

                    if (customItem != null) {
                        brokenBlock.getWorld().dropItemNaturally(brokenBlock.getLocation(), customItem.clone());

                        event.setDropItems(false);

                        LoggingUtils.sendAndLog(player,"torch.guard_broken", brokenLocation);
                    }
                }
            }
        }
    }

    private void transferPersistentData(PersistentDataContainer source, PersistentDataContainer target) {
        for (NamespacedKey key : source.getKeys()) {
            if (source.has(key, PersistentDataType.STRING)) {
                target.set(key, PersistentDataType.STRING, Objects.requireNonNull(source.get(key, PersistentDataType.STRING)));
            }
        }
    }
}