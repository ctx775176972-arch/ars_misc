package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Set;

public final class EffectSoulHarvest extends AbstractEffect implements IDamageEffect {
    public static final EffectSoulHarvest INSTANCE = new EffectSoulHarvest();
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "soul_harvest"));

    private EffectSoulHarvest() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_soul_harvest"), "Soul Harvest");
    }

    @Override public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        var entity = hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : hit.getEntity();
        if (level.isClientSide || caster == null || !(entity instanceof LivingEntity target) || !target.isAlive()) return;
        // The native damage pipeline adds SPELL_DAMAGE_BONUS once; a zero base gives exactly spell power.
        SoulHarvestGrowth.attempt(caster, target, () -> attemptDamage(level, caster, stats, context,
                resolver, entity, buildDamageSource(level, caster), 0));
    }

    @Override public DamageSource buildDamageSource(Level level, LivingEntity caster) {
        return DamageUtil.source(level, DAMAGE_TYPE, caster);
    }
    @Override public SpellTier defaultTier() { return SpellTier.TWO; }
    @Override public int getDefaultManaCost() { return 50; }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.NECROMANCY); }
    @Override public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
    @Override public String getBookDescription() {
        return "Deals spell damage. Killing a target permanently increases maximum mana and mana regeneration. Growth survives death.";
    }
}
