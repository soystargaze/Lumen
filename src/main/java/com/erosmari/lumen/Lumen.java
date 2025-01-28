package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.DatabaseHandler;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.lights.integrations.ItemFAWEHandler;
import com.erosmari.lumen.listeners.MobListener;
import com.erosmari.lumen.listeners.TorchListener;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.AsyncExecutor;
import com.erosmari.lumen.utils.ConsoleUtils;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Lumen extends JavaPlugin implements Listener {

    private static Lumen instance;
    private LumenCommandManager commandManager;
    private LumenItems lumenItems;
    private CoreProtectHandler coreProtectHandler;

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void onEnable() {
        instance = this;
        loadConfigurations();

        ConsoleUtils.displayAsciiArt(this);
        LoggingUtils.logTranslated("plugin.separator");
        LoggingUtils.logTranslated("plugin.name");
        LoggingUtils.logTranslated("plugin.version", getPluginMeta().getVersion());
        LoggingUtils.logTranslated("plugin.author", getPluginMeta().getAuthors().getFirst());
        LoggingUtils.logTranslated("plugin.separator");

        try {
            initializeDatabase();
            initializeSystems();
            registerComponents();
            registerServerLoadListener();

            ConsoleUtils.displaySuccessMessage(this);
        } catch (Exception e) {
            LoggingUtils.logTranslated(("plugin.enable_error"), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        AsyncExecutor.shutdown();
        DatabaseHandler.close();
        LoggingUtils.logTranslated("plugin.disabled");
        instance = null;
    }

    public static Lumen getInstance() {
        return instance;
    }

    public LumenItems getLumenItems() {
        return lumenItems;
    }

    public CoreProtectHandler getCoreProtectHandler() {
        return coreProtectHandler;
    }

    private void loadConfigurations() {
        ConfigHandler.setup(this);
        setupTranslations();
        TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            LoggingUtils.logTranslated("translations.folder_error");
            return;
        }

        // Carga solo el idioma seleccionado en la configuración
        String language = ConfigHandler.getLanguage();
        saveDefaultTranslation(language + ".yml");
    }

    private void saveDefaultTranslation(String fileName) {
        File translationFile = new File(getDataFolder(), "Translations/" + fileName);
        if (!translationFile.exists()) {
            try {
                saveResource("Translations/" + fileName, false);
            } catch (Exception e) {
                getLogger().severe(TranslationHandler.get("translations.file_error") + ": " + e.getMessage());
            }
        }
    }

    private void initializeDatabase() {
        try {
            DatabaseHandler.initialize(this);
        } catch (Exception e) {
            LoggingUtils.logTranslated("database.init_error", e.getMessage());
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
            LoggingUtils.logTranslated("command.register_error", e.getMessage());
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
            LoggingUtils.logTranslated("items.init_error", e.getMessage());
        }
    }

    private void initializeCoreProtectIntegration() {
        if (coreProtectHandler == null) {
            coreProtectHandler = new CoreProtectHandler();
            ItemFAWEHandler.setCoreProtectHandler(coreProtectHandler);

            if (coreProtectHandler.isEnabled()) {
                LoggingUtils.logTranslated("coreprotect.enabled");
                LoggingUtils.logTranslated("plugin.separator");
            } else {
                LoggingUtils.logTranslated("coreprotect.unavailable");
                LoggingUtils.logTranslated("plugin.separator");
                coreProtectHandler = null;
            }
        }
    }

    private void registerComponents() {
        registerEvents();
    }

    private void registerEvents() {
        try {
            ItemLightsHandler lightsHandler = new ItemLightsHandler(this);
            ItemMobsHandler mobsHandler = new ItemMobsHandler(this);

            getServer().getPluginManager().registerEvents(new TorchListener(this, lightsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new MobListener(this, mobsHandler, lumenItems), this);
        } catch (Exception e) {
            LoggingUtils.logTranslated("events.register_error", e.getMessage());
        }
    }

    private void registerServerLoadListener() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        initializeCoreProtectIntegration();
    }
}