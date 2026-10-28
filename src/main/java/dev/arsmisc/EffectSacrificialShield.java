package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.Map;
import java.util.Set;

public final class EffectSacrificialShield extends AbstractEffect implements IPotionEffect {
    public static final EffectSacrificialShield INSTANCE = new EffectSacrificialShield();
    public static final ResourceKey<DamageType> SACRIFICE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "sacrifice"));
    private EffectSacrificialShield() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_sacrificial_shield"), "Sacrificial Shield");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level world, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (world.isClientSide || !caster.isAlive() || !(hit.getEntity() instanceof LivingEntity target) || !target.isAlive()) return;
        int level = 2 + 2 * Math.clamp(stats.getBuffCount(AugmentAmplify.INSTANCE), 0, 4);
        // The caster pays real health; armor, resistance and absorption cannot pay this cost.
        caster.setHealth(caster.getHealth() - level);
        if (caster.getHealth() <= 0) {
            var deathSource = new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SACRIFICE));
            if (!((dev.arsmisc.mixin.LivingTotemInvoker) caster).arsMisc$tryTotem(deathSource)) {
                caster.die(deathSource);
                return;
            }
        }
        int duration = Math.max(1, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
        // Replace the existing vanilla effect so repeated casts refill rather than stack or keep a hidden effect.
        target.removeEffect(MobEffects.ABSORPTION);
        if (target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, level - 1))) {
            target.setAbsorptionAmount(level * 4.0f);
        }
    }
    @Override public SpellTier defaultTier() { return SpellTier.TWO; }
    @Override public int getDefaultManaCost() { return 100; }
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
        addPotionConfig(builder, 60);
        addExtendTimeConfig(builder, 15);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 60 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 15 : EXTEND_TIME.get(); }
    @Override public String getBookDescription() {
        return "Grants Absorption to the target at the cost of the caster's health. Warning: Amplify increases the health cost, which can kill the caster!";
    }
}
