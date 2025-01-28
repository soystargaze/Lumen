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
import com.google.gson.Gson;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Lumen extends JavaPlugin implements Listener {

    private static Lumen instance;
    private LumenCommandManager commandManager;
    private LumenItems lumenItems;
    private CoreProtectHandler coreProtectHandler;
    private static final Gson gson = new Gson();

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
            return;
        }
        verifyLicense();
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
        startLicenseVerificationTask();
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

    private boolean verifyPurchase() {
        try {
            URI uri = new URI("https://api.polymart.org/v1/verifyPurchase");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            String postData = "license=" + URLEncoder.encode("%%__LICENSE__%%", StandardCharsets.UTF_8) +
                    "&resource_id=" + URLEncoder.encode("%%__RESOURCE__%%", StandardCharsets.UTF_8) +
                    "&user_id=" + URLEncoder.encode("%%__USER__%%", StandardCharsets.UTF_8) +
                    "&signature=" + URLEncoder.encode("%%__SIGNATURE__%%", StandardCharsets.UTF_8);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    Map<?, ?> responseMap = gson.fromJson(reader, Map.class);
                    boolean success = (Boolean) ((Map<?, ?>) responseMap.get("response")).get("success");
                    if (!success) {
                        getLogger().warning("License verification failed: " + responseMap.get("error"));
                    }
                    return success;
                }
            } else {
                final String LOCAL_TEST_MESSAGE_KEY = "plugin.license_failed_with_response";
                TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "License verification failed. Disabling plugin. {0}");
                LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY,responseCode);
            }
        } catch (Exception e) {
            final String LOCAL_TEST_MESSAGE_KEY = "plugin.license_failed_to_verify";
            TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "License verification failed. Disabling plugin. {0}");
            LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY, e.getMessage());
        }
        return false;
    }

    private boolean isLocalTest() {
        return getServer().getIp().equalsIgnoreCase("127.0.0.1") || getServer().getPort() == 25565;
    }

    private void startLicenseVerificationTask() {
        if (isLocalTest()) {
            return;
        }

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!verifyPurchase()) {
                final String LOCAL_TEST_MESSAGE_KEY = "plugin.license_not_verified";
                TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "License verification failed. Disabling plugin.");
                LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
                getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            }
        }, 0L, 72000L);
    }

    private void verifyLicense() {
        if (isLocalTest()) {
            final String LOCAL_TEST_MESSAGE_KEY = "plugin.local_test";
            TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "Skipping license verification (local test mode enabled)");
            LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (verifyPurchase()) {
                final String LOCAL_TEST_MESSAGE_KEY = "plugin.license_verified";
                TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "License verified successfully.");
                LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
            } else {
                final String LOCAL_TEST_MESSAGE_KEY = "plugin.license_not_verified";
                TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "License verification failed. Disabling plugin.");
                LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
                getServer().getScheduler().runTask(this, () -> getServer().getPluginManager().disablePlugin(this));
            }
        });
    }
}