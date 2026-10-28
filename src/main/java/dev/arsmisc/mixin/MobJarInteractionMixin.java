package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.block.MobJar;
import com.hollingsworth.arsnouveau.common.block.tile.MobJarTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

@Mixin(value = MobJar.class, remap = false)
public abstract class MobJarInteractionMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult arsMisc$rememberOpenedMenu(Player player, Entity entity, InteractionHand hand,
            Operation<InteractionResult> original, @Share("openedMenu") LocalBooleanRef openedMenu) {
        var previous = player.containerMenu;
        var result = original.call(player, entity, hand);
        openedMenu.set(!player.level().isClientSide && player.containerMenu != previous);
        return result;
    }

    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/common/block/tile/MobJarTile;dispatchBehavior(Ljava/util/function/Consumer;)V"))
    private void arsMisc$avoidReopeningMenu(MobJarTile jar, Consumer<?> behavior, Operation<Void> original,
            @Share("openedMenu") LocalBooleanRef openedMenu) {
        // The initial interactOn is only used for entities without a registered jar behavior.
        // Their default behavior would interact again and invalidate the newly opened merchant menu.
        if (!openedMenu.get()) original.call(jar, behavior);
    }
}
