package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import java.util.Set;

/** Wither is an application prerequisite; healing reversal remains in the existing effect. */
public final class EffectDecay extends AbstractEffect implements IPotionEffect {
    public static final EffectDecay INSTANCE = new EffectDecay();
    private EffectDecay() { super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID,"glyph_decay"),"Decay"); }
    @Override public void onResolveEntity(EntityHitResult hit, Level level, LivingEntity caster,
            SpellStats stats, SpellContext context, SpellResolver resolver) {
        var entity=hit.getEntity() instanceof net.neoforged.neoforge.entity.PartEntity<?> part ? part.getParent() : hit.getEntity();
        if (!level.isClientSide && entity instanceof LivingEntity target && target.isAlive() && target.hasEffect(MobEffects.WITHER))
            target.addEffect(new MobEffectInstance(ArsMiscMod.DECAY,600,0),caster);
    }
    @Override public SpellTier defaultTier() { return SpellTier.THREE; }
    @Override public int getDefaultManaCost() { return 100; }
    @Override public int getBaseDuration() { return 30; }
    @Override public int getExtendTimeDuration() { return 0; }
    @Override public Set<AbstractAugment> getCompatibleAugments() { return Set.of(); }
    @Override public Set<SpellSchool> getSchools() { return setOf(SpellSchools.NECROMANCY); }
    @Override public String getBookDescription() { return "Inflicts Decay on a target already afflicted with Wither, converting healing received into magic damage."; }
}
