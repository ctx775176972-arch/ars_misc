package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.arsmisc.EffectAvadaKedavra;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class AvadaKedavraTotemMixin {
    @WrapOperation(method = "checkTotemDeathProtection", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean arsMisc$allowExecutionTotem(DamageSource source, TagKey<DamageType> tag, Operation<Boolean> original) {
        // Bypass creative/invulnerable damage checks while deliberately retaining totem protection.
        if (tag.equals(DamageTypeTags.BYPASSES_INVULNERABILITY) && source.is(EffectAvadaKedavra.DAMAGE_TYPE)) return false;
        return original.call(source, tag);
    }
}
