package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arsmisc.compat.SpellBlockLootOwner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.List;

@Mixin(Block.class)
public abstract class SpellBlockLootMixin {
    @WrapOperation(method="getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;getDrops(Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;"))
    private static List<ItemStack> arsMisc$resolveOwner(BlockState receiver, LootParams.Builder builder,
            Operation<List<ItemStack>> original, BlockState state, ServerLevel level, BlockPos pos,
            BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        var player=SpellBlockLootOwner.ownerFor(level,pos,breaker);
        if(player!=null) builder.withOptionalParameter(LootContextParams.THIS_ENTITY,player);
        return original.call(receiver,builder);
    }
}
