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

import static java.lang.Math.clamp;

public class DisplayUtil {

    private static final Map<UUID, BossBar> bossBars = new HashMap<>();

    public static void showBossBar(Player player, double progress) {
        if (!ConfigHandler.isBossBarEnabled()) return;

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
        bossBar.setProgress(clamp(progress, 0.0, 1.0));
    }

    public static void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public static void showActionBar(Player player, double progress) {
        if (!ConfigHandler.isActionBarEnabled()) return;

        String message = ConfigHandler.getActionBarMessage().replace("{progress}", String.valueOf((int) (progress * 100)));

        player.sendActionBar(Component.text(message));
    }

    @SuppressWarnings("unused")
    public static void clearAllBossBars() {
        for (BossBar bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();
    }
}