package com.soystargaze.lumen;

import com.soystargaze.lumen.commands.LumenCommandManager;
import com.soystargaze.lumen.config.ConfigHandler;
import com.soystargaze.lumen.database.DatabaseHandler;
import com.soystargaze.lumen.connections.CoreProtectHandler;
import com.soystargaze.lumen.items.LumenItems;
import com.soystargaze.lumen.lights.ItemLightsHandler;
import com.soystargaze.lumen.lights.integrations.ItemFAWEHandler;
import com.soystargaze.lumen.listeners.CraftPermissionListener;
import com.soystargaze.lumen.listeners.MobListener;
import com.soystargaze.lumen.listeners.TorchListener;
import com.soystargaze.lumen.mobs.ItemMobsHandler;
import com.soystargaze.lumen.utils.*;
import com.soystargaze.lumen.utils.text.TextHandler;
import com.soystargaze.lumen.utils.text.legacy.LegacyTranslationHandler;
import com.soystargaze.lumen.utils.updater.UpdateOnFullLoad;
import com.soystargaze.lumen.utils.updater.UpdateOnJoinListener;
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
            TextHandler.get().logTranslated("plugin.enable_error", e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        AsyncExecutor.shutdown();
        DatabaseHandler.close();
        TextHandler.get().logTranslated("plugin.disabled");
        instance = null;
    }

    private void initializePlugin() {
        LumenConstants.init(this);
        loadConfigurations();
        initializeMetrics();

        try {
            initializeDatabase();
            registerEvents();
            registerServerLoadListener();
        } catch (Exception e) {
            TextHandler.get().logTranslated("plugin.enable_error", e);
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
        getConfig().options().copyDefaults(true);
        saveConfig();

        TextHandler.init(this);
        setupTranslations();
        ConsoleUtils.displayAsciiArt(this);

        AsyncExecutor.initialize();
        initializeCommandManager();
        initializeItems();
        ConsoleUtils.displaySuccessMessage(this);
    }

    private void setupTranslations() {
        File translationsFolder = new File(getDataFolder(), "Translations");
        if (!translationsFolder.exists() && !translationsFolder.mkdirs()) {
            TextHandler.get().logTranslated("translations.folder_error");
            return;
        }

        String[] defaults = {
                "en_us.yml","es_es.yml","fr_fr.yml","de_de.yml",
                "pt_br.yml","zh_cn.yml","it_it.yml","es_andaluh.yml"
        };

        boolean replace = getConfig().getBoolean("translations.force-update", true);
        for (String file : defaults) {
            try {
                saveResource("Translations/" + file, replace);
            } catch (Exception e) {
                TextHandler.get().registerTemporaryTranslation(
                        "translations.save_error",
                        "Language cannot be saved: {0}"
                );
                TextHandler.get().logTranslated("translations.save_error", file);
            }
        }

        String lang = ConfigHandler.getLanguage();
        if (TextHandler.get().isLanguageAvailable(lang)) {
            TextHandler.get().loadTranslations(this, lang);
        } else {
            TextHandler.get().registerTemporaryTranslation(
                    "translations.language_not_found",
                    "Language not found: {0}"
            );
            TextHandler.get().logTranslated("translations.language_not_found", lang);
            TextHandler.get().loadTranslations(this, TextHandler.get().getActiveLanguage());
        }
    }

    private void initializeDatabase() {
        try {
            DatabaseHandler.initialize(this);
        } catch (Exception e) {
            TextHandler.get().logTranslated("database.init_error", e.getMessage());
            throw new IllegalStateException(LegacyTranslationHandler.get("database.init_fatal_error"));
        }
    }

    private void initializeCommandManager() {
        try {
            if (commandManager == null) {
                commandManager = new LumenCommandManager(this);
                commandManager.registerCommands();
            }
        } catch (Exception e) {
            TextHandler.get().logTranslated("command.register_error", e.getMessage());
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
            TextHandler.get().logTranslated("items.init_error", e.getMessage());
        }
    }

    private void initializeCoreProtectIntegration() {
        if (!isCoreProtectAvailable()) {
            TextHandler.get().logTranslated("plugin.separator");
            TextHandler.get().logTranslated("coreprotect.unavailable");
            return;
        }

        try {
            coreProtectHandler = new CoreProtectHandler();
            if (isFAWEAvailable()) {
                ItemFAWEHandler.setCoreProtectHandler(coreProtectHandler);
            } else {
                TextHandler.get().logTranslated("plugin.separator");
                TextHandler.get().logTranslated("coreprotect.integration.no_fawe");
            }
            if (coreProtectHandler.isEnabled()) {
                TextHandler.get().logTranslated("plugin.separator");
                TextHandler.get().logTranslated("coreprotect.enabled");
            } else {
                TextHandler.get().logTranslated("plugin.separator");
                TextHandler.get().logTranslated("coreprotect.unavailable");
                coreProtectHandler = null;
            }
        } catch (Exception e) {
            TextHandler.get().logTranslated("coreprotect.error_initializing", e.getMessage());
            coreProtectHandler = null;
        }
        TextHandler.get().logTranslated("plugin.separator");
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
            getServer().getPluginManager().registerEvents(new UpdateOnFullLoad(), this);
            getServer().getPluginManager().registerEvents(new UpdateOnJoinListener(), this);
            getServer().getPluginManager().registerEvents(new TorchListener(this, lightsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new MobListener(mobsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new CraftPermissionListener(), this);
        } catch (Exception e) {
            TextHandler.get().logTranslated("events.register_error", e.getMessage());
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
            TextHandler.get().registerTemporaryTranslation(BSTATS_ERROR, "BStats error: {0}");
            TextHandler.get().logTranslated(BSTATS_ERROR, e.getMessage());
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        initializeCoreProtectIntegration();
    }
}