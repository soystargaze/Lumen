package com.erosmari.lumen.listeners;

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

public class TorchListener implements Listener {

    private final Plugin plugin;
    private final ItemLightsHandler lightsHandler;

    public TorchListener(Plugin plugin, ItemLightsHandler lightsHandler) {
        this.plugin = plugin;
        this.lightsHandler = lightsHandler;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getItemMeta() != null) {
            PersistentDataContainer itemContainer = itemInHand.getItemMeta().getPersistentDataContainer();
            NamespacedKey idKey = new NamespacedKey(plugin, "lumen_id"); // Clave única para identificar Lumen Torch

            if (itemContainer.has(idKey, PersistentDataType.STRING)) {
                String id = itemContainer.get(idKey, PersistentDataType.STRING);

                if ("light".equals(id)) {
                    Block placedBlock = event.getBlock();
                    Player player = event.getPlayer();
                    Location placedLocation = placedBlock.getLocation();

                    // Transfiere el ID al bloque colocado si es compatible con TileState
                    if (placedBlock.getState() instanceof TileState tileState) {
                        PersistentDataContainer blockContainer = tileState.getPersistentDataContainer();
                        blockContainer.set(idKey, PersistentDataType.STRING, id);
                        tileState.update(); // Aplica los cambios al bloque
                    }

                    // Lógica para el Lumen Torch (luz)
                    String operationId = "torch-" + placedLocation.hashCode();
                    lightsHandler.placeLights(player, placedLocation, operationId);

                    // Efecto visual y sonoro
                    ItemEffectUtil.playEffect(placedLocation, "torch");

                    plugin.getLogger().info(TranslationHandler.getFormatted(
                            "torch.light_placed", placedLocation, operationId));
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        // Verifica si el bloque tiene un valor de ID almacenado en su PersistentDataContainer
        if (brokenBlock.getState() instanceof TileState tileState) {
            PersistentDataContainer container = tileState.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "lumen_id"); // Clave registrada en LumenItems

            if (container.has(key, PersistentDataType.STRING)) {
                String id = container.get(key, PersistentDataType.STRING);

                // Comprueba que el ID sea exactamente "light"
                if ("light".equals(id)) {
                    try {
                        // Lógica para el Lumen Torch (luz)
                        String operationId = "torch-" + brokenBlock.getLocation().hashCode();

                        lightsHandler.cancelOperation(player, operationId);
                        lightsHandler.removeLights(player, operationId);

                        plugin.getLogger().info(TranslationHandler.getFormatted("torch.light_broken", operationId));
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error handling Lumen Torch removal: " + e.getMessage());
                    }
                }
            }
        }
    }
}
