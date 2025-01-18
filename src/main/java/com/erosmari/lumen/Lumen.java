package com.erosmari.lumen;

import com.erosmari.lumen.commands.LumenCommandManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lumen extends JavaPlugin {

    private LumenCommandManager commandManager;

    @Override
    public void onEnable() {
        // Inicializar el sistema de comandos
        commandManager = new LumenCommandManager(this);
        commandManager.registerCommands();

        getLogger().info("Lumen habilitado correctamente.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lumen deshabilitado.");
    }
}
