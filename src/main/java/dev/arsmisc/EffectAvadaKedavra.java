package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Set;

public final class EffectAvadaKedavra extends AbstractEffect implements IDamageEffect {
    public static final EffectAvadaKedavra INSTANCE = new EffectAvadaKedavra();
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "avada_kedavra"));
    private EffectAvadaKedavra() {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "glyph_avada_kedavra"), "Avada Kedavra");
    }
    @Override public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        var entity = hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : hit.getEntity();
        if (!level.isClientSide && caster != null && entity instanceof LivingEntity target && target.isAlive())
            AvadaKedavraExecution.execute(caster, target, buildDamageSource(level, caster));
    }
    @Override public DamageSource buildDamageSource(Level level, LivingEntity caster) {
        return DamageUtil.source(level, DAMAGE_TYPE, caster);
    }
    @Override public SpellTier defaultTier() { return SpellTier.THREE; }
    @Override public int getDefaultManaCost() { return 10000; }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.NECROMANCY); }
    @Override public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
    @Override public String getBookDescription() {
        return "Deals overwhelming spell damage and attempts to execute surviving targets. Totems and death protection can prevent execution.";
    }
}
