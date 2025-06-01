package com.soystargaze.lumen.utils.updater;

import com.soystargaze.lumen.config.ConfigHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateOnJoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!ConfigHandler.getConfig().getBoolean("update-check", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.isOp()) {
            UpdateChecker.checkForUpdates(player);
        }
    }

}