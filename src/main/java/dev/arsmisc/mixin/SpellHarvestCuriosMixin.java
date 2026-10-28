package dev.arsmisc.mixin;

import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosCapability;

/** Resolves direct Curios capability queries as well as CuriosApi lookups during spell harvesting. */
@Mixin(Entity.class)
public abstract class SpellHarvestCuriosMixin {
    @Inject(method = "getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void arsMisc$curios(EntityCapability<?, ?> capability, CallbackInfoReturnable<Object> result) {
        arsMisc$resolveCurios(capability, result);
    }

    @Inject(method = "getCapability(Lnet/neoforged/neoforge/capabilities/EntityCapability;Ljava/lang/Object;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void arsMisc$curiosWithContext(EntityCapability<?, ?> capability, Object context, CallbackInfoReturnable<Object> result) {
        if (context == null) arsMisc$resolveCurios(capability, result);
    }

    @org.spongepowered.asm.mixin.Unique
    private void arsMisc$resolveCurios(EntityCapability<?, ?> capability, CallbackInfoReturnable<Object> result) {
        if (capability != CuriosCapability.INVENTORY) return;
        var owner = SpellBlockLootOwner.effectOwner((Entity) (Object) this);
        if (owner != null) result.setReturnValue(owner.getCapability(CuriosCapability.INVENTORY));
    }
}
