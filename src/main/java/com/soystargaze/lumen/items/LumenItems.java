package com.soystargaze.lumen.items;

import com.soystargaze.lumen.utils.LumenConstants;
import com.soystargaze.lumen.utils.TranslationHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class LumenItems {

    private final Plugin plugin;

    private final Map<String, ItemStack> lumenTorchItems = new HashMap<>();

    public LumenItems(Plugin plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        ItemStack lumenTorch = createLumenTorch(
                TranslationHandler.get("items.torch.name"),
                TranslationHandler.get("items.torch.lore"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzViNTFjYzJlOTlkMDhkZDI4NzlhNzkyZjA2MmUwNzc4MzJhMDE2M2YzZDg1YzI0NGUwYmExYzM5MmFiMDlkZSJ9fX0=",
                "torch"
        );
        ItemStack lumenGuard = createLumenTorch(
                TranslationHandler.get("items.guard.name"),
                TranslationHandler.get("items.guard.lore"),
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzg0NDE3MjViZDQzMDczNmNmNDBkNGNlOTVjYjVhNWUxMDMwNWI3OTVhYzRmZjg0NzRlMDUzNWRmN2FmMWRkNyJ9fX0=",
                "guard"
        );

        lumenTorchItems.put("torch", lumenTorch);
        lumenTorchItems.put("guard", lumenGuard);

        registerRecipe("lumen_torch", lumenTorch, Material.GOLD_INGOT, Material.LANTERN);
        registerRecipe("lumen_guard", lumenGuard, Material.DIAMOND, Material.SOUL_LANTERN);
    }

    public ItemStack getLumenItem(String key) {
        return lumenTorchItems.getOrDefault(key, null);
    }

    private ItemStack createLumenTorch(String name, String lore, String texture, String identifier) {
        ItemStack head = getSkull(texture);
        ItemMeta meta = head.getItemMeta();

        if (meta != null) {
            Component displayName = MiniMessage.miniMessage()
                    .deserialize(name)
                    .decoration(TextDecoration.ITALIC, false);

            List<Component> loreComponents = List.of(
                    MiniMessage.miniMessage()
                            .deserialize(lore)
                            .decoration(TextDecoration.ITALIC, false)
            );

            LegacyComponentSerializer legacy = LegacyComponentSerializer.legacySection();
            String legacyName = legacy.serialize(displayName);
            List<String> legacyLore = loreComponents.stream()
                    .map(legacy::serialize)
                    .collect(Collectors.toList());

            meta.setDisplayName(legacyName);
            meta.setLore(legacyLore);

            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(LumenConstants.getLumenIdKey(),
                    PersistentDataType.STRING,
                    identifier);

            head.setItemMeta(meta);
        }

        return head;
    }

    private ItemStack getSkull(String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null) {
            try {
                String json = new String(
                        Base64.getDecoder().decode(texture),
                        StandardCharsets.UTF_8
                );
                int i = json.indexOf("\"url\":\"") + 7;
                int j = json.indexOf('"', i);
                String skinUrl = json.substring(i, j);

                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), null);
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(new URI(skinUrl).toURL());
                profile.setTextures(textures);

                meta.setOwnerProfile(profile);
                head.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("Error applying texture to skull: " + e.getMessage());
            }
        }

        return head;
    }

    private void registerRecipe(String key, ItemStack result, Material ingredientG, Material ingredientS) {
        NamespacedKey recipeKey = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);

        recipe.shape("GGG", "FSF", "GGG");
        recipe.setIngredient('G', ingredientG);
        recipe.setIngredient('S', ingredientS);
        recipe.setIngredient('F', Material.GLASS);

        Bukkit.addRecipe(recipe);
    }
}