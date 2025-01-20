package com.erosmari.lumen.listeners;

import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.utils.ItemEffectUtil;
import com.erosmari.lumen.utils.TranslationHandler;
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

    private final NamespacedKey idKey;

    public TorchListener(Plugin plugin, ItemLightsHandler lightsHandler) {
        this.plugin = plugin;
        this.lightsHandler = lightsHandler;
        this.idKey = new NamespacedKey(plugin, "lumen_id");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();

        if (itemInHand.getItemMeta() != null) {
            PersistentDataContainer container = itemInHand.getItemMeta().getPersistentDataContainer();
            if (container.has(idKey, PersistentDataType.STRING)) {
                String id = container.get(idKey, PersistentDataType.STRING);

                Block placedBlock = event.getBlock();
                if ("light".equals(id)) {
                    // Lógica para el Lumen Torch (luz)
                    String operationId = "torch-" + placedBlock.getLocation().hashCode();
                    lightsHandler.placeLights(event.getPlayer(), placedBlock.getLocation(), operationId);

                    // Efecto visual y sonoro
                    ItemEffectUtil.playEffect(placedBlock.getLocation(), "torch");

                    plugin.getLogger().info(TranslationHandler.getFormatted(
                            "torch.light_placed", placedBlock.getLocation(), operationId));
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
            String id = container.get(key, PersistentDataType.STRING);

            // Comprueba que el ID sea exactamente "light"
            if ("light".equals(id)) {
                // Lógica para el Lumen Torch (luz)
                String operationId = "torch-" + brokenBlock.getLocation().hashCode();

                lightsHandler.cancelOperation(player, operationId);
                lightsHandler.removeLights(player, operationId);

                plugin.getLogger().info(TranslationHandler.getFormatted("torch.light_broken", operationId));
            }
        }
    }
}
