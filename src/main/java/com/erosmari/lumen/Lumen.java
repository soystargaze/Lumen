package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.DatabaseHandler;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.listeners.MobListener;
import com.erosmari.lumen.listeners.TorchListener;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.AsyncExecutor;
import com.erosmari.lumen.utils.ConsoleUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class Lumen extends JavaPlugin implements Listener {

    private static Lumen instance;
    private LumenCommandManager commandManager;
    private LumenItems lumenItems;
    private CoreProtectHandler coreProtectHandler;

    @Override
    public void onEnable() {
        instance = this;

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
            registerServerLoadListener();

            ConsoleUtils.displaySuccessMessage(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, TranslationHandler.get("plugin.enable_error"), e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        AsyncExecutor.shutdown(); // Apagar el ExecutorService centralizado
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

    public CoreProtectHandler getCoreProtectHandler() {
        return coreProtectHandler;
    }

    private void loadConfigurations() {
        CompletableFuture.runAsync(() -> {
            ConfigHandler.setup(this);
            setupTranslations();
            TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
        }, AsyncExecutor.getExecutor()).exceptionally(ex -> null);
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
        if (coreProtectHandler == null) {
            coreProtectHandler = new CoreProtectHandler(this);

            if (coreProtectHandler.isEnabled()) {
                getLogger().info(TranslationHandler.get("coreprotect.enabled"));
            } else {
                getLogger().warning(TranslationHandler.get("coreprotect.unavailable"));
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
            getLogger().log(Level.SEVERE, TranslationHandler.get("events.register_error"), e);
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