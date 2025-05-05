package com.soystargaze.lumen.tasks;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskManager {
    private static final Map<UUID, BukkitTask> playerTasks = new HashMap<>();

    public static void addTask(UUID playerId, BukkitTask task) {
        cancelTask(playerId);
        playerTasks.put(playerId, task);
    }

    public static void cancelTask(UUID playerId) {
        BukkitTask task = playerTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    public static boolean hasActiveTask(UUID playerId) {
        return playerTasks.containsKey(playerId);
    }

}
