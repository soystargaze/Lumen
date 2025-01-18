package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class Lumen extends JavaPlugin {

    private LumenCommandManager commandManager;

    @Override
    public void onEnable() {
        getLogger().info("--------------------------------------------");
        getLogger().info("Lumen Plugin");
        getLogger().info("v1.0");
        getLogger().info("by Eros Marí");
        getLogger().info("--------------------------------------------");

        try {
            loadConfigurations();
            initializeSystems();
            registerComponents();

            getLogger().info("Lumen habilitado correctamente.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error durante la habilitación del plugin.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Lumen deshabilitado correctamente.");
    }

    private void loadConfigurations() {
        saveDefaultConfig(); // Guarda la configuración por defecto si no existe
        setupTranslations();
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
            getLogger().log(Level.SEVERE, "Error al registrar comandos.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void registerComponents() {
        registerEvents();
    }

    private void registerEvents() {
        try {
            // Aquí se registran los listeners específicos del plugin
            // Ejemplo:
            // getServer().getPluginManager().registerEvents(new LightEventListener(), this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al registrar eventos.", e);
        }
    }
}
