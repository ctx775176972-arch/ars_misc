package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class SoulHarvestGrowth {
    private static final String DATA = "ars_misc:soul_harvest";
    private static final ResourceLocation LEGACY_HEALTH = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "soul_harvest_health");
    private static final ResourceLocation MANA = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "soul_harvest_mana");
    private static final ResourceLocation REGEN = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "soul_harvest_regen");
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();

    private static final class Attempt {
        final LivingEntity target;
        LivingDeathEvent death;
        final java.util.Set<LivingEntity> rewarded;
        Attempt(LivingEntity target, Attempt previous) {
            this.target = target;
            rewarded = previous == null ? java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>()) : previous.rewarded;
        }
    }

    public static void attempt(LivingEntity caster, LivingEntity target, BooleanSupplier damage) {
        var previous = CURRENT.get();
        var current = new Attempt(target, previous);
        CURRENT.set(current);
        try {
            boolean applied = damage.getAsBoolean();
            // Inspect the final event after all death listeners and the native hurt call have returned.
            if (applied && current.death != null && !current.death.isCanceled()
                    && target.isDeadOrDying() && current.rewarded.add(target)) {
                var data = caster.getPersistentData().getCompound(DATA).copy();
                data.putDouble("mana", Math.min(Double.MAX_VALUE, manaBonus(caster) + MiscConfig.SOUL_HARVEST_MANA_PER_KILL.get()));
                data.putDouble("regen", Math.min(Double.MAX_VALUE, regenBonus(caster) + MiscConfig.SOUL_HARVEST_REGEN_PER_KILL.get()));
                caster.getPersistentData().put(DATA, data);
                restore(caster);
            }
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void observeDeath(LivingDeathEvent event) {
        var current = CURRENT.get();
        if (current != null && event.getEntity() == current.target) current.death = event;
    }

    public static double manaBonus(LivingEntity entity) { return bonus(entity, "mana"); }
    public static double regenBonus(LivingEntity entity) { return bonus(entity, "regen"); }
    private static double bonus(LivingEntity entity, String key) {
        double value = entity.getPersistentData().getCompound(DATA).getDouble(key);
        return Double.isFinite(value) && value > 0 ? value : 0;
    }
    public static void restore(LivingEntity entity) {
        // Retire only this feature's former health growth; retain mana/regen and unrelated health sources.
        var health=entity.getAttribute(Attributes.MAX_HEALTH);
        if (health!=null && health.hasModifier(LEGACY_HEALTH)) {
            health.removeModifier(LEGACY_HEALTH);
            if (entity.getHealth()>entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
        }
        var data=entity.getPersistentData().getCompound(DATA);
        if (data.contains("health")) data.remove("health");
        apply(entity, PerkAttributes.MAX_MANA, MANA, manaBonus(entity));
        apply(entity, PerkAttributes.MANA_REGEN_BONUS, REGEN, regenBonus(entity));
    }
    private static void apply(LivingEntity entity, Holder<Attribute> type, ResourceLocation id, double amount) {
        var attribute = entity.getAttribute(type);
        if (attribute == null) return;
        if (amount > 0) attribute.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        else attribute.removeModifier(id);
    }

    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        CompoundTag data = event.getOriginal().getPersistentData();
        if (data.contains(DATA)) event.getEntity().getPersistentData().put(DATA, data.getCompound(DATA).copy());
        restore(event.getEntity());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { restore(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { restore(event.getEntity()); }
    private SoulHarvestGrowth() {}
}
