package dev.arsmisc.item;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.PlayerCaster;
import com.hollingsworth.arsnouveau.common.items.Wand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Full-spell inscription and native wand rendering with a dedicated left-button cast input. */
public final class CombatWandItem extends Wand {
    public static final int CAST_INTERVAL = 4;
    public static final int MANA_DISCOUNT = 20;
    @Override public AbstractCaster<?> getSpellCaster(ItemStack stack) {
        return stack.getOrDefault(com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry.SPELL_CASTER.get(), new SpellCaster());
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public boolean isScribedSpellValid(AbstractCaster<?> caster,Player player,InteractionHand hand,ItemStack stack,Spell spell) {
        return spell.isValid() && com.hollingsworth.arsnouveau.api.ArsNouveauAPI.getInstance()
                .getSpellCastingSpellValidator().validate(spell.unsafeList()).isEmpty();
    }
    @Override public void scribeModifiedSpell(AbstractCaster<?> caster,Player player,InteractionHand hand,ItemStack stack,Spell.Mutable spell) {
        // Keep the complete spell, including its chosen form and explicitly written augments.
    }
    @Override public void sendInvalidMessage(Player player) {
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("item.ars_misc.combat_wand.invalid"),true);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.ars_misc.combat_wand"));
    }
    @Override public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) { return true; }
    @Override public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level,
            net.minecraft.core.BlockPos pos, Player player) { return false; }

    public static void cast(ServerPlayer player, float pitch, float yaw) {
        ItemStack wand = player.getMainHandItem();
        if (!(wand.getItem() instanceof CombatWandItem) || !player.isAlive() || player.isSpectator()
                || !Float.isFinite(pitch) || !Float.isFinite(yaw) || player.getCooldowns().isOnCooldown(wand.getItem())) return;
        AbstractCaster<?> caster = SpellCasterRegistry.from(wand);
        if (caster == null) return;
        long now = player.level().getGameTime();
        var data = player.getPersistentData();
        String key = "ars_misc:last_combat_wand_cast";
        if (data.contains(key) && now >= data.getLong(key) && now - data.getLong(key) < CAST_INTERVAL) return;
        data.putLong(key, now);
        float oldPitch = player.getXRot(), oldYaw = player.getYRot(), oldHead = player.getYHeadRot();
        try {
            player.setXRot(net.minecraft.util.Mth.clamp(pitch, -90, 90));
            player.setYRot(yaw); player.setYHeadRot(yaw);
            Spell spell = caster.getSpell(player.serverLevel(), player, InteractionHand.MAIN_HAND, caster);
            spell = caster.modifySpellBeforeCasting(player.serverLevel(), player, InteractionHand.MAIN_HAND, spell);
            if (!spell.isValid()) return;
            var context = new SpellContext(player.level(), spell, player, new PlayerCaster(player), wand);
            var resolver=caster.getSpellResolver(context, player.level(), player, InteractionHand.MAIN_HAND);
            boolean sensitive=resolver.spell.getBuffsAtIndex(0,player,
                    com.hollingsworth.arsnouveau.common.spell.augment.AugmentSensitive.INSTANCE)>0;
            var hit=resolver.spell.getCastMethod() instanceof com.hollingsworth.arsnouveau.common.spell.method.MethodTouch
                    ? dev.arsmisc.spell.TouchTargeting.trace(player,1,sensitive)
                    : com.hollingsworth.arsnouveau.api.util.SpellUtil.rayTrace(player,
                            player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE)+0.5,1,sensitive);
            if(hit instanceof net.minecraft.world.phys.EntityHitResult entity && entity.getEntity() instanceof net.minecraft.world.entity.LivingEntity)
                resolver.onCastOnEntity(wand,entity.getEntity(),InteractionHand.MAIN_HAND);
            else if(hit instanceof net.minecraft.world.phys.BlockHitResult block
                    && (hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK || sensitive))
                resolver.onCastOnBlock(new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,block));
            else resolver.onCast(wand,player.level());
        } finally {
            player.setXRot(oldPitch); player.setYRot(oldYaw); player.setYHeadRot(oldHead);
        }
    }
}
