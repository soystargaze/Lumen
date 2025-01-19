package com.erosmari.lumen.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class LumenItems {

    private final Plugin plugin;

    public LumenItems(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        // Crear los objetos con texturas
        ItemStack lumenTorch = createLumenTorch("Lumen Torch",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzViNTFjYzJlOTlkMDhkZDI4NzlhNzkyZjA2MmUwNzc4MzJhMDE2M2YzZDg1YzI0NGUwYmExYzM5MmFiMDlkZSJ9fX0=");
        ItemStack lumenTorchMob = createLumenTorch("Lumen Torch Mob",
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzg0NDE3MjViZDQzMDczNmNmNDBkNGNlOTVjYjVhNWUxMDMwNWI3OTVhYzRmZjg0NzRlMDUzNWRmN2FmMWRkNyJ9fX0=");

        // Registrar las recetas de los objetos
        registerRecipe("lumen_torch", lumenTorch, Material.GOLD_INGOT);
        registerRecipe("lumen_torch_mob", lumenTorchMob, Material.DIAMOND);
    }

    private ItemStack createLumenTorch(String name, String texture) {
        // Crear la cabeza personalizada con la textura
        ItemStack head = getSkull(texture);
        ItemMeta meta = head.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(name)); // Asignar el nombre al objeto
            head.setItemMeta(meta);
        }

        return head;
    }

    private ItemStack getSkull(String textures) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        head.editMeta(SkullMeta.class, skullMeta -> {
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID());
            playerProfile.setProperty(new ProfileProperty("textures", textures));
            skullMeta.setPlayerProfile(playerProfile);
        });

        return head;
    }

    private void registerRecipe(String key, ItemStack result, Material ingredientG) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        recipe.shape("GGG", "FSF", "GGG");
        recipe.setIngredient('G', ingredientG);
        recipe.setIngredient('S', Material.SEA_LANTERN);
        recipe.setIngredient('F', Material.GLASS);

        Bukkit.addRecipe(recipe);
        plugin.getLogger().info("Registered recipe: " + key);
    }
}
