package com.erosmari.lumen.listeners;

import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.LumenConstants;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;
import java.util.UUID;

public class TorchListener implements Listener {

    private final Plugin plugin;
    private final ItemLightsHandler lightsHandler;
    private final LumenItems lumenItems;
    private final NamespacedKey lumenIdKey;

    public TorchListener(Plugin plugin, ItemLightsHandler lightsHandler, LumenItems lumenItems) {
        this.plugin = plugin;
        this.lightsHandler = lightsHandler;
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

                if ("torch".equals(id)) {
                    Block placedBlock = event.getBlock();
                    Player player = event.getPlayer();
                    Location placedLocation = placedBlock.getLocation();

                    int incrementalId = LightRegistry.registerOperation(UUID.randomUUID(), "Lumen Torch placed at " + placedLocation);

                    NamespacedKey lightLevelKey = new NamespacedKey(plugin, "custom_light_level");
                    int lightLevel = itemContainer.has(lightLevelKey, PersistentDataType.INTEGER)
                            ? Objects.requireNonNullElse(itemContainer.get(lightLevelKey, PersistentDataType.INTEGER), 15)
                            : ConfigHandler.getInt("settings.torch_light_level", 15);

                    if (placedBlock.getState() instanceof TileState tileState) {
                        PersistentDataContainer container = tileState.getPersistentDataContainer();
                        transferPersistentData(itemContainer, container);
                        container.set(new NamespacedKey(plugin, "operation_id"), PersistentDataType.INTEGER, incrementalId); // Guardar el ID
                        tileState.update();
                    }

                    lightsHandler.placeLights(player, placedLocation, incrementalId, lightLevel);

                    ItemEffectUtil.playEffect(placedLocation, "torch");

                    LoggingUtils.sendAndLog(player,"torch.light_placed", placedLocation, incrementalId);
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

                if ("torch".equals(id)) {
                    try {
                        NamespacedKey operationKey = new NamespacedKey(plugin, "operation_id");
                        if (blockContainer.has(operationKey, PersistentDataType.INTEGER)) {
                            Integer incrementalId = blockContainer.get(operationKey, PersistentDataType.INTEGER);
                            if (incrementalId != null) {

                            lightsHandler.cancelOperation(player, incrementalId);
                            lightsHandler.removeLights(player, incrementalId);

                            ItemStack customItem = lumenItems.getLumenItem(id);

                                if (customItem != null) {
                                    brokenBlock.getWorld().dropItemNaturally(brokenBlock.getLocation(), customItem.clone());

                                    event.setDropItems(false);

                                    LoggingUtils.sendAndLog(player,"torch.light_broken", incrementalId);
                                }
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe(String.format(
                                "Error handling Lumen Torch removal for player %s at %s: %s",
                                player.getName(), brokenBlock.getLocation(), e.getMessage()
                        ));
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

    @EventHandler
    public void onPlayerRightClickAir(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand.getType() == Material.PLAYER_HEAD && itemInHand.getItemMeta() != null) {
            PersistentDataContainer container = itemInHand.getItemMeta().getPersistentDataContainer();

            if (container.has(lumenIdKey, PersistentDataType.STRING) &&
                    "torch".equals(container.get(lumenIdKey, PersistentDataType.STRING))) {

                player.sendMessage("§eEscribe un valor de luz entre 0 y 15 en el chat.");

                plugin.getServer().getPluginManager().registerEvents(new Listener() {
                    @EventHandler
                    public void onChat(AsyncChatEvent chatEvent) {
                        if (!chatEvent.getPlayer().equals(player)) return;

                        chatEvent.setCancelled(true);
                        String message = PlainTextComponentSerializer.plainText().serialize(chatEvent.message());

                        try {
                            int lightLevel = Integer.parseInt(message);
                            if (lightLevel < 0 || lightLevel > 15) {
                                player.sendMessage("§cEl valor debe estar entre 0 y 15.");
                                return;
                            }

                            ItemStack currentItem = player.getInventory().getItemInMainHand();
                            if (currentItem.getType() != Material.PLAYER_HEAD || currentItem.getItemMeta() == null) {
                                player.sendMessage("§cNo tienes la antorcha en la mano.");
                                return;
                            }

                            ItemMeta meta = currentItem.getItemMeta();
                            PersistentDataContainer metaContainer = meta.getPersistentDataContainer();
                            metaContainer.set(new NamespacedKey(plugin, "custom_light_level"), PersistentDataType.INTEGER, lightLevel);
                            currentItem.setItemMeta(meta);

                            LoggingUtils.sendAndLog(player, "torch.light_level_set", lightLevel);
                        } catch (NumberFormatException e) {
                            LoggingUtils.sendAndLog(player, "torch.error.invalid_light_level");
                        }

                        AsyncChatEvent.getHandlerList().unregister(this);
                    }
                }, plugin);
            }
        }
    }
}