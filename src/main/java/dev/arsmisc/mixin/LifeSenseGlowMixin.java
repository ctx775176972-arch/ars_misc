package dev.arsmisc.mixin;

import dev.arsmisc.LifeSenseEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class LifeSenseGlowMixin {
    @Inject(method="shouldEntityAppearGlowing", at=@At("RETURN"), cancellable=true)
    private void arsMisc$lifeSense(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (LifeSenseEffect.shouldHighlight(Minecraft.getInstance().player, entity)) cir.setReturnValue(true);
    }
}
