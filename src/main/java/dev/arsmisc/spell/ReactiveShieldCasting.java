package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.api.util.SpellUtil;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive;
import com.hollingsworth.arsnouveau.setup.registry.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.ItemAbilities;
import java.util.ArrayDeque;
import java.util.function.BooleanSupplier;

/** Records committed shield blocks and casts only after their damage call returns. */
public final class ReactiveShieldCasting {
    public static final int COOLDOWN_TICKS = 10;
    private static final ThreadLocal<ArrayDeque<Frame>> FRAMES = new ThreadLocal<>();
    private static final class Frame {
        final ServerPlayer player;
        ItemStack shield = ItemStack.EMPTY;
        InteractionHand hand;
        Frame(ServerPlayer player) { this.player = player; }
    }
    public static boolean isShield(ItemStack stack) {
        return !stack.isEmpty() && (stack.getItem() instanceof ShieldItem || stack.canPerformAction(ItemAbilities.SHIELD_BLOCK));
    }
    public static boolean duringDamage(LivingEntity entity, BooleanSupplier damage) {
        if (!(entity instanceof ServerPlayer player) || !isShield(player.getUseItem())) return damage.getAsBoolean();
        var frames = FRAMES.get();
        if (frames == null) { frames = new ArrayDeque<>(); FRAMES.set(frames); }
        var frame = new Frame(player); frames.push(frame);
        boolean result;
        try { result = damage.getAsBoolean(); }
        finally { frames.pop(); if (frames.isEmpty()) FRAMES.remove(); }
        if (!frame.shield.isEmpty() && player.isAlive() && !player.isSpectator()) trigger(player, frame.shield, frame.hand);
        return result;
    }
    public static void recordBlock(LivingEntity entity, float blocked) {
        var frames = FRAMES.get();
        if (!(blocked > 0) || frames == null || frames.isEmpty()) return;
        var frame = frames.peek();
        if (frame.player != entity || !isShield(entity.getUseItem())) return;
        frame.shield = entity.getUseItem().copyWithCount(1);
        frame.hand = entity.getUsedItemHand();
    }
    private static void trigger(ServerPlayer player, ItemStack shield, InteractionHand hand) {
        int level = shield.getEnchantmentLevel(player.level().holderOrThrow(EnchantmentRegistry.REACTIVE_ENCHANTMENT));
        var caster = shield.get(DataComponentRegistry.REACTIVE_CASTER.get());
        if (level <= 0 || caster == null || !caster.getSpell().isValid()) return;
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        String cooldown = "ars_misc:last_reactive_shield";
        if (data.contains(cooldown) && now >= data.getLong(cooldown) && now - data.getLong(cooldown) < COOLDOWN_TICKS) return;
        data.putLong(cooldown, now);
        if (player.getRandom().nextDouble() >= Math.min(1, level * 0.25)) return;
        Spell spell = caster.getSpell(player.level(), player, hand, caster);
        spell = caster.modifySpellBeforeCasting(player.serverLevel(), player, hand, spell);
        if (!spell.isValid()) return;
        var context = new SpellContext(player.level(), spell, player, new PlayerCaster(player), shield);
        var resolver = caster.getSpellResolver(context, player.level(), player, hand);
        boolean sensitive = spell.getBuffsAtIndex(0, player, AugmentSensitive.INSTANCE) > 0;
        var hit = SpellUtil.rayTrace(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 0.5, 1, sensitive);
        if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity) {
            resolver.onCastOnEntity(shield, entity.getEntity(), hand);
        } else if (hit instanceof BlockHitResult block && (hit.getType() == HitResult.Type.BLOCK || sensitive)) {
            resolver.onCastOnBlock(new ShieldUseContext(player, shield, hand, block));
        } else resolver.onCast(shield, player.level());
    }
    private static final class ShieldUseContext extends UseOnContext {
        ShieldUseContext(ServerPlayer player, ItemStack shield, InteractionHand hand, BlockHitResult hit) {
            super(player.level(), player, hand, shield, hit);
        }
    }
}
