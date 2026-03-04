package com.soystargaze.lumen.utils;

import com.soystargaze.lumen.config.ConfigHandler;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.lang.Math.clamp;

public class DisplayUtil {

    private static final Map<UUID, BossBar> bossBars = new HashMap<>();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public static void showBossBar(Player player, double progress) {
        if (!ConfigHandler.isBossBarEnabled()) return;

        String rawMessage = ConfigHandler.getBossBarMessage().replace("{progress}", String.valueOf((int) (progress * 100)));
        Component title = MINI.deserialize(rawMessage);
        BossBar.Color color = ConfigHandler.getBossBarColor();
        BossBar.Overlay overlay = ConfigHandler.getBossBarStyle();

        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar == null) {
            bossBar = BossBar.bossBar(title, (float) clamp(progress, 0.0, 1.0), color, overlay);
            bossBars.put(player.getUniqueId(), bossBar);
            player.showBossBar(bossBar);
        } else {
            bossBar.name(title);
            bossBar.progress((float) clamp(progress, 0.0, 1.0));
            bossBar.color(color);
            bossBar.overlay(overlay);
        }
    }

    public static void hideBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    public static void showActionBar(Player player, double progress) {
        if (!ConfigHandler.isActionBarEnabled()) return;

        String rawMessage = ConfigHandler.getActionBarMessage().replace("{progress}", String.valueOf((int) (progress * 100)));
        player.sendActionBar(MINI.deserialize(rawMessage));
    }

    public static void clearAllBossBars(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }
}
