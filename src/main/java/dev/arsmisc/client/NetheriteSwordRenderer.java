package dev.arsmisc.client;

import dev.arsmisc.item.NetheriteEnchantersSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Keep the native three-dimensional crystal blade, using Netherite-colored metal and purple crystal. */
public final class NetheriteSwordRenderer extends GeoItemRenderer<NetheriteEnchantersSwordItem> {
    public NetheriteSwordRenderer() { super(new SwordModel()); }
    public static GeoRenderProvider createProvider() {
        return new GeoRenderProvider() {
            private NetheriteSwordRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getGeoItemRenderer() {
                if (renderer==null) renderer=new NetheriteSwordRenderer();
                return renderer;
            }
        };
    }
    @Override public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack pose,
            NetheriteEnchantersSwordItem item,software.bernie.geckolib.cache.object.GeoBone bone,
            net.minecraft.client.renderer.RenderType type,net.minecraft.client.renderer.MultiBufferSource buffers,
            com.mojang.blaze3d.vertex.VertexConsumer vertices,boolean reRender,float partialTick,int light,int overlay,int color) {
        super.renderRecursively(pose,item,bone,type,buffers,vertices,reRender,partialTick,light,overlay,
                bone.getName().equals("blade")?0xDDB78DE9:color);
    }
    @Override public net.minecraft.client.renderer.RenderType getRenderType(NetheriteEnchantersSwordItem item,
            ResourceLocation texture,net.minecraft.client.renderer.MultiBufferSource buffers,float partialTick) {
        return net.minecraft.client.renderer.RenderType.entityTranslucent(texture);
    }
    private static final class SwordModel extends GeoModel<NetheriteEnchantersSwordItem> {
        @Override public ResourceLocation getModelResource(NetheriteEnchantersSwordItem item) { return NetheriteSwordTexture.MODEL; }
        @Override public ResourceLocation getTextureResource(NetheriteEnchantersSwordItem item) { return NetheriteSwordTexture.texture(); }
        @Override public ResourceLocation getAnimationResource(NetheriteEnchantersSwordItem item) {
            return ResourceLocation.fromNamespaceAndPath("ars_nouveau","animations/sword.json");
        }
    }
}
