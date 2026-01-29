package me.sunmc.dodgeball.utility;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages custom model data for textures in the game.
 * Allows easy application of custom textures via resource packs.
 *
 * <p>Usage:
 * <pre>{@code
 * // Register a texture
 * TextureManager.registerTexture("red_ball", Material.SNOWBALL, 1001);
 *
 * // Apply it to an item
 * ItemStack ball = TextureManager.getTexturedItem("red_ball");
 * }</pre>
 */
public class TextureManager {

    private static final Map<String, TextureData> REGISTERED_TEXTURES = new HashMap<>();

    // Default textures for dodgeball items
    static {
        registerTexture("snowball_default", Material.SNOWBALL, 0);
        registerTexture("red_dodgeball", Material.SNOWBALL, 1001);
        registerTexture("blue_dodgeball", Material.SNOWBALL, 1002);
        registerTexture("green_dodgeball", Material.SNOWBALL, 1003);
        registerTexture("yellow_dodgeball", Material.SNOWBALL, 1004);
    }

    /**
     * Registers a texture with a custom model data ID.
     *
     * @param id              Texture identifier
     * @param material        Base material
     * @param customModelData Custom model data integer
     */
    public static void registerTexture(@NotNull String id, @NotNull Material material, int customModelData) {
        REGISTERED_TEXTURES.put(id, new TextureData(material, customModelData));
    }

    /**
     * Gets an ItemStack with the specified texture applied.
     *
     * @param id Texture identifier
     * @return ItemStack with custom model data, or default if not found
     */
    public static @NotNull ItemStack getTexturedItem(@NotNull String id) {
        return getTexturedItem(id, 1);
    }

    /**
     * Gets an ItemStack with the specified texture and amount.
     *
     * @param id     Texture identifier
     * @param amount Stack size
     * @return ItemStack with custom model data
     */
    public static @NotNull ItemStack getTexturedItem(@NotNull String id, int amount) {
        TextureData data = REGISTERED_TEXTURES.get(id);
        if (data == null) {
            // Fallback to default snowball
            return new ItemStack(Material.SNOWBALL, amount);
        }

        ItemStack item = new ItemStack(data.material, amount);
        if (data.customModelData > 0) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(data.customModelData);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Applies a texture to an existing ItemStack.
     *
     * @param item ItemStack to modify
     * @param id   Texture identifier
     * @return Modified ItemStack
     */
    public static @NotNull ItemStack applyTexture(@NotNull ItemStack item, @NotNull String id) {
        TextureData data = REGISTERED_TEXTURES.get(id);
        if (data == null || data.customModelData <= 0) {
            return item;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data.customModelData);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Gets the custom model data for a texture ID.
     *
     * @param id Texture identifier
     * @return Custom model data, or 0 if not found
     */
    public static int getCustomModelData(@NotNull String id) {
        TextureData data = REGISTERED_TEXTURES.get(id);
        return data != null ? data.customModelData : 0;
    }

    /**
     * Checks if a texture is registered.
     *
     * @param id Texture identifier
     * @return true if registered
     */
    public static boolean isRegistered(@NotNull String id) {
        return REGISTERED_TEXTURES.containsKey(id);
    }

    /**
     * Gets all registered texture IDs.
     *
     * @return Map of texture IDs to their data
     */
    @Contract(pure = true)
    public static @NotNull @Unmodifiable Map<String, TextureData> getRegisteredTextures() {
        return Map.copyOf(REGISTERED_TEXTURES);
    }

    /**
     * Data holder for texture information.
     */
    public record TextureData(@NotNull Material material, int customModelData) {
    }
}