package dev.arsmisc.client;

import dev.arsmisc.item.NetheriteMageArmorItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public final class NetheriteMageArmorRenderer extends GeoArmorRenderer<NetheriteMageArmorItem> {
    public NetheriteMageArmorRenderer() { super(new ArmorModel()); }

    public static GeoRenderProvider createProvider() {
        return new GeoRenderProvider() {
            private NetheriteMageArmorRenderer renderer;
            @Override public <T extends LivingEntity> HumanoidModel<?> getGeoArmorRenderer(T entity,
                    ItemStack stack, EquipmentSlot slot, HumanoidModel<T> original) {
                if (renderer == null) renderer = new NetheriteMageArmorRenderer();
                renderer.prepForRender(entity, stack, slot, original);
                return renderer;
            }
        };
    }

    @Override protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        super.applyBoneVisibilityBySlot(slot);
        boolean slim = currentEntity instanceof AbstractClientPlayer player
                && player.getSkin().model() == PlayerSkin.Model.SLIM;
        getGeoModel().getBone("armorRightArmNormal").ifPresent(bone -> bone.setHidden(slim));
        getGeoModel().getBone("armorLeftArmNormal").ifPresent(bone -> bone.setHidden(slim));
        getGeoModel().getBone("armorRightArmSlim").ifPresent(bone -> bone.setHidden(!slim));
        getGeoModel().getBone("armorLeftArmSlim").ifPresent(bone -> bone.setHidden(!slim));
    }

    @Override public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack pose,
            NetheriteMageArmorItem item, software.bernie.geckolib.cache.object.GeoBone bone,
            net.minecraft.client.renderer.RenderType renderType,
            net.minecraft.client.renderer.MultiBufferSource buffers,
            com.mojang.blaze3d.vertex.VertexConsumer vertices, boolean reRender,
            float partialTick, int light, int overlay, int color) {
        float x = bone.getScaleX(), y = bone.getScaleY(), z = bone.getScaleZ();
        // Scope changes to this draw: the baked native model is shared with other armor renderers.
        switch (bone.getName()) {
            case "hat1" -> bone.updateScale(x * 0.94f, y, z * 0.94f);
            case "hat2" -> bone.updateScale(x * 0.94f, y * 1.12f, z * 0.94f);
            case "armorBody" -> bone.updateScale(x * 1.025f, y, z * 1.04f);
            case "armorRightArmNormal", "armorLeftArmNormal", "armorRightArmSlim", "armorLeftArmSlim" ->
                    bone.updateScale(x * 1.04f, y, z * 1.04f);
            default -> {}
        }
        try {
            super.renderRecursively(pose, item, bone, renderType, buffers, vertices, reRender, partialTick, light, overlay, color);
        } finally {
            bone.updateScale(x, y, z);
        }
    }

    private static final class ArmorModel extends GeoModel<NetheriteMageArmorItem> {
        @Override public ResourceLocation getModelResource(NetheriteMageArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath("ars_nouveau", "geo/medium_armor.geo.json");
        }
        @Override public ResourceLocation getTextureResource(NetheriteMageArmorItem item) {
            return NetheriteMageArmorTexture.texture();
        }
        @Override public ResourceLocation getAnimationResource(NetheriteMageArmorItem item) {
            return ResourceLocation.fromNamespaceAndPath("ars_nouveau", "animations/empty.json");
        }
    }
}
