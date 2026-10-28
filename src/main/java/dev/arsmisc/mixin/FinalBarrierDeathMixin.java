package dev.arsmisc.mixin;

import dev.arsmisc.FinalBarrier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers lethal health writes that reach death ticking without posting a death event. */
@Mixin(LivingEntity.class)
public abstract class FinalBarrierDeathMixin {
    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void arsMisc$barrierRescue(CallbackInfo callback) {
        if ((Object) this instanceof Player player && FinalBarrier.rescue(player)) callback.cancel();
    }
}
