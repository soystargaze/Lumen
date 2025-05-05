package com.soystargaze.lumen.commands;

import com.soystargaze.lumen.Lumen;

import java.util.Objects;

public class LumenCommandManager {

    private final Lumen plugin;
    private final RemoveCommand removeCommand;
    private final UndoCommand undoCommand;
    private final ClearCommand clearCommand;
    private final RedoCommand redoCommand;

    public LumenCommandManager(Lumen plugin) {
        this.plugin = plugin;
        this.removeCommand = new RemoveCommand(plugin);
        this.undoCommand = new UndoCommand(plugin);
        this.clearCommand = new ClearCommand();
        this.redoCommand = new RedoCommand(plugin);
    }

    public void registerCommands() {
        registerCommand("lumen");
        registerCommand("lu");
        registerCommand("l");
    }

    private void registerCommand(String commandName) {
        LumenCommandExecutor executor = new LumenCommandExecutor(plugin, removeCommand, undoCommand, clearCommand, redoCommand);
        Objects.requireNonNull(plugin.getCommand(commandName)).setExecutor(executor);
        Objects.requireNonNull(plugin.getCommand(commandName)).setTabCompleter(executor);
    }
}