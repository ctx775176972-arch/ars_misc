package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.event.ReactiveEvents;
import dev.arsmisc.spell.ReactiveShieldCasting;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ReactiveEvents.class, remap = false)
public class ReactiveShieldLeftClickMixin {
    @Inject(method = "castSpell", at = @At("HEAD"), cancellable = true)
    private static void shieldUsesBlockTrigger(LivingEntity entity, ItemStack stack, CallbackInfo ci) {
        if (ReactiveShieldCasting.isShield(stack)) ci.cancel();
    }
}
