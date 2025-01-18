package com.erosmari.lumen.tasks;

import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskManager {
    private static final Map<UUID, BukkitTask> activeTasks = new HashMap<>();

    /**
     * Añade una tarea activa para un jugador.
     *
     * @param playerId  El UUID del jugador.
     * @param task      La tarea programada.
     */
    public static void addTask(UUID playerId, BukkitTask task) {
        cancelTask(playerId); // Cancelar cualquier tarea previa antes de añadir una nueva.
        activeTasks.put(playerId, task);
    }

    /**
     * Cancela la tarea activa de un jugador.
     *
     * @param playerId El UUID del jugador.
     */
    public static void cancelTask(UUID playerId) {
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Verifica si un jugador tiene una tarea activa.
     *
     * @param playerId El UUID del jugador.
     * @return Verdadero si hay una tarea activa.
     */
    public static boolean hasActiveTask(UUID playerId) {
        return activeTasks.containsKey(playerId);
    }
}