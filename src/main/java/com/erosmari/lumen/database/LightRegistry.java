package com.erosmari.lumen.database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LightRegistry {

    /**
     * Registra un bloque iluminado en la base de datos.
     *
     * @param location   Ubicación del bloque.
     * @param lightLevel Nivel de luz del bloque.
     */
    public static void addBlock(Location location, int lightLevel) {
        String query = "INSERT INTO illuminated_blocks (world, x, y, z, light_level) VALUES (?, ?, ?, ?, ?);";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());
            statement.setInt(5, lightLevel);

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Elimina un bloque iluminado de la base de datos.
     *
     * @param location Ubicación del bloque.
     */
    public static void removeBlock(Location location) {
        String query = "DELETE FROM illuminated_blocks WHERE world = ? AND x = ? AND y = ? AND z = ?;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, location.getWorld().getName());
            statement.setInt(2, location.getBlockX());
            statement.setInt(3, location.getBlockY());
            statement.setInt(4, location.getBlockZ());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Location createLocationFromResultSet(ResultSet resultSet) throws SQLException {
        String worldName = resultSet.getString("world");
        int x = resultSet.getInt("x");
        int y = resultSet.getInt("y");
        int z = resultSet.getInt("z");

        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return new Location(world, x, y, z);
        }
        return null; // Devuelve null si el mundo no es válido
    }

    /**
     * Obtiene todos los bloques iluminados almacenados en la base de datos.
     *
     * @return Una lista de ubicaciones de bloques iluminados.
     */
    public static List<Location> getAllBlocks() {
        String query = "SELECT * FROM illuminated_blocks;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Location location = createLocationFromResultSet(resultSet);
                if (location != null) {
                    blocks.add(location);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return blocks;
    }

    /**
     * Elimina todos los bloques iluminados de la base de datos.
     */
    public static void clearAllBlocks() {
        String query = "DELETE FROM illuminated_blocks;";

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<Location> getBlocksInRange(Location center, int range) {
        String query = "SELECT * FROM illuminated_blocks WHERE world = ? AND x BETWEEN ? AND ? AND y BETWEEN ? AND ? AND z BETWEEN ? AND ?;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, center.getWorld().getName());
            statement.setInt(2, center.getBlockX() - range);
            statement.setInt(3, center.getBlockX() + range);
            statement.setInt(4, center.getBlockY() - range);
            statement.setInt(5, center.getBlockY() + range);
            statement.setInt(6, center.getBlockZ() - range);
            statement.setInt(7, center.getBlockZ() + range);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        blocks.add(location);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return blocks;
    }

    public static List<Location> getBlocksByWorld(String worldName) {
        String query = "SELECT * FROM illuminated_blocks WHERE world = ?;";
        List<Location> blocks = new ArrayList<>();

        try (Connection connection = DatabaseHandler.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1, worldName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Location location = createLocationFromResultSet(resultSet);
                    if (location != null) {
                        blocks.add(location);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return blocks;
    }
}
