package dev.arsmisc.mixin;

import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;
import java.util.Map;

/** Read-through queries apply only to the exact fake player used by the current spell harvest. */
@Mixin(LivingEntity.class)
public abstract class SpellHarvestEffectsMixin {
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void arsMisc$hasEffect(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> result) {
        var owner = SpellBlockLootOwner.effectOwner((LivingEntity) (Object) this);
        if (owner != null) result.setReturnValue(owner.hasEffect(effect));
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void arsMisc$getEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> result) {
        var owner = SpellBlockLootOwner.effectOwner((LivingEntity) (Object) this);
        if (owner != null) {
            var instance = owner.getEffect(effect);
            result.setReturnValue(instance == null ? null : new MobEffectInstance(instance));
        }
    }

    @Inject(method = "getActiveEffects", at = @At("HEAD"), cancellable = true)
    private void arsMisc$getEffects(CallbackInfoReturnable<Collection<MobEffectInstance>> result) {
        var owner = SpellBlockLootOwner.effectOwner((LivingEntity) (Object) this);
        if (owner != null) result.setReturnValue(owner.getActiveEffects().stream().map(MobEffectInstance::new)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new)));
    }

    @Inject(method = "getActiveEffectsMap", at = @At("HEAD"), cancellable = true)
    private void arsMisc$getEffectsMap(CallbackInfoReturnable<Map<Holder<MobEffect>, MobEffectInstance>> result) {
        var owner = SpellBlockLootOwner.effectOwner((LivingEntity) (Object) this);
        if (owner != null) {
            var effects = new java.util.HashMap<Holder<MobEffect>, MobEffectInstance>();
            owner.getActiveEffectsMap().forEach((type, instance) -> effects.put(type, new MobEffectInstance(instance)));
            result.setReturnValue(effects);
        }
    }
}
