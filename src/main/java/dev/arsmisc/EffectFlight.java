package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.setup.registry.ModPotions;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentExtendTime;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Set;
import java.util.Map;

/** Grants the native Flight effect, including its native creative-flight attribute. */
public final class EffectFlight extends AbstractEffect implements IPotionEffect {
    public static final EffectFlight INSTANCE = new EffectFlight();
    private EffectFlight() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,"glyph_flight"),"Flight");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level world, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!world.isClientSide && hit.getEntity() instanceof LivingEntity target && target.isAlive()) {
            int duration = Math.max(1, getBaseDuration() * 20 + getExtendTimeDuration() * stats.getDurationInTicks());
            target.addEffect(new MobEffectInstance(ModPotions.FLIGHT_EFFECT,duration,0));
        }
    }
    @Override public SpellTier defaultTier() { return SpellTier.THREE; }
    @Override public int getDefaultManaCost() { return 200; }
    @Override public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 30);
        addExtendTimeConfig(builder, 10);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 30 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 10 : EXTEND_TIME.get(); }
    @Override public Set<AbstractAugment> getCompatibleAugments() {
        return setOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        super.addAugmentDescriptions(descriptions);
        descriptions.put(AugmentExtendTime.INSTANCE, "Extends the duration of Flight.");
        descriptions.put(AugmentDurationDown.INSTANCE, "Reduces the duration of Flight.");
    }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.MANIPULATION); }
    @Override public String getBookDescription() {
        return "Grants Flight to the target.";
    }
}
