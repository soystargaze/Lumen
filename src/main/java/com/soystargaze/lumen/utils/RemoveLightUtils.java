package com.soystargaze.lumen.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class RemoveLightUtils {

    public static boolean removeLightBlock(Location blockLocation) {
        Block block = blockLocation.getBlock();
        if (block.getType() == Material.LIGHT) {

            block.setType(Material.AIR, false);
            return true;
        }
        return false;
    }
}