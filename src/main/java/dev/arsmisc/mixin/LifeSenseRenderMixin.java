package dev.arsmisc.mixin;

import dev.arsmisc.LifeSenseEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class LifeSenseRenderMixin {
    @Inject(method="shouldRender", at=@At("HEAD"), cancellable=true)
    private void arsMisc$renderSensed(Entity entity, Frustum frustum, double x, double y, double z,
            CallbackInfoReturnable<Boolean> cir) {
        if (LifeSenseEffect.shouldHighlight(Minecraft.getInstance().player, entity)) cir.setReturnValue(true);
    }
}
