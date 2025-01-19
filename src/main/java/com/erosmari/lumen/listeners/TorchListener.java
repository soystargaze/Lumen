package com.erosmari.lumen.listeners;

import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
    private final ItemMobsHandler mobsHandler;

    private final NamespacedKey idKey;

    public TorchListener(Plugin plugin, ItemLightsHandler lightsHandler, ItemMobsHandler mobsHandler) {
        this.plugin = plugin;
        this.lightsHandler = lightsHandler;
        this.mobsHandler = mobsHandler;
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
                    // Lógica para la Lumen Torch (luz)
                    String operationId = "torch-" + placedBlock.getLocation().hashCode();
                    lightsHandler.placeLights(event.getPlayer(), placedBlock.getLocation(), operationId);

                    plugin.getLogger().info("Lumen Torch colocada en: " + placedBlock.getLocation() +
                            " con ID de operación: " + operationId);
                } else if ("mob".equals(id)) {
                    // Lógica para la Lumen Torch Mob (anti-mobs)
                    mobsHandler.registerAntiMobArea(event.getPlayer(), placedBlock.getLocation());

                    plugin.getLogger().info("Lumen Torch Mob colocada en: " + placedBlock.getLocation());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        if (brokenBlock.getType() == Material.PLAYER_HEAD) {
            String operationId = "torch-" + brokenBlock.getLocation().hashCode();

            // Verificar si es una Lumen Torch de luz
            if (lightsHandler.isLightTorch(brokenBlock)) {
                lightsHandler.cancelOperation(player, operationId);
                lightsHandler.removeLights(player, operationId);

                plugin.getLogger().info("Lumen Torch rota. Operación cancelada y luces eliminadas: " + operationId);
            }

            // Verificar si es una Lumen Torch Mob
            if (mobsHandler.isMobTorch(brokenBlock)) {
                mobsHandler.unregisterAntiMobArea(brokenBlock.getLocation());
                plugin.getLogger().info("Lumen Torch Mob rota. Área de protección eliminada.");
            }
        }
    }
}
