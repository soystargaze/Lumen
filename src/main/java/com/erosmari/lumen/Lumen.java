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
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.crypto.Cipher;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

public class Lumen extends JavaPlugin implements Listener {

    private static Lumen instance;
    private LumenCommandManager commandManager;
    private LumenItems lumenItems;
    private CoreProtectHandler coreProtectHandler;
    private static final String API_URL = "https://api.polymart.org/v1/verifyPurchase";

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
        verifyLicenseTask();
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

    @SuppressWarnings({"ConstantConditions", "CallToPrintStackTrace"})
    private boolean verifyPurchase() {
        try {
            String resource = "%%__RESOURCE__%%";
            String user = "%%__USER__%%";
            String license = "%%__LICENSE__%%";

            String params;
            if (license != null && !license.contains("%%__LICENSE__%%")) {
                String signature = "%%__SIGNATURE__%%";
                params = "license=" + URLEncoder.encode(license, StandardCharsets.UTF_8) +
                        "&resource_id=" + URLEncoder.encode(resource, StandardCharsets.UTF_8) +
                        "&user_id=" + URLEncoder.encode(user, StandardCharsets.UTF_8) +
                        "&signature=" + URLEncoder.encode(signature, StandardCharsets.UTF_8);

                String rsaPublicKeyStr = "%%__RSA_PUBLIC_KEY__%%";
                try {
                    String decryptedSignature = decryptSignature(signature, rsaPublicKeyStr);
                    getLogger().info("Signature: " + decryptedSignature);
                } catch (Exception ex) {
                    getLogger().warning("Error: " + ex.getMessage());
                }
            } else {
                String downloadToken = "%%__VERIFY_TOKEN__%%";
                String nonce = "%%__NONCE__%%";
                String injectVersion = "%%__INJECT_VER__%%";
                String downloadAgent = "%%__AGENT__%%";
                String downloadTime = "%%__TIMESTAMP__%%";
                params = "download_token=" + URLEncoder.encode(downloadToken, StandardCharsets.UTF_8) +
                        "&user_id=" + URLEncoder.encode(user, StandardCharsets.UTF_8) +
                        "&resource_id=" + URLEncoder.encode(resource, StandardCharsets.UTF_8) +
                        "&nonce=" + URLEncoder.encode(nonce, StandardCharsets.UTF_8) +
                        "&inject_version=" + URLEncoder.encode(injectVersion, StandardCharsets.UTF_8) +
                        "&download_agent=" + URLEncoder.encode(downloadAgent, StandardCharsets.UTF_8) +
                        "&download_time=" + URLEncoder.encode(downloadTime, StandardCharsets.UTF_8);
            }
            String response = sendPostRequest(params);
            //getLogger().info("Polymart response: " + response);
            return response.contains("\"success\":true");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envía la solicitud POST con los parámetros proporcionados y retorna la respuesta como String.
     */
    private String sendPostRequest(String params) throws IOException {
        URL url = URI.create(API_URL).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = params.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return readResponse(connection);
    }

    /**
     * Lee la respuesta del HttpURLConnection y la retorna como String.
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    /**
     * Tarea asíncrona que verifica la licencia y, en caso de fallo, desactiva el plugin.
     */
    private void verifyLicenseTask() {
        if (isLocalTest()) {
            return;
        }
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            if (verifyPurchase()) {
                final String LICENSE_SUCCESS_KEY = "plugin.license_verified";
                TranslationHandler.registerTemporaryTranslation(LICENSE_SUCCESS_KEY, "License verification successful.");
                LoggingUtils.logTranslated(LICENSE_SUCCESS_KEY);
            } else {
                handleLicenseFailure();
            }
        });
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
            byte[] hashBytes = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
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

    /**
     * Descifra el signature usando RSA con padding PKCS1.
     *
     * @param signatureStr La cadena del signature (web-safe base64).
     * @param rsaPublicKeyStr El placeholder con la clave pública RSA.
     * @return El JSON descifrado (como String).
     * @throws Exception Si ocurre algún error durante el proceso de descifrado.
     */
    private String decryptSignature(String signatureStr, String rsaPublicKeyStr) throws Exception {
        String publicKeyStandard = rsaPublicKeyStr.replace('-', '+').replace('_', '/');
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStandard);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        String signatureStandard = signatureStr.replace('-', '+').replace('_', '/');
        byte[] signatureBytes = Base64.getDecoder().decode(signatureStandard);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        byte[] decryptedBytes = cipher.doFinal(signatureBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}