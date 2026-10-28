package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsmisc.spell.TouchTargeting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractCaster.class, remap = false)
public abstract class PlayerTouchTargetingMixin {
    @WrapOperation(method = "castSpell(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/network/chat/Component;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lnet/minecraft/world/InteractionResultHolder;",
            at = @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/util/SpellUtil;rayTrace(Lnet/minecraft/world/entity/Entity;DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private HitResult arsMisc$playerTouchRay(Entity viewer, double distance, float partialTick, boolean sensitive,
            Operation<HitResult> original, Level level, LivingEntity caster, InteractionHand hand,
            Component message, Spell spell) {
        if (viewer instanceof Player player && spell.getCastMethod() instanceof MethodTouch) {
            return TouchTargeting.trace(player, partialTick, sensitive);
        }
        return original.call(viewer, distance, partialTick, sensitive);
    }
}
