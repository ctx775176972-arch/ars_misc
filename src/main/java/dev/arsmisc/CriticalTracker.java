package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.event.SpellDamageEvent;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import net.minecraft.world.entity.Entity;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Bridges Sauce's existing roll to Ars's successful-damage event, without rolling again. */
public final class CriticalTracker {
    private static final Map<SpellContext, Map<Entity, Boolean>> PENDING = new WeakHashMap<>();
    private CriticalTracker() {}

    public static synchronized void begin(SpellDamageEvent.Pre event) {
        Map<Entity, Boolean> targets = PENDING.get(event.context);
        if (targets != null) {
            targets.remove(event.target);
            if (targets.isEmpty()) PENDING.remove(event.context);
        }
    }

    public static synchronized void mark(SpellDamageEvent.Pre event) {
        PENDING.computeIfAbsent(event.context, key -> new IdentityHashMap<>()).put(event.target, true);
    }

    public static synchronized boolean consume(SpellDamageEvent.Post event) {
        Map<Entity, Boolean> targets = PENDING.get(event.context);
        if (targets == null) return false;
        boolean critical = Boolean.TRUE.equals(targets.remove(event.target));
        if (targets.isEmpty()) PENDING.remove(event.context);
        return critical;
    }
}
