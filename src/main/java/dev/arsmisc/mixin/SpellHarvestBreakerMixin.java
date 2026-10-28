package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.util.BlockUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlockUtil.class, remap = false)
public abstract class SpellHarvestBreakerMixin {
    @WrapOperation(method = "breakExtraBlock", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/util/FakePlayer;getInventory()Lnet/minecraft/world/entity/player/Inventory;"))
    private static Inventory arsMisc$bindBreaker(FakePlayer breaker, Operation<Inventory> original) {
        SpellBlockLootOwner.bindBreaker(breaker);
        return original.call(breaker);
    }
}
