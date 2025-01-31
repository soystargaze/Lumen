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
import com.erosmari.lumen.utils.AsyncExecutor;
import com.erosmari.lumen.utils.ConsoleUtils;
import com.erosmari.lumen.utils.LoggingUtils;
import com.erosmari.lumen.utils.TranslationHandler;
import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
            getServer().getPluginManager().registerEvents(new MobListener(this, mobsHandler, lumenItems), this);
            getServer().getPluginManager().registerEvents(new CraftPermissionListener(this), this);
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
                    if (success) {
                        final String LICENSE_SUCCESS_KEY = "plugin.license_success";
                        TranslationHandler.registerTemporaryTranslation(LICENSE_SUCCESS_KEY, "License verified successfully.");
                        LoggingUtils.logTranslated(LICENSE_SUCCESS_KEY);
                        validateResponse(responseMap);
                    } else {
                        final String LICENSE_ERROR_KEY = "plugin.license_error";
                        TranslationHandler.registerTemporaryTranslation(LICENSE_ERROR_KEY, "License verification failed. Reason: {0}");
                        LoggingUtils.logTranslated(LICENSE_ERROR_KEY, responseMap.get("error"));
                    }
                    return success;
                }
            } else {
                final String LICENSE_RESPONSE_CODE_KEY = "plugin.license_response_code_error";
                TranslationHandler.registerTemporaryTranslation(LICENSE_RESPONSE_CODE_KEY, "License verification failed. Response code: {0}");
                LoggingUtils.logTranslated(LICENSE_RESPONSE_CODE_KEY, responseCode);
            }
        } catch (Exception e) {
            final String LICENSE_EXCEPTION_KEY = "plugin.license_exception";
            TranslationHandler.registerTemporaryTranslation(LICENSE_EXCEPTION_KEY, "License verification failed due to an exception: {0}");
            LoggingUtils.logTranslated(LICENSE_EXCEPTION_KEY, e.getMessage());
        }
        return false;
    }
    private void validateResponse(Map<?, ?> responseMap) {
        Map<?, ?> response = (Map<?, ?>) responseMap.get("response");
        String returnedLicense = (String) response.get("license");
        String returnedResourceId = (String) response.get("resource_id");
        String returnedUserId = (String) response.get("user_id");
        if (!"%%__LICENSE__%%".equals(returnedLicense) ||
                !"%%__RESOURCE__%%".equals(returnedResourceId) ||
                !"%%__USER__%%".equals(returnedUserId)) {
            throw new IllegalStateException("License verification response does not match expected values.");
        }
    }
    private void startLicenseVerificationTask() {
        if (isLocalTest()) {
            return;
        }
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (!verifyPurchase()) {
                handleLicenseFailure();
            }
        }, 0L, 72000L); // Reintento cada 72000 ticks (~1 hora)
    }
    private void handleLicenseFailure() {
        final String LICENSE_FAILED_KEY = "plugin.license_not_verified";
        TranslationHandler.registerTemporaryTranslation(LICENSE_FAILED_KEY, "License verification failed. Disabling plugin.");
        LoggingUtils.logTranslated(LICENSE_FAILED_KEY);
        getServer().getScheduler().runTask(this, () -> {
            AsyncExecutor.shutdown();
            getServer().getPluginManager().disablePlugin(this);
        });
    }
    private void verifyLicense() {
        if (isLocalTest()) {
            return;
        }
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (verifyPurchase()) {
                final String LICENSE_VERIFIED_KEY = "plugin.license_verified";
                TranslationHandler.registerTemporaryTranslation(LICENSE_VERIFIED_KEY, "License verified successfully.");
                LoggingUtils.logTranslated(LICENSE_VERIFIED_KEY);
            } else {
                handleLicenseFailure();
            }
        });
    }

    private boolean isLocalTest() {
        File localTestFile = new File(getDataFolder(), "dev.yml");
        if (!localTestFile.exists()) {
            return false;
        }
        try {
            Map<String, Object> config = ConfigHandler.loadYaml(localTestFile);
            if (!config.containsKey("dev_test") || !config.containsKey("key")) {
                return false;
            }
            Object isLocalEnabledObj = config.get("dev_test");
            if (!(isLocalEnabledObj instanceof Boolean)) {
                return false;
            }
            boolean isLocalEnabled = (boolean) isLocalEnabledObj;
            String providedKey = (String) config.get("key");
            if (providedKey == null || providedKey.isEmpty()) {
                return false;
            }
            String serverKey = generateServerKey();
            if (serverKey == null) {
                return false;
            }
            if (isLocalEnabled && serverKey.equals(providedKey)) {
                final String LOCAL_TEST_MESSAGE_KEY = "plugin.dev_test";
                TranslationHandler.registerTemporaryTranslation(LOCAL_TEST_MESSAGE_KEY, "Local dev mode activated: Valid key.");
                LoggingUtils.logTranslated(LOCAL_TEST_MESSAGE_KEY);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    private String generateServerKey() {
        try {
            String ip = getConfigValue("server-ip", "127.0.0.1");
            String seed = getConfigValue("level-seed", "default_seed");
            String payload = ip + ":" + seed;
            byte[] hashBytes = java.security.MessageDigest.getInstance("SHA-256").digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            return null;
        }
    }
    private String getConfigValue(String key, String defaultValue) {
        File propertiesFile = new File("server.properties");
        try (BufferedReader reader = new BufferedReader(new FileReader(propertiesFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(key + "=")) {
                    return line.split("=", 2)[1];
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error reading server.properties: " + e.getMessage());
        }
        return defaultValue;
    }
}