package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsmisc.spell.ReactiveShieldCasting;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class ReactiveShieldBlockMixin {
    @WrapMethod(method = "hurt")
    private boolean shieldDamageScope(DamageSource source, float amount, Operation<Boolean> original) {
        return ReactiveShieldCasting.duringDamage((LivingEntity)(Object)this, () -> original.call(source, amount));
    }
    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/damagesource/DamageContainer;setBlockedDamage(Lnet/neoforged/neoforge/event/entity/living/LivingShieldBlockEvent;)V", remap = false))
    private void committedBlock(DamageContainer container, LivingShieldBlockEvent event, Operation<Void> original) {
        original.call(container, event);
        if (!event.isCanceled()) {
            ReactiveShieldCasting.recordBlock((LivingEntity)(Object)this, container.getBlockedDamage());
            dev.arsmisc.spell.AmplifiedCutShield.recordBlock((LivingEntity)(Object)this,
                    container.getSource(), container.getBlockedDamage());
        }
    }
}
