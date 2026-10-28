package dev.arsmisc;

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

public final class EffectCrystallization extends AbstractEffect implements IDamageEffect, IPotionEffect {
    public static final EffectCrystallization INSTANCE = new EffectCrystallization();
    private EffectCrystallization() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_crystallization"), "Crystallization");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        var entity = hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : hit.getEntity();
        if (level.isClientSide || caster == null || !(entity instanceof LivingEntity target) || !target.isAlive()
                || !canDamage(caster, stats, context, resolver, target)) return;
        int amplify = Math.clamp(stats.getBuffCount(AugmentAmplify.INSTANCE), 0, 3);
        if (stats.hasBuff(AugmentExtendTime.INSTANCE) || stats.hasBuff(AugmentDurationDown.INSTANCE)) {
            int duration = Math.max(1, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
            target.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTALLIZATION, duration, Math.min(amplify, 2)), caster);
            return;
        }
        float damage = (float) ((DAMAGE == null ? 5 : DAMAGE.get())
                + (AMP_VALUE == null ? 2 : AMP_VALUE.get()) * stats.getAmpMultiplier());
        var source = buildDamageSource(level, caster);
        CrystallizationDrops.attempt(target, source, amplify + 1,
                () -> attemptDamage(level, caster, stats, context, resolver, target, source, damage));
    }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override public int getDefaultManaCost() { return 15; }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.ABJURATION); }
    @Override public Set<AbstractAugment> getCompatibleAugments() {
        return setOf(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
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
        addDamageConfig(builder, 5); addAmpConfig(builder, 2);
        addPotionConfig(builder, 10); addExtendTimeConfig(builder, 5);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 10 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 5 : EXTEND_TIME.get(); }
    @Override public String getBookDescription() {
        return "Deals spell damage and grants amethyst shards on a kill. Duration augments replace damage with Crystallization, reducing general spell resistance.";
    }
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        super.addAugmentDescriptions(descriptions);
        descriptions.put(AugmentAmplify.INSTANCE, "Increases damage and amethyst drops. In status mode, raises the level of Crystallization.");
        descriptions.put(AugmentDampen.INSTANCE, "Reduces damage.");
        descriptions.put(AugmentExtendTime.INSTANCE, "Replaces damage with Crystallization and extends its duration.");
        descriptions.put(AugmentDurationDown.INSTANCE, "Replaces damage with Crystallization and shortens its duration.");
    }
}
