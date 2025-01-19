package com.erosmari.lumen.listeners;

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

    public TorchListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();

        // Verifica si el bloque colocado es una "Lumen Torch"
        if (itemInHand.getItemMeta() != null && itemInHand.getItemMeta().displayName() != null) {
            String displayName = Objects.requireNonNull(itemInHand.getItemMeta().displayName()).toString();

            if (displayName.contains("Lumen Torch")) {
                Block placedBlock = event.getBlock();

                if (displayName.contains("Mob")) {
                    // Acción para "Lumen Torch Mob"
                    plugin.getLogger().info("Lumen Torch Mob colocada en: " + placedBlock.getLocation());
                    // Lógica específica para la Lumen Torch Mob
                    createLightAreaAroundBlock(placedBlock, 5); // Por ejemplo, ilumina un área de radio 5
                } else {
                    // Acción para "Lumen Torch"
                    plugin.getLogger().info("Lumen Torch colocada en: " + placedBlock.getLocation());
                    // Lógica específica para la Lumen Torch normal
                    createLightAreaAroundBlock(placedBlock, 3); // Por ejemplo, ilumina un área de radio 3
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();

        // Verifica si el bloque roto es una "Lumen Torch"
        if (brokenBlock.getType() == Material.PLAYER_HEAD) {
            // Realiza acciones específicas según el tipo de antorcha
            plugin.getLogger().info("Lumen Torch rota en: " + brokenBlock.getLocation());
            // Aquí puedes eliminar la luz generada o cualquier otra acción
            removeLightAreaAroundBlock(brokenBlock);
        }
    }

    private void createLightAreaAroundBlock(Block block, int radius) {
        // Implementa aquí la lógica para iluminar un área alrededor del bloque
        plugin.getLogger().info("Generando luz en un radio de " + radius + " alrededor del bloque.");
        // Puedes usar el LightHandler o cualquier lógica de iluminación que tengas
    }

    private void removeLightAreaAroundBlock(Block block) {
        // Implementa aquí la lógica para eliminar la luz generada alrededor del bloque
        plugin.getLogger().info("Eliminando luz generada alrededor del bloque.");
        // Asegúrate de rastrear las luces creadas para poder eliminarlas correctamente
    }
}
