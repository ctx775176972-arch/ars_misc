package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.event.ScribesTableInteractEvent;
import com.hollingsworth.arsnouveau.api.item.IScribeable;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;

/** A scoped source for native scribeable items; neither equipment slot is modified. */
public final class EquippedBookScribing {
    private record Source(ServerPlayer player, ItemStack book) {}
    private static final ThreadLocal<Source> SOURCE = new ThreadLocal<>();

    private EquippedBookScribing() {}

    public static ItemStack sourceFor(LivingEntity entity, InteractionHand hand, ItemStack held) {
        Source source = SOURCE.get();
        return source != null && source.player == entity && hand == InteractionHand.MAIN_HAND ? source.book : held;
    }

    public static boolean activeFor(LivingEntity entity) {
        Source source = SOURCE.get();
        return source != null && source.player == entity;
    }

    /** True also means a rejected scribing attempt: never cast into an occupied table. */
    public static boolean tryScribe(ServerPlayer player, ItemStack book) {
        var hit = player.pick(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), 1, false);
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK) return false;
        var level = player.serverLevel();
        if (!(level.getBlockEntity(block.getBlockPos()) instanceof ScribesTile clicked)) return false;
        var tile = clicked.getLogicTile();
        if (tile == null || tile.getStack().isEmpty() || !(tile.getStack().getItem() instanceof IScribeable scribeable)) return false;
        var pos = tile.getBlockPos();
        if (!level.mayInteract(player, block.getBlockPos()) || !level.mayInteract(player, pos)) return true;
        Source previous = SOURCE.get();
        SOURCE.set(new Source(player, book));
        try {
            var interaction = CommonHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, block.getBlockPos(), block);
            if (interaction.isCanceled() || interaction.getUseBlock() == TriState.FALSE
                    || interaction.getUseItem() == TriState.FALSE) return true;
            var event = NeoForge.EVENT_BUS.post(new ScribesTableInteractEvent(level, pos, tile, player, InteractionHand.MAIN_HAND, book));
            if (event.isCanceled()) return true;
            scribeable.onScribe(level, pos, player, InteractionHand.MAIN_HAND, tile.getStack());
            tile.setChanged();
            level.sendBlockUpdated(pos, tile.getBlockState(), tile.getBlockState(), 2);
            return true;
        } finally {
            if (previous == null) SOURCE.remove();
            else SOURCE.set(previous);
        }
    }
}
