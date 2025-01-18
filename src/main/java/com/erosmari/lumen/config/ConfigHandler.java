package com.erosmari.lumen.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class ConfigHandler {

    private static FileConfiguration config; // Configuración general (config.yml)
    private static String language; // Idioma seleccionado (es_es, en_us, etc.)

    /**
     * Configura y carga los archivos de configuración.
     *
     * @param plugin El plugin principal.
     */
    public static void setup(JavaPlugin plugin) {
        // Configuración general (config.yml)
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Cargar el idioma configurado
        language = config.getString("language", "es_es"); // Idioma predeterminado: es_es
    }

    /**
     * Recarga la configuración desde el archivo.
     *
     * @param plugin El plugin principal.
     */
    public static void reload(JavaPlugin plugin) {
        plugin.reloadConfig();
        config = plugin.getConfig();
        language = config.getString("language", "es_es"); // Actualiza el idioma configurado
    }

    /**
     * Retorna la configuración general (config.yml).
     *
     * @return Configuración general.
     */
    public static FileConfiguration getConfig() {
        return config;
    }

    /**
     * Retorna el idioma configurado en config.yml.
     *
     * @return Idioma configurado.
     */
    public static String getLanguage() {
        return language;
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
}