package com.soystargaze.lumen.utils.updater;

import com.soystargaze.lumen.Lumen;
import com.soystargaze.lumen.utils.AsyncExecutor;
import com.soystargaze.lumen.utils.text.TextHandler;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {

    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";
    private static final String PROJECT_ID = "lumen";
    private static final String CURRENT_VERSION = Lumen.getInstance().getPluginMeta().getVersion();
    private static final String DOWNLOAD_URL = "https://modrinth.com/plugin/lumen";

    public static void checkForUpdates(Player player) {
        AsyncExecutor.getExecutor().execute(() -> {
            try {
                String url = String.format(API_URL, PROJECT_ID);
                URI uri = new URI(url);
                URL urlObj = uri.toURL();
                HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "soystargaze/Lumen/" + CURRENT_VERSION + " (dev@soystargaze.com)");

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    String latestVersion = parseLatestVersion(content.toString());
                    if (latestVersion != null && !latestVersion.equalsIgnoreCase(CURRENT_VERSION)) {
                        TextHandler.get().sendMessage(player, "updater.update_available", latestVersion, DOWNLOAD_URL);
                    }
                }
            } catch (Exception e) {
                TextHandler.get().logTranslated("updater.error_checking", e.getMessage());
            }
        });
    }

    private static String parseLatestVersion(String jsonResponse) {
        try {
            int versionStart = jsonResponse.indexOf("\"version_number\":\"") + 18;
            int versionEnd = jsonResponse.indexOf("\"", versionStart);
            return jsonResponse.substring(versionStart, versionEnd);
        } catch (Exception e) {
            return null;
        }
    }
}
