package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import top.theillusivec4.curios.api.CuriosApi;

/** Native Ars casting with an explicit equipped tool, without replacing either hand. */
public final class EquippedSpellbook {
    private EquippedSpellbook() {}

    public static ItemStack find(LivingEntity entity) {
        if (entity == null) return ItemStack.EMPTY;
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(inventory -> inventory.findFirstCurio(stack -> stack.getItem() instanceof SpellBook))
                .map(top.theillusivec4.curios.api.SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    public static void select(ServerPlayer player, int action, int slot) {
        ItemStack book = find(player);
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (caster == null) return;
        if (action == 1) caster.setNextSlot().saveToStack(book);
        else if (action == 2) caster.setPreviousSlot().saveToStack(book);
        else if (action == 3 && slot >= 0 && slot < caster.getMaxSlots()) caster.setCurrentSlot(slot).saveToStack(book);
    }

    public static void cast(ServerPlayer player, float pitch, float yaw) {
        if (!player.isAlive() || player.isSpectator() || !Float.isFinite(pitch) || !Float.isFinite(yaw)) return;
        ItemStack book = find(player);
        AbstractCaster<?> caster = SpellCasterRegistry.from(book);
        if (caster == null) return;
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        if (data.contains("ars_misc:last_book_cast") && now >= data.getLong("ars_misc:last_book_cast")
                && now - data.getLong("ars_misc:last_book_cast") < 4) return;
        data.putLong("ars_misc:last_book_cast", now);
        float oldPitch = player.getXRot(), oldYaw = player.getYRot(), oldHead = player.getYHeadRot();
        try {
            player.setXRot(net.minecraft.util.Mth.clamp(pitch, -90, 90));
            player.setYRot(yaw);
            player.setYHeadRot(yaw);
            if (EquippedBookScribing.tryScribe(player, book)) return;
            Spell spell = caster.getSpell(player.serverLevel(), player, InteractionHand.MAIN_HAND, caster);
            spell = caster.modifySpellBeforeCasting(player.serverLevel(), player, InteractionHand.MAIN_HAND, spell);
            if (!spell.isValid()) return;
            SpellContext context = new SpellContext(player.level(), spell, player, new PlayerCaster(player), book);
            SpellResolver resolver = caster.getSpellResolver(context, player.level(), player, InteractionHand.MAIN_HAND);
            boolean sensitive = resolver.spell.getBuffsAtIndex(0, player, AugmentSensitive.INSTANCE) > 0;
            HitResult hit = resolver.spell.getCastMethod() instanceof MethodTouch
                    ? TouchTargeting.trace(player, 1, sensitive)
                    : SpellUtil.rayTrace(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 0.5, 1, sensitive);
            if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity) {
                resolver.onCastOnEntity(book, entity.getEntity(), InteractionHand.MAIN_HAND);
            } else if (hit instanceof BlockHitResult block && (hit.getType() == HitResult.Type.BLOCK || sensitive)) {
                resolver.onCastOnBlock(new EquippedUseContext(player, book, block));
            } else resolver.onCast(book, player.level());
        } finally {
            player.setXRot(oldPitch);
            player.setYRot(oldYaw);
            player.setYHeadRot(oldHead);
        }
    }

    private static final class EquippedUseContext extends UseOnContext {
        EquippedUseContext(ServerPlayer player, ItemStack book, BlockHitResult hit) {
            super(player.level(), player, InteractionHand.MAIN_HAND, book, hit);
        }
    }
}
