package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.item.NetheriteEnchantersSwordItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class SwordSpellDamage {
    private static final String ATTACK="ars_misc:netherite_sword_attack";
    private SwordSpellDamage() {}
    public static void mark(SpellContext context,LivingEntity caster) {
        double attack=caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
        context.tag.putDouble(ATTACK,Double.isFinite(attack) ? Math.max(0,attack) : 0);
    }
    public static boolean applies(SpellContext context) {
        return context!=null && context.getCasterTool().getItem() instanceof NetheriteEnchantersSwordItem
                && context.tag.contains(ATTACK,net.minecraft.nbt.Tag.TAG_DOUBLE);
    }
    public static float attack(SpellContext context) { return (float)Math.min(Float.MAX_VALUE,context.tag.getDouble(ATTACK)); }

    /** Preserve the original spell type, tags, attribution and native Fortune handling. */
    public static DamageSource piercing(DamageSource original) {
        return new DamageUtil.SpellDamageSource(original.typeHolder(),original.getDirectEntity(),original.getEntity(),original.sourcePositionRaw()) {
            @Override public boolean is(TagKey<DamageType> tag) {
                return tag.equals(DamageTypeTags.BYPASSES_ARMOR) || original.is(tag);
            }
        };
    }
    /** Vanilla mobs do not call Item.hurtEnemy; keep direct non-player melee usable as native caster swords are. */
    @SubscribeEvent public static void mobMelee(LivingDamageEvent.Post event) {
        var source=event.getSource();
        if (source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)
                && source.getDirectEntity()==attacker && !attacker.level().isClientSide
                && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))
                && attacker.getMainHandItem().getItem() instanceof NetheriteEnchantersSwordItem)
            NetheriteEnchantersSwordItem.cast(attacker.getMainHandItem(),event.getEntity(),attacker);
    }
}
