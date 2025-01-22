package com.erosmari.lumen.utils;

import com.erosmari.lumen.connections.CoreProtectCompatibility;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public class RemoveLightUtils {

    /**
     * Elimina un bloque de luz y lo registra en CoreProtect.
     *
     * @param logger                 Logger del plugin.
     * @param player                 Jugador que ejecuta la eliminación.
     * @param blockLocation          Ubicación del bloque a eliminar.
     * @param coreProtectCompatibility Instancia de CoreProtectCompatibility.
     * @return True si el bloque fue eliminado, False en caso contrario.
     */
    public static boolean removeLightBlock(Logger logger, Player player, Location blockLocation, CoreProtectCompatibility coreProtectCompatibility) {
        Block block = blockLocation.getBlock();
        if (block.getType() == Material.LIGHT) {
            // Registrar en CoreProtect
            CoreProtectUtils.logLightRemoval(logger, coreProtectCompatibility, player, blockLocation);

            // Eliminar el bloque
            block.setType(Material.AIR, false);
            return true;
        }
        return false;
    }
}