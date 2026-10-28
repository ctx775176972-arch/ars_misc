package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.arsmisc.PaladinFocusEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Collection;

/** Filters only the native potion loop, preserving dispel events, cures, tags and entity callbacks. */
@Mixin(EffectDispel.class)
public abstract class PaladinDispelMixin {
    @ModifyExpressionValue(method = "onResolveEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getActiveEffects()Ljava/util/Collection;"))
    private Collection<MobEffectInstance> arsMisc$selectEffects(Collection<MobEffectInstance> effects,
            EntityHitResult hit, Level level, LivingEntity caster, SpellStats stats,
            SpellContext context, SpellResolver resolver) {
        return PaladinFocusEvents.dispellableEffects(caster, (LivingEntity) hit.getEntity(), effects);
    }
}
