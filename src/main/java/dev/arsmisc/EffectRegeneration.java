package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Set;
import java.util.Map;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class EffectRegeneration extends AbstractEffect implements IPotionEffect {
    public static final EffectRegeneration INSTANCE = new EffectRegeneration();
    private EffectRegeneration() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_regeneration"), "Regeneration");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level world, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!world.isClientSide && hit.getEntity() instanceof LivingEntity living && living.isAlive()) {
            int amplifier = Math.clamp(stats.getBuffCount(AugmentAmplify.INSTANCE), 0,
                    caster != null && PaladinFocusEvents.equipped(caster) ? 2 : 1);
            int duration = Math.max(1, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
            living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier));
        }
    }
    @Override public SpellTier defaultTier() { return SpellTier.TWO; }
    @Override public int getDefaultManaCost() { return 50; }
    @Override public Set<AbstractAugment> getCompatibleAugments() {
        return setOf(AugmentAmplify.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }
    @Override public int getAugmentLimit(ResourceLocation augment) {
        int configured = super.getAugmentLimit(augment);
        return augment.equals(AugmentAmplify.INSTANCE.getRegistryName()) ? Math.min(1, configured) : configured;
    }
    @Override protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> limits) {
        limits.put(AugmentAmplify.INSTANCE.getRegistryName(), 1);
    }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.ABJURATION); }
    @Override public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 20);
        addExtendTimeConfig(builder, 5);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 20 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 5 : EXTEND_TIME.get(); }
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        super.addAugmentDescriptions(descriptions);
        descriptions.put(AugmentAmplify.INSTANCE, "Raises the level of Regeneration. Focus of Paladin can raise it further.");
        descriptions.put(AugmentExtendTime.INSTANCE, "Extends the duration of Regeneration.");
        descriptions.put(AugmentDurationDown.INSTANCE, "Reduces the duration of Regeneration.");
    }
    @Override public String getBookDescription() {
        return "Grants Regeneration to the target.";
    }
}
