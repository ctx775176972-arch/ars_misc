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
            int amplifier = Math.clamp(stats.getBuffCount(AugmentAmplify.INSTANCE), 0, 4);
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
        return augment.equals(AugmentAmplify.INSTANCE.getRegistryName()) ? Math.min(4, configured) : configured;
    }
    @Override protected void addDefaultAugmentLimits(Map<ResourceLocation, Integer> limits) {
        limits.put(AugmentAmplify.INSTANCE.getRegistryName(), 4);
    }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.ABJURATION); }
    @Override public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 10);
        addExtendTimeConfig(builder, 5);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 10 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 5 : EXTEND_TIME.get(); }
    @Override public String getBookDescription() {
        return "Grants vanilla Regeneration I for 10 seconds. Each Amplify increases the level by one, up to Regeneration V. Extend Time and Duration Down modify duration. Healing can be reversed by Decay.";
    }
}
