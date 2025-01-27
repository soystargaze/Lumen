package com.erosmari.lumen.listeners;

import com.erosmari.lumen.database.LightRegistry;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.TranslationHandler;
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
        this.lumenIdKey = new NamespacedKey(plugin, "lumen_id");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getItemMeta() != null) {
            PersistentDataContainer itemContainer = itemInHand.getItemMeta().getPersistentDataContainer();

            if (itemContainer.has(lumenIdKey, PersistentDataType.STRING)) {
                String id = itemContainer.get(lumenIdKey, PersistentDataType.STRING);

                if ("light".equals(id)) {
                    Block placedBlock = event.getBlock();
                    Player player = event.getPlayer();
                    Location placedLocation = placedBlock.getLocation();

                    // Generar un ID incremental para la operación
                    int incrementalId = LightRegistry.registerOperation(UUID.randomUUID(), "Lumen Torch placed at " + placedLocation);

                    // Transfiere el ID al bloque colocado si es compatible con TileState
                    if (placedBlock.getState() instanceof TileState tileState) {
                        PersistentDataContainer container = tileState.getPersistentDataContainer();
                        transferPersistentData(itemContainer, container);
                        container.set(new NamespacedKey(plugin, "operation_id"), PersistentDataType.INTEGER, incrementalId); // Guardar el ID
                        tileState.update();
                    }

                    // Lógica para el Lumen Torch (luz)
                    lightsHandler.placeLights(player, placedLocation, incrementalId);

                    // Efecto visual y sonoro
                    ItemEffectUtil.playEffect(placedLocation, "torch");

                    player.sendMessage(TranslationHandler.getPlayerMessage("torch.light_placed", placedLocation, incrementalId));
                    plugin.getLogger().info(TranslationHandler.getFormatted("torch.light_placed", placedLocation, incrementalId));
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

                if ("light".equals(id)) {
                    try {
                        // Recuperar el ID incremental del bloque
                        NamespacedKey operationKey = new NamespacedKey(plugin, "operation_id");
                        if (blockContainer.has(operationKey, PersistentDataType.INTEGER)) {
                            Integer incrementalId = blockContainer.get(operationKey, PersistentDataType.INTEGER);
                            if (incrementalId != null) {

                            // Cancelar operaciones asociadas con este ID
                            lightsHandler.cancelOperation(player, incrementalId);
                            lightsHandler.removeLights(player, incrementalId);

                            // Obtener el ítem original desde la instancia de lumenItems
                            ItemStack customItem = lumenItems.getLumenItem(id);

                                if (customItem != null) {
                                    // Soltar el ítem clonado
                                    brokenBlock.getWorld().dropItemNaturally(brokenBlock.getLocation(), customItem.clone());

                                    // Evitar el drop predeterminado
                                    event.setDropItems(false);

                                    player.sendMessage(TranslationHandler.getPlayerMessage("torch.light_broken", incrementalId));
                                    plugin.getLogger().info(TranslationHandler.getFormatted("torch.light_broken", incrementalId));

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
}