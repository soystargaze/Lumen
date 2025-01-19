package com.erosmari.lumen.listeners;

import com.erosmari.lumen.lights.ItemLightsHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

            if (displayName.contains("Lumen Torch")) {
                Block placedBlock = event.getBlock();

                // Generar un operationId único basado en la ubicación del bloque
                String operationId = "torch-" + placedBlock.getLocation().hashCode();

                // Registrar la operación y comenzar la colocación de luces
                lightsHandler.placeLights(event.getPlayer(), placedBlock.getLocation(), operationId);

                plugin.getLogger().info("Lumen Torch colocada en: " + placedBlock.getLocation() +
                        " con ID de operación: " + operationId);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer(); // Obtiene al jugador que rompió el bloque

        // Verifica si el bloque roto es una "Lumen Torch"
        if (brokenBlock.getType() == Material.PLAYER_HEAD) {
            String operationId = "torch-" + brokenBlock.getLocation().hashCode();

            // Cancela la tarea y elimina las luces asociadas
            lightsHandler.cancelOperation(player, operationId);
            lightsHandler.removeLights(player, operationId);

            plugin.getLogger().info("Lumen Torch rota. Operación cancelada y luces eliminadas: " + operationId);
        }
    }
}