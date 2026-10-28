package dev.arsmisc.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.arsmisc.ArsMiscMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Recolor only the installed native hilt UVs; the source texture and geometry are not bundled. */
public final class NetheriteSwordTexture {
    public static final ResourceLocation MODEL=ResourceLocation.fromNamespaceAndPath("ars_nouveau","geo/sword.geo.json");
    public static final ResourceLocation SOURCE=ResourceLocation.fromNamespaceAndPath("ars_nouveau","textures/item/enchanters_sword.png");
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,"dynamic/netherite_enchanters_sword");
    private static boolean available;
    public static ResourceLocation texture() { return available ? TEXTURE : SOURCE; }
    public static void reload(ResourceManager resources) {
        try (var input=resources.open(SOURCE);
                var model=new InputStreamReader(resources.open(MODEL),StandardCharsets.UTF_8)) {
            var image=NativeImage.read(input);
            boolean[][] hilt=new boolean[image.getWidth()][image.getHeight()];
            for (var geometry:JsonParser.parseReader(model).getAsJsonObject().getAsJsonArray("minecraft:geometry")) {
                for (var entry:geometry.getAsJsonObject().getAsJsonArray("bones")) {
                    var bone=entry.getAsJsonObject();
                    if (!bone.get("name").getAsString().startsWith("hilt") || !bone.has("cubes")) continue;
                    for (var cube:bone.getAsJsonArray("cubes")) {
                        var uv=cube.getAsJsonObject().get("uv");
                        if (uv==null || !uv.isJsonObject()) continue;
                        for (var face:uv.getAsJsonObject().entrySet()) {
                            var f=face.getValue().getAsJsonObject();var pos=f.getAsJsonArray("uv");var size=f.getAsJsonArray("uv_size");
                            if (pos==null || size==null) continue;
                            double x=pos.get(0).getAsDouble(),y=pos.get(1).getAsDouble();
                            double endX=x+size.get(0).getAsDouble(),endY=y+size.get(1).getAsDouble();
                            for (int px=Math.max(0,(int)Math.floor(Math.min(x,endX)));px<Math.min(image.getWidth(),(int)Math.ceil(Math.max(x,endX)));px++)
                                for (int py=Math.max(0,(int)Math.floor(Math.min(y,endY)));py<Math.min(image.getHeight(),(int)Math.ceil(Math.max(y,endY)));py++) hilt[px][py]=true;
                        }
                    }
                }
            }
            for (int y=0;y<image.getHeight();y++) for (int x=0;x<image.getWidth();x++) {
                int pixel=image.getPixelRGBA(x,y),alpha=pixel>>>24;
                if (!hilt[x][y] || alpha==0) continue;
                int red=pixel&255,green=pixel>>>8&255,blue=pixel>>>16&255;
                int light=(red*3+green*6+blue)/10;
                int r=25+light*40/100,g=22+light*34/100,b=29+light*41/100;
                image.setPixelRGBA(x,y,alpha<<24|b<<16|g<<8|r);
            }
            Minecraft.getInstance().getTextureManager().register(TEXTURE,new DynamicTexture(image));available=true;
        } catch (java.io.IOException | RuntimeException exception) {
            available=false;ArsMiscMod.LOGGER.warn("Unable to recolor native crystal sword texture",exception);
        }
    }
    private NetheriteSwordTexture() {}
}
