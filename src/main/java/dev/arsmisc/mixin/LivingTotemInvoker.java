package dev.arsmisc.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingTotemInvoker {
    @Invoker("checkTotemDeathProtection")
    boolean arsMisc$tryTotem(DamageSource source);
    @org.spongepowered.asm.mixin.gen.Accessor("dead")
    void arsMisc$setDead(boolean dead);
}
