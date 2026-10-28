package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsmisc.spell.SwordSpellDamage;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IDamageEffect.class)
public interface NetheriteSwordSpellMixin {
    @WrapMethod(method="attemptDamage")
    private boolean arsMisc$swordDamage(Level level,LivingEntity caster,SpellStats stats,SpellContext context,
            SpellResolver resolver,Entity target,DamageSource source,float baseDamage,Operation<Boolean> original) {
        if (!level.isClientSide && SwordSpellDamage.applies(context)) {
            baseDamage=(float)Math.min(Float.MAX_VALUE,(double)baseDamage+SwordSpellDamage.attack(context));
            source=SwordSpellDamage.piercing(source);
        }
        return original.call(level,caster,stats,context,resolver,target,source,baseDamage);
    }
}
