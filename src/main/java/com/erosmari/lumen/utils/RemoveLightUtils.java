package com.erosmari.lumen.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class RemoveLightUtils {

    /**
     * Elimina un bloque de luz y lo registra en CoreProtect.
     *
     * @param blockLocation          Ubicación del bloque a eliminar.
     * @return True si el bloque fue eliminado, False en caso contrario.
     */
    public static boolean removeLightBlock(Location blockLocation) {
        Block block = blockLocation.getBlock();
        if (block.getType() == Material.LIGHT) {

            // Eliminar el bloque
            block.setType(Material.AIR, false);
            return true;
        }
        return false;
    }
}