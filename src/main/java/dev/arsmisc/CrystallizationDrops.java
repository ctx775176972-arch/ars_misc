package dev.arsmisc;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import java.util.function.BooleanSupplier;

/** Rewards only the direct glyph hit, through the normal, cancellable death-drop event. */
@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class CrystallizationDrops {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    private static final class Attempt {
        final LivingEntity target;
        final DamageSource source;
        final int count;
        boolean rewarded;
        Attempt(LivingEntity target, DamageSource source, int count) {
            this.target = target; this.source = source; this.count = Math.clamp(count, 1, 4);
        }
    }
    public static void attempt(LivingEntity target, DamageSource source, int count, BooleanSupplier damage) {
        var previous = CURRENT.get();
        CURRENT.set(new Attempt(target, source, count));
        try { damage.getAsBoolean(); }
        finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        var current = CURRENT.get();
        if (current == null || current.rewarded || event.getEntity() != current.target
                || event.getSource().getEntity() != current.source.getEntity()
                || !event.getSource().typeHolder().equals(current.source.typeHolder())
                || !event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
        current.rewarded = true;
        var target = event.getEntity();
        event.getDrops().add(new ItemEntity(target.level(), target.getX(), target.getY(), target.getZ(),
                new ItemStack(Items.AMETHYST_SHARD, current.count)));
    }
    private CrystallizationDrops() {}
}
