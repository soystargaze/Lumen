package com.erosmari.lumen.utils;

import com.erosmari.lumen.config.ConfigHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisplayUtil {

    private static final Map<UUID, BossBar> bossBars = new HashMap<>();

    /**
     * Muestra un BossBar al jugador con progreso dinámico de 0 a 100.
     *
     * @param player   El jugador que verá el BossBar.
     * @param progress Progreso entre 0.0 y 1.0 (se convierte a porcentaje).
     */
    public static void showBossBar(Player player, double progress) {
        if (!ConfigHandler.isBossBarEnabled()) return; // Desactivado en config.yml

        String message = ConfigHandler.getBossBarMessage().replace("{progress}", String.valueOf((int) (progress * 100)));
        BarColor color = ConfigHandler.getBossBarColor();
        BarStyle style = ConfigHandler.getBossBarStyle();

        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(message, color, style);
            bossBars.put(player.getUniqueId(), bossBar);
            bossBar.addPlayer(player);
        }

        bossBar.setTitle(message);
        bossBar.setProgress(Math.max(0.0, Math.min(progress, 1.0))); // Clampea el progreso
    }

    /**
     * Oculta el BossBar de un jugador.
     *
     * @param player El jugador al que se le ocultará el BossBar.
     */
    public static void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /**
     * Muestra un mensaje de ActionBar al jugador con progreso dinámico de 0 a 100.
     *
     * @param player   El jugador que verá el ActionBar.
     * @param progress Progreso entre 0.0 y 1.0 (se convierte a porcentaje).
     */
    public static void showActionBar(Player player, double progress) {
        if (!ConfigHandler.isActionBarEnabled()) return; // Desactivado en config.yml

        String message = ConfigHandler.getActionBarMessage().replace("{progress}", String.valueOf((int) (progress * 100)));

        // Usando Adventure para enviar el mensaje
        player.sendActionBar(Component.text(message));
    }

    /**
     * Limpia todos los BossBars activos.
     */
    @SuppressWarnings("unused")
    public static void clearAllBossBars() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
    }
}