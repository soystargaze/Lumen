package com.erosmari.lumen.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigHandler {

    private static FileConfiguration config;

    /**
     * Configuración inicial del manejador.
     *
     * @param plugin El plugin principal.
     */
    public static void setup(JavaPlugin plugin) {
        plugin.saveDefaultConfig(); // Guarda la configuración predeterminada si no existe
        config = plugin.getConfig(); // Carga la configuración
    }

    /**
     * Recarga la configuración desde el archivo.
     *
     * @param plugin El plugin principal.
     */
    public static void reload(JavaPlugin plugin) {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    /**
     * Obtiene el rango máximo permitido para comandos.
     *
     * @return Rango máximo configurado.
     */
    public static int getMaxLightRange() {
        return config.getInt("settings.max_light_range", 50); // Valor predeterminado: 50
    }

    /**
     * Obtiene el nivel de luz predeterminado para bloques invisibles.
     *
     * @return Nivel de luz configurado.
     */
    public static int getDefaultInvisibleLightLevel() {
        return config.getInt("settings.default_invisible_light_level", 15); // Valor predeterminado: 15
    }

    /**
     * Obtiene si las luces invisibles están habilitadas.
     *
     * @return Verdadero si están habilitadas, falso de lo contrario.
     */
    public static boolean isInvisibleLightEnabled() {
        return config.getBoolean("settings.invisible_light_enabled", true); // Valor predeterminado: true
    }

    /**
     * Obtiene el idioma configurado.
     *
     * @return Idioma configurado (por ejemplo, "es_es").
     */
    public static String getLanguage() {
        return config.getString("language", "es_es"); // Valor predeterminado: es_es
    }
}
