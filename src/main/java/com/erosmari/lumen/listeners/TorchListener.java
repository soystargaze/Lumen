package com.erosmari.lumen.listeners;

import com.erosmari.lumen.lights.ItemLightsHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

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

        // Verifica si el bloque colocado es una "Lumen Torch"
        if (itemInHand.getItemMeta() != null && itemInHand.getItemMeta().displayName() != null) {
            String displayName = Objects.requireNonNull(itemInHand.getItemMeta().displayName()).toString();

            if (displayName.contains("Lumen Torch") && !displayName.contains("Mob")) {
                Block placedBlock = event.getBlock();

                plugin.getLogger().info("Lumen Torch colocada en: " + placedBlock.getLocation());
                // Llama a la lógica de iluminación para la Lumen Torch
                String operationId = "torch-" + placedBlock.getLocation().hashCode();
                lightsHandler.placeLights(event.getPlayer(), placedBlock.getLocation(), operationId);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // Verifica si el bloque roto es una "Lumen Torch"
        if (brokenBlock.getType() == Material.PLAYER_HEAD) {
            // Usa la lógica de eliminación de luces del ItemLightsHandler
            String operationId = "torch-" + brokenBlock.getLocation().hashCode();
            plugin.getLogger().info("Lumen Torch rota en: " + brokenBlock.getLocation());
            lightsHandler.removeLights(event.getPlayer(), operationId);
        }
    }
}
