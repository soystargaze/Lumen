package com.erosmari.lumen.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public class ItemEffectUtil {

    /**
     * Ejecuta un efecto de partículas y sonido en una ubicación específica.
     *
     * @param location La ubicación donde se generarán los efectos.
     * @param effectType El tipo de efecto (por ejemplo, "torch" o "mob_torch").
     */
    public static void playEffect(Location location, String effectType) {
        if (location == null || location.getWorld() == null) {
            return; // Evitar errores si la ubicación o el mundo son nulos.
        }

        World world = location.getWorld();

        // Configurar los efectos según el tipo
        switch (effectType.toLowerCase()) {
            case "torch":
                playTorchEffect(world, location);
                break;

            case "guard":
                playGuardEffect(world, location);
                break;

            default:
                // Efecto genérico o futuro
                world.spawnParticle(Particle.CRIT, location, 10, 0.5, 0.5, 0.5, 0.1);
                world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                break;
        }
    }

    /**
     * Efecto de partículas y sonido para el Lumen Torch.
     */
    private static void playTorchEffect(World world, Location location) {
        // Partículas de chispa y humo
        world.spawnParticle(Particle.FLAME, location.add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.SMOKE, location.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);

        // Sonido de colocación
        world.playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
    }

    /**
     * Efecto de partículas y sonido para el Lumen Torch Mob.
     */
    private static void playGuardEffect(World world, Location location) {
        // Partículas de esmeralda y chispa
        world.spawnParticle(Particle.HAPPY_VILLAGER, location.add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.CRIT, location.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);

        // Sonido de campanilla
        world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
    }
}
