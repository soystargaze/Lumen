package com.erosmari.lumen.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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
     * Obtiene un valor entero de la configuración.
     *
     * @param path La ruta de la clave en el archivo de configuración.
     * @param def  El valor predeterminado si no existe la clave.
     * @return El valor entero obtenido de la configuración.
     */
    public static int getInt(String path, int def) {
        return config.getInt(path, def);
    }
}