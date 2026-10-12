package dev.arsmisc.mixin;

import com.alexthw.sauce.event.AttributeEventHandler;
import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import dev.arsmisc.CriticalTracker;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AttributeEventHandler.class, remap = false)
public abstract class SauceCriticalMixin {
    @Inject(method = "critChanceAndDamageMultiplier", at = @At("HEAD"))
    private static void arsMisc$begin(SpellDamageEvent.Pre event, CallbackInfo ci) {
        CriticalTracker.begin(event);
    }

    @Redirect(method = "critChanceAndDamageMultiplier", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/RandomSource;nextDouble()D"), require = 1)
    private static double arsMisc$observeRoll(RandomSource random, SpellDamageEvent.Pre event) {
        double roll = random.nextDouble();
        if (roll < event.caster.getAttributeValue(ModRegistry.SPELL_CRIT)) CriticalTracker.mark(event);
        return roll;
    }
}
