package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.connections.CoreProtectCompatibility; // Importa la clase de integración
import com.erosmari.lumen.database.DatabaseHandler;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.listeners.MobListener;
import com.erosmari.lumen.listeners.TorchListener;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.ConsoleUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;

public class Lumen extends JavaPlugin {

    private static Lumen instance; // Instancia estática para obtener el plugin fácilmente
    private LumenCommandManager commandManager;
    private LumenItems lumenItems; // Nueva instancia de LumenItems
    private CoreProtectCompatibility coreProtectCompatibility; // Instancia de integración con CoreProtect

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
            initializeCoreProtectIntegration(); // Inicializa CoreProtect

            ConsoleUtils.displaySuccessMessage(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("plugin.enable_error"), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        DatabaseHandler.close();
        getLogger().info(TranslationHandler.get("plugin.disabled"));
        instance = null;
    }

    public static Lumen getInstance() {
        return instance;
    }

    public LumenItems getLumenItems() {
        return lumenItems;
    }

    public CoreProtectCompatibility getCoreProtectCompatibility() {
        return Objects.requireNonNullElseGet(coreProtectCompatibility, () -> new CoreProtectCompatibility(this));
    }

    private void loadConfigurations() {
        ConfigHandler.setup(this);
        setupTranslations();
        TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            getLogger().severe(TranslationHandler.get("translations.folder_error"));
            return;
        }

        File defaultTranslationFile = new File(translationsFolder, "es_es.yml");
        if (!defaultTranslationFile.exists()) {
            try {
                saveResource("Translations/es_es.yml", false);
            } catch (Exception e) {
                getLogger().severe(TranslationHandler.get("translations.file_error") + ": " + e.getMessage());
            }
        }
    }

    private void initializeDatabase() {
        try {
            DatabaseHandler.initialize(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("database.init_error"), e);
            throw new IllegalStateException(TranslationHandler.get("database.init_fatal_error"));
        }
    }

    private void initializeSystems() {
        initializeCommandManager();
        initializeItems();
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

    private void initializeItems() {
        try {
            if (lumenItems == null) {
                lumenItems = new LumenItems(this);
                lumenItems.registerItems();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("items.init_error"), e);
        }
    }

    private void initializeCoreProtectIntegration() {
        try {
            coreProtectCompatibility = new CoreProtectCompatibility(this);
            if (coreProtectCompatibility.isEnabled()) {
                getLogger().info("Integración con CoreProtect habilitada correctamente.");
            } else {
                getLogger().info("CoreProtect no está disponible. La integración será omitida.");
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error al inicializar la integración con CoreProtect.", e);
        }
    }

    private void registerComponents() {
        registerEvents();
    }

    private void registerEvents() {
        try {
            ItemLightsHandler lightsHandler = new ItemLightsHandler(this);
            ItemMobsHandler mobsHandler = new ItemMobsHandler(this);

            getServer().getPluginManager().registerEvents(new TorchListener(this, lightsHandler, lumenItems, coreProtectCompatibility), this);
            getServer().getPluginManager().registerEvents(new MobListener(this, mobsHandler, lumenItems, coreProtectCompatibility), this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("events.register_error"), e);
        }
    }
}