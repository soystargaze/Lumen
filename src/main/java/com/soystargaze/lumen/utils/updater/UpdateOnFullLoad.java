package com.soystargaze.lumen.utils.updater;

import com.soystargaze.lumen.Lumen;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class UpdateOnFullLoad implements Listener {

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (event.getType() == ServerLoadEvent.LoadType.STARTUP) {
            Bukkit.getScheduler().runTaskAsynchronously(
                    Lumen.getInstance(),
                    UpdateChecker::checkForUpdates
            );
        }
    }
}