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

public final class EffectLootSense extends AbstractEffect implements IPotionEffect {
    public static final EffectLootSense INSTANCE = new EffectLootSense();
    private EffectLootSense() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_loot_sense"), "Loot Sense");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level world, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        if (!world.isClientSide && hit.getEntity() instanceof LivingEntity living)
            applyConfigPotion(living, ArsMiscMod.LOOT_SENSE, stats);
    }
    @Override public SpellTier defaultTier() { return SpellTier.TWO; }
    @Override public int getDefaultManaCost() { return 50; }
    @Override public Set<AbstractAugment> getCompatibleAugments() {
        return setOf(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE);
    }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.ABJURATION); }
    @Override public void buildConfig(ModConfigSpec.Builder builder) {
        super.buildConfig(builder);
        addPotionConfig(builder, 60);
        addExtendTimeConfig(builder, 15);
    }
    @Override public int getBaseDuration() { return POTION_TIME == null ? 60 : POTION_TIME.get(); }
    @Override public int getExtendTimeDuration() { return EXTEND_TIME == null ? 15 : EXTEND_TIME.get(); }
    @Override public void addAugmentDescriptions(java.util.Map<AbstractAugment, String> descriptions) {
        super.addAugmentDescriptions(descriptions);
        descriptions.put(AugmentExtendTime.INSTANCE, "Extends the duration of Loot Sense.");
        descriptions.put(AugmentDurationDown.INSTANCE, "Reduces the duration of Loot Sense.");
    }
    @Override public String getBookDescription() {
        return "Grants awareness of nearby unopened loot containers, revealing their positions through walls.";
    }
}
