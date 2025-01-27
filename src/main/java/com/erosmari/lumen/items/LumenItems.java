package com.erosmari.lumen.items;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.erosmari.lumen.utils.TranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LumenItems {

    private final Plugin plugin;

    // Mapa para almacenar los ítems personalizados
    private final Map<String, ItemStack> lumenItems = new HashMap<>();

    public LumenItems(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        // Crear los objetos con texturas
        ItemStack lumenTorch = createLumenTorch(
                TranslationHandler.get("items.torch.name"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzViNTFjYzJlOTlkMDhkZDI4NzlhNzkyZjA2MmUwNzc4MzJhMDE2M2YzZDg1YzI0NGUwYmExYzM5MmFiMDlkZSJ9fX0=",
                "torch"
        );
        ItemStack lumenGuard = createLumenTorch(
                TranslationHandler.get("items.guard.name"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzg0NDE3MjViZDQzMDczNmNmNDBkNGNlOTVjYjVhNWUxMDMwNWI3OTVhYzRmZjg0NzRlMDUzNWRmN2FmMWRkNyJ9fX0=",
                "guard"
        );

        // Agregar los ítems al mapa
        lumenItems.put("torch", lumenTorch);
        lumenItems.put("guard", lumenGuard);

        // Registrar las recetas de los objetos
        registerRecipe("lumen_torch", lumenTorch, Material.GOLD_INGOT);
        registerRecipe("lumen_guard", lumenGuard, Material.DIAMOND);
    }

    /**
     * Obtiene un ítem personalizado por su clave.
     *
     * @param key La clave del ítem (por ejemplo, "light" o "anti_mob").
     * @return El ItemStack correspondiente, o null si no existe.
     */
    public ItemStack getLumenItem(String key) {
        return lumenItems.getOrDefault(key, null);
    }

    private ItemStack createLumenTorch(String name, String texture, String identifier) {
        // Crear la cabeza personalizada con la textura
        ItemStack head = getSkull(texture);
        ItemMeta meta = head.getItemMeta();

        if (meta != null) {
            Component displayName = MiniMessage.miniMessage().deserialize(name).decoration(TextDecoration.ITALIC, false);
            meta.displayName(displayName);

            // Agregar el identificador al PersistentDataContainer
            NamespacedKey key = new NamespacedKey(plugin, "lumen_id");
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(key, PersistentDataType.STRING, identifier);

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
    }
}
