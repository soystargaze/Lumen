package com.erosmari.lumen.database;

import com.erosmari.lumen.utils.TranslationHandler;
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

            logger.info(TranslationHandler.getFormatted("mob_registry.protected_area_added", location, radius));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("mob_registry.error.adding_area", location), e);
        }
    }

    public static void removeProtectedArea(Location location) {
        String query = "DELETE FROM protected_areas WHERE world = ? AND x = ? AND y = ? AND z = ?;";
        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, location.getBlockX());
            stmt.setInt(3, location.getBlockY());
            stmt.setInt(4, location.getBlockZ());
            stmt.executeUpdate();

            logger.info(TranslationHandler.getFormatted("mob_registry.protected_area_removed", location));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.getFormatted("mob_registry.error.removing_area", location), e);
        }
    }

    public static Map<Location, Integer> getNearbyProtectedAreas(Location location, int range) {
        String query = "SELECT world, x, y, z, radius " +
                "FROM protected_areas " +
                "WHERE world = ? " +
                "AND x BETWEEN ? AND ? " +
                "AND z BETWEEN ? AND ?;";

        Map<Location, Integer> areas = new HashMap<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            int minX = location.getBlockX() - range;
            int maxX = location.getBlockX() + range;
            int minZ = location.getBlockZ() - range;
            int maxZ = location.getBlockZ() + range;

            stmt.setString(1, location.getWorld().getName());
            stmt.setInt(2, minX);
            stmt.setInt(3, maxX);
            stmt.setInt(4, minZ);
            stmt.setInt(5, maxZ);

            try (ResultSet rs = stmt.executeQuery()) {
                areas.putAll(processResultSet(rs));
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.get("mob_registry.error.fetching_areas"), e);
        }

        return areas;
    }

    public static Map<Location, Integer> getProtectedAreas() {
        String query = "SELECT world, x, y, z, radius FROM protected_areas;";
        Map<Location, Integer> areas = new HashMap<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            areas.putAll(processResultSet(rs));

        } catch (SQLException e) {
            logger.log(Level.SEVERE, TranslationHandler.get("mob_registry.error.fetching_areas"), e);
        }

        return areas;
    }

    private static Map<Location, Integer> processResultSet(ResultSet rs) throws SQLException {
        Map<Location, Integer> areas = new HashMap<>();

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
                logger.warning(TranslationHandler.getFormatted("mob_registry.warning.world_not_found", worldName));
            }
        }

        return areas;
    }
}