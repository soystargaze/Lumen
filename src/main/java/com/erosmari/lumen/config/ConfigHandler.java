package com.erosmari.lumen.config;

import com.erosmari.lumen.Lumen;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class ConfigHandler {

    private static FileConfiguration config; // Configuración general (config.yml)
    private static String language; // Idioma seleccionado (es_es, en_us, etc.)

    /**
     * Configura y carga los archivos de configuración.
     *
     * @param plugin El plugin principal.
     */
    public static void setup(JavaPlugin plugin) {
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
    @SuppressWarnings("unused")
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

    public static boolean isBossBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.bossbar.enabled", true);
    }

    public static String getBossBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.bossbar.message", "Progreso: {progress}% completado");
    }

    public static BarColor getBossBarColor() {
        String color = Lumen.getInstance().getConfig().getString("display.bossbar.color", "GREEN");
        try {
            return BarColor.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarColor.GREEN;
        }
    }

    public static BarStyle getBossBarStyle() {
        String style = Lumen.getInstance().getConfig().getString("display.bossbar.style", "SOLID");
        try {
            return BarStyle.valueOf(style.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BarStyle.SOLID;
        }
    }

    public static boolean isActionBarEnabled() {
        return Lumen.getInstance().getConfig().getBoolean("display.actionbar.enabled", true);
    }

    public static String getActionBarMessage() {
        return Lumen.getInstance().getConfig().getString("display.actionbar.message", "Progreso: {progress}% completado");
    }

    public static Map<String, Object> loadYaml(File file) throws Exception {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = new FileInputStream(file)) {
            return yaml.load(inputStream);
        }
    }
}