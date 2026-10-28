package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Map;
import java.util.Set;

public final class EffectCrystalShield extends AbstractEffect implements IPotionEffect {
    public static final EffectCrystalShield INSTANCE = new EffectCrystalShield();

    private EffectCrystalShield() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_crystal_shield"), "Crystal Shield");
    }

    @Override public void onResolveEntity(EntityHitResult hit, Level world, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (world.isClientSide || !(hit.getEntity() instanceof LivingEntity target) || !target.isAlive()) return;
        int amplifier = Math.clamp(stats.getBuffCount(AugmentAmplify.INSTANCE), 0, 3);
        int duration = Math.max(1, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
        var existing = target.getEffect(ArsMiscMod.CRYSTAL_SHIELD);
        if (existing != null && existing.getAmplifier() > amplifier) return;
        if (target.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTAL_SHIELD, duration, amplifier), caster)) {
            var active = target.getEffect(ArsMiscMod.CRYSTAL_SHIELD);
            if (active != null && active.getAmplifier() == amplifier) {
                double power = caster.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS);
                target.getPersistentData().putDouble(CrystalShieldEffect.POWER_KEY, Double.isFinite(power) ? power : 0);
            }
        }
        CrystalShieldEvents.refreshMovement(target);
    }

    @Override public SpellTier defaultTier() { return SpellTier.TWO; }
    @Override public int getDefaultManaCost() { return 100; }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.ABJURATION); }
    @Override public Set<AbstractAugment> getCompatibleAugments() {
        return setOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }
    @Override public int getAugmentLimit(ResourceLocation augment) {
        int configured = super.getAugmentLimit(augment);
        return augment.equals(AugmentAmplify.INSTANCE.getRegistryName()) ? Math.min(3, configured) : configured;
    }
    @Override protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> limits) {
        limits.put(AugmentAmplify.INSTANCE.getRegistryName(), 3);
    }
    @Override public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 30);
        addExtendTimeConfig(builder, 10);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 30 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 10 : EXTEND_TIME.get(); }
    @Override public String getBookDescription() {
        return "Grants Crystal Shield, reducing incoming damage and slowing movement. Focus of Paladin strengthens the shield and removes its movement penalty.";
    }
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        super.addAugmentDescriptions(descriptions);
        descriptions.put(AugmentAmplify.INSTANCE, "Raises the level of Crystal Shield.");
        descriptions.put(AugmentExtendTime.INSTANCE, "Extends the duration of Crystal Shield.");
    }
}
