package dev.arsmisc.mixin;

import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(RangedAttribute.class)
public abstract class ManaAttributeLimitMixin {
    @ModifyVariable(method = "<init>(Ljava/lang/String;DDD)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private static double arsMisc$expandManaLimit(double maximum, String description,
            double defaultValue, double minimum, double originalMaximum) {
        // Adjust the native definitions on both logical sides before attribute values are cached.
        return description.equals("ars_nouveau.perk.max_mana") || description.equals("ars_nouveau.perk.mana_regen")
                ? maximum * 10 : maximum;
    }
}
