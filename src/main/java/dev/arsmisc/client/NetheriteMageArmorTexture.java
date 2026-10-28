package dev.arsmisc.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.arsmisc.ArsMiscMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.IOException;

/** Recolors the installed native resource in memory; no upstream image is bundled. */
public final class NetheriteMageArmorTexture {
    private static final ResourceLocation SOURCE = ResourceLocation.fromNamespaceAndPath("ars_nouveau",
            "textures/item/medium_armor/medium_armor_black.png");
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,
            "dynamic/netherite_mage_armor");
    private static boolean available;

    public static ResourceLocation texture() { return available ? TEXTURE : SOURCE; }

    public static void reload(ResourceManager resources) {
        try (var input = resources.open(SOURCE)) {
            var image = NativeImage.read(input);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    int alpha = pixel >>> 24;
                    int red = pixel & 255, green = pixel >>> 8 & 255, blue = pixel >>> 16 & 255;
                    // Keep the native gold trim and any colored details intact.
                    if (alpha == 0 || red > blue + 18 || green > blue + 18) continue;
                    int light = (red * 3 + green * 6 + blue) / 10;
                    // Warm charcoal fabric and muted Netherite metal, with stronger readable highlights.
                    int r = Math.min(255, 24 + light * 55 / 100);
                    int g = Math.min(255, 21 + light * 49 / 100);
                    int b = Math.min(255, 25 + light * 53 / 100);
                    image.setPixelRGBA(x, y, alpha << 24 | b << 16 | g << 8 | r);
                }
            }
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(image));
            available = true;
        } catch (IOException exception) {
            available = false;
            ArsMiscMod.LOGGER.warn("Unable to recolor native mage armor texture", exception);
        }
    }

    private NetheriteMageArmorTexture() {}
}
