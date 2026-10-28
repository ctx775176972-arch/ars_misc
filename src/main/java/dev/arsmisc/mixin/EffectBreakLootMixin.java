package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectBreak;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.UUID;

@Mixin(value=EffectBreak.class,remap=false)
public abstract class EffectBreakLootMixin {
    @WrapOperation(method="onResolveBlock",at=@At(value="INVOKE",target="Lcom/hollingsworth/arsnouveau/api/util/BlockUtil;breakExtraBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;Ljava/util/UUID;Z)Z"))
    private boolean arsMisc$harvestOwner(ServerLevel level, BlockPos pos, ItemStack tool, UUID ownerId,
            boolean skipHarvestCheck, Operation<Boolean> original,
            BlockHitResult hit, Level world, LivingEntity caster, SpellStats stats,
            SpellContext context, SpellResolver resolver) {
        if(caster instanceof Player player && !(player instanceof FakePlayer))
            return SpellBlockLootOwner.duringHarvest(player,level,pos,
                    ()->original.call(level,pos,tool,ownerId,skipHarvestCheck));
        return original.call(level,pos,tool,ownerId,skipHarvestCheck);
    }
}
