package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;

/** Uses native validation, events, filters and mana without executing a cast form. */
public final class FieldSpellResolver extends SpellResolver {
    public FieldSpellResolver(SpellContext context) { super(context); }

    @Override public boolean canCast(LivingEntity caster) {
        // A virtual form satisfies the native structural validator only. It is never
        // stored, executed or included in the actual spell's mana cost.
        var validationParts=new java.util.ArrayList<AbstractSpellPart>(spell.unsafeList());
        validationParts.addFirst(MethodTouch.INSTANCE);
        return !spell.isEmpty() && ArsNouveauAPI.getInstance().getSpellCastingSpellValidator()
                .validate(validationParts).isEmpty() && enoughMana(caster);
    }

    public boolean castOnTarget(LivingEntity caster, LivingEntity target) {
        if (caster.level().isClientSide || !canCast(caster) || postEvent().isCanceled()) return false;
        onResolveEffect(caster.level(),new EntityHitResult(target));
        expendMana();
        return true;
    }
}
