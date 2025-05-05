package com.soystargaze.lumen.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class LumenConstants {

    private static NamespacedKey lumenIdKey;

    private LumenConstants() {}

    public static void init(Plugin plugin) {
        if (lumenIdKey == null) {
            lumenIdKey = new NamespacedKey(plugin, "lumen_id");
        }
    }

    public static NamespacedKey getLumenIdKey() {
        if (lumenIdKey == null) {
            throw new IllegalStateException("LumenConstants has not been initialized. Call init(plugin) first.");
        }
        return lumenIdKey;
    }
}
