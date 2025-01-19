package com.erosmari.lumen.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MobRegistry {

    private static final Logger logger = Logger.getLogger("Lumen-MobRegistry");

    /**
     * Añade un área protegida contra mobs.
     *
     * @param location La ubicación central del área.
     * @param radius   El radio del área protegida.
     */
    public static void addProtectedArea(Location location, int radius) {
        String query = "INSERT INTO protected_areas (world, x, y, z, radius) VALUES (?, ?, ?, ?, ?);";
        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            stmt.setInt(5, radius);
            stmt.executeUpdate();

            logger.info("Área protegida añadida en " + location);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al añadir el área protegida: " + location, e);
        }
    }

    /**
     * Elimina un área protegida contra mobs.
     *
     * @param location La ubicación central del área.
     */
    public static void removeProtectedArea(Location location) {
        String query = "DELETE FROM protected_areas WHERE world = ? AND x = ? AND y = ? AND z = ?;";
        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            stmt.executeUpdate();

            logger.info("Área protegida eliminada en " + location);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al eliminar el área protegida: " + location, e);
        }
    }

    /**
     * Obtiene todas las áreas protegidas contra mobs.
     *
     * @return Un mapa de ubicaciones y radios de las áreas protegidas.
     */
    public static Map<Location, Integer> getProtectedAreas() {
        String query = "SELECT world, x, y, z, radius FROM protected_areas;";
        Map<Location, Integer> areas = new HashMap<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String worldName = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                int radius = rs.getInt("radius");

                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    Location location = new Location(world, x, y, z);
                    areas.put(location, radius);
                } else {
                    logger.warning("No se pudo encontrar el mundo: " + worldName);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error al obtener las áreas protegidas.", e);
        }

        return areas;
    }
}
