package com.erosmari.lumen.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class LumenConstants {

    private static NamespacedKey LUMEN_ID_KEY;

    // Constructor privado para evitar instanciación
    private LumenConstants() {}

    /**
     * Inicializa el NamespacedKey. Debe llamarse una única vez al iniciar el plugin.
     *
     * @param plugin La instancia del plugin.
     */
    public static void init(Plugin plugin) {
        if (LUMEN_ID_KEY == null) {
            LUMEN_ID_KEY = new NamespacedKey(plugin, "lumen_id");
        }
    }

    /**
     * Devuelve el NamespacedKey utilizado para identificar los ítems de Lumen.
     *
     * @return El NamespacedKey para "lumen_id".
     * @throws IllegalStateException si no se ha inicializado previamente mediante init(plugin).
     */
    public static NamespacedKey getLumenIdKey() {
        if (LUMEN_ID_KEY == null) {
            throw new IllegalStateException("LumenConstants no ha sido inicializado. Llama a init(plugin) primero.");
        }
        return LUMEN_ID_KEY;
    }
}
