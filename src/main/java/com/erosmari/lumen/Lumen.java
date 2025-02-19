package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import com.erosmari.lumen.config.ConfigHandler;
import com.erosmari.lumen.database.DatabaseHandler;
import com.erosmari.lumen.connections.CoreProtectHandler;
import com.erosmari.lumen.items.LumenItems;
import com.erosmari.lumen.lights.ItemLightsHandler;
import com.erosmari.lumen.lights.integrations.ItemFAWEHandler;
import com.erosmari.lumen.listeners.CraftPermissionListener;
import com.erosmari.lumen.listeners.MobListener;
import com.erosmari.lumen.listeners.TorchListener;
import com.erosmari.lumen.mobs.ItemMobsHandler;
import com.erosmari.lumen.utils.*;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

import java.io.*;

public class Lumen extends JavaPlugin implements Listener {

    private static Lumen instance;
    private LumenCommandManager commandManager;
    private LumenItems lumenItems;
    private CoreProtectHandler coreProtectHandler;
    private static final int BSTATS_PLUGIN_ID = 24638;

    @SuppressWarnings("CallToPrintStackTrace")
    @Override
    public void onEnable() {
        instance = this;
        try {
            initializePlugin();
        } catch (Exception e) {
            LoggingUtils.logTranslated("plugin.enable_error", e.getMessage());
            e.printStackTrace();
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

    @SuppressWarnings("UnstableApiUsage")
    private void initializePlugin() {
        LumenConstants.init(this);
        initializeMetrics();
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
            registerEvents();
            registerServerLoadListener();

            ConsoleUtils.displaySuccessMessage(this);
        } catch (Exception e) {
            LoggingUtils.logTranslated("plugin.enable_error", e);
            getServer().getPluginManager().disablePlugin(this);
        }
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
        AsyncExecutor.initialize();
        setupTranslations();
        TranslationHandler.loadTranslations(this, ConfigHandler.getLanguage());
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            LoggingUtils.logTranslated("translations.folder_error");
            return;
        }

        String[] defaultLanguages = {"en_us.yml", "es_es.yml", "fr_fr.yml", "de_de.yml", "it_it.yml", "pt_br.yml", "es_andaluh.yml"};
        for (String languageFile : defaultLanguages) {
            saveDefaultTranslation(languageFile);
        }

        File[] translationFiles = translationsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (translationFiles != null) {
            for (File file : translationFiles) {
                String language = file.getName().replace(".yml", "");
                TranslationHandler.loadTranslations(this, language);
            }
        }

        String configuredLanguage = ConfigHandler.getLanguage();
        if (TranslationHandler.isLanguageAvailable(configuredLanguage)) {
            TranslationHandler.setActiveLanguage(configuredLanguage);
        } else {
            final String LICENSE_SUCCESS_KEY = "translations.language_not_found";
            TranslationHandler.registerTemporaryTranslation(LICENSE_SUCCESS_KEY, "Language not found: {0}");
            LoggingUtils.logTranslated(LICENSE_SUCCESS_KEY, configuredLanguage);
        }
    }

    private void saveDefaultTranslation(String fileName) {
        File translationFile = new File(getDataFolder(), "Translations/" + fileName);
        if (!translationFile.exists()) {
            try {
                saveResource("Translations/" + fileName, false);
            } catch (Exception ignored) {
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
        if (!isCoreProtectAvailable()) {
            LoggingUtils.logTranslated("plugin.separator");
            LoggingUtils.logTranslated("coreprotect.unavailable");
            return;
        }

        try {
            coreProtectHandler = new CoreProtectHandler();
            if (isFAWEAvailable()) {
                ItemFAWEHandler.setCoreProtectHandler(coreProtectHandler);
            } else {
                LoggingUtils.logTranslated("plugin.separator");
                LoggingUtils.logTranslated("coreprotect.integration.no_fawe");
            }
            if (coreProtectHandler.isEnabled()) {
                LoggingUtils.logTranslated("plugin.separator");
                LoggingUtils.logTranslated("coreprotect.enabled");
            } else {
                LoggingUtils.logTranslated("plugin.separator");
                LoggingUtils.logTranslated("coreprotect.unavailable");
                coreProtectHandler = null;
            }
        } catch (Exception e) {
            LoggingUtils.logTranslated("coreprotect.error_initializing", e.getMessage());
            coreProtectHandler = null;
        }
        LoggingUtils.logTranslated("plugin.separator");
    }

    private boolean isCoreProtectAvailable() {
        if (Bukkit.getPluginManager().getPlugin("CoreProtect") == null) {
            return false;
        }
        try {
            Class.forName("net.coreprotect.CoreProtect");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isFAWEAvailable() {
        if (Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") == null) {
            return false;
        }
        try {
            Class.forName("com.fastasyncworldedit.core.FaweAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private void registerEvents() {
        try {
            ItemLightsHandler lightsHandler = new ItemLightsHandler(this);
            ItemMobsHandler mobsHandler = new ItemMobsHandler(this);
            getServer().getPluginManager().registerEvents(new TorchListener(this, lightsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new MobListener(mobsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new CraftPermissionListener(), this);
        } catch (Exception e) {
            LoggingUtils.logTranslated("events.register_error", e.getMessage());
        }
    }

    private void registerServerLoadListener() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void initializeMetrics() {
        try {
            new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (Exception e) {
            final String BSTATS_ERROR = "bstats.error";
            TranslationHandler.registerTemporaryTranslation(BSTATS_ERROR, "BStats error: {0}");
            LoggingUtils.logTranslated(BSTATS_ERROR, e.getMessage());
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        initializeCoreProtectIntegration();
    }
}