package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.DatabaseHandler;
import com.erosmari.lumen.utils.ConsoleUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class Lumen extends JavaPlugin {

    private static Lumen instance; // Instancia estática para obtener el plugin fácilmente
    private LumenCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this; // Inicializa la instancia estática

        ConsoleUtils.displayAsciiArt(this);
        getLogger().info("--------------------------------------------");
        getLogger().info("Lumen Plugin");
        getLogger().info("v1.0");
        getLogger().info("by Eros Marí");
        getLogger().info("--------------------------------------------");

        try {
            loadConfigurations();
            initializeDatabase();
            initializeSystems();
            registerComponents();

            getLogger().info("Lumen habilitado correctamente.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("plugin.enable_error"), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        DatabaseHandler.close(); // Cierra el pool de conexiones de la base de datos
        getLogger().info(TranslationHandler.get("plugin.disabled"));
        instance = null; // Limpia la instancia estática al desactivar el plugin
    }

    /**
     * Devuelve la instancia estática del plugin.
     *
     * @return Instancia del plugin.
     */
    public static Lumen getInstance() {
        return instance;
    }

    private void loadConfigurations() {
        ConfigHandler.setup(this);
        setupTranslations();
        TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().severe("Error: No se pudo crear la carpeta de traducciones.");
            return;
        }

        File defaultTranslationFile = new File(translationsFolder, "es_es.yml");
        if (!defaultTranslationFile.exists()) {
            try {
                saveResource("Translations/es_es.yml", false);
            } catch (Exception e) {
                getLogger().severe("Error: No se pudo crear el archivo de traducción: " + e.getMessage());
            }
        }
    }

    private void initializeDatabase() {
        try {
            DatabaseHandler.initialize(this); // Delega la inicialización al DatabaseHandler
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("database.init_error"), e);
            throw new IllegalStateException("Falló la inicialización de la base de datos.");
        }
    }

    private void initializeSystems() {
        initializeCommandManager();
    }

    private void initializeCommandManager() {
        try {
            if (commandManager == null) {
                commandManager = new LumenCommandManager(this);
                commandManager.registerCommands();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("command.register_error"), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerComponents() {
        registerEvents();
    }

    private void registerEvents() {
        try {
            // Registrar eventos relevantes aquí si son necesarios
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("events.register_error"), e);
        }
    }
}
