package com.soystargaze.lumen.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;

public class ItemEffectUtil {

    public static void playEffect(Location location, String effectType) {
        if (location == null || location.getWorld() == null) {
            return;
        }

        World world = location.getWorld();

        switch (effectType.toLowerCase()) {
            case "torch":
                playTorchEffect(world, location);
                break;

            case "guard":
                playGuardEffect(world, location);
                break;

            default:
                world.spawnParticle(Particle.CRIT, location, 10, 0.5, 0.5, 0.5, 0.1);
                world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                break;
        }
    }

    private static void playTorchEffect(World world, Location location) {
        world.spawnParticle(Particle.FLAME, location.add(0.5, 0.5, 0.5), 20, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.SMOKE, location.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);

        world.playSound(location, Sound.BLOCK_FIRE_AMBIENT, 1f, 1f);
    }

    private static void playGuardEffect(World world, Location location) {
        world.spawnParticle(Particle.HAPPY_VILLAGER, location.add(0.5, 0.5, 0.5), 15, 0.3, 0.3, 0.3, 0.01);
        world.spawnParticle(Particle.CRIT, location.add(0.5, 0.5, 0.5), 10, 0.2, 0.2, 0.2, 0.01);

        world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.5f);
    }
}
