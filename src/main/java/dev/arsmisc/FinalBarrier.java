package dev.arsmisc;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.arsmisc.item.FinalBarrierItem;
import dev.arsmisc.mixin.LivingTotemInvoker;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class FinalBarrier {
    public static final String COOLDOWN = "ars_misc:final_barrier_cooldown";
    public static final String NEXT_UPKEEP = "ars_misc:final_barrier_next_upkeep";
    private static final String READY_PENDING = "ars_misc:final_barrier_ready_pending";

    public static long cooldownTicks(Player player) {
        return Math.max(0, player.getPersistentData().getLong(COOLDOWN) - player.level().getGameTime());
    }

    public static boolean active(Player player) {
        if (player.level().isClientSide || player.isSpectator() || player.isRemoved() || cooldownTicks(player) > 0) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> inventory.findCurios(stack -> stack.getItem() instanceof FinalBarrierItem)
                .stream().anyMatch(result -> !result.slotContext().cosmetic()
                        && inventory.isSlotActive(result.slotContext().identifier(), result.slotContext().index())
                        && FinalBarrierItem.enabled(result.stack()))).orElse(false);
    }

    public static boolean requestState(Player player, ItemStack stack, boolean enabled) {
        if (!enabled) { FinalBarrierItem.setEnabled(stack, false); return true; }
        if (cooldownTicks(player) > 0) {
            player.displayClientMessage(Component.translatable("message.ars_misc.final_barrier.cooldown", (cooldownTicks(player) + 19) / 20), true);
            return false;
        }
        var mana = CapabilityRegistry.getMana(player);
        if (mana == null || !Double.isFinite(mana.getCurrentMana()) || mana.getCurrentMana() <= 0) {
            player.displayClientMessage(Component.translatable("message.ars_misc.final_barrier.no_mana"), true);
            return false;
        }
        FinalBarrierItem.setEnabled(stack, true);
        player.getPersistentData().remove(READY_PENDING);
        tick(player);
        return FinalBarrierItem.enabled(stack);
    }

    public static void tick(Player player) {
        if (!player.isAlive()) return;
        if (player.getPersistentData().getBoolean(READY_PENDING) && cooldownTicks(player) == 0
                && AccessoryEvents.isWearing(player, ArsMiscMod.FINAL_BARRIER.get(), "casting_device")) {
            player.getPersistentData().remove(READY_PENDING);
            player.displayClientMessage(Component.translatable("message.ars_misc.final_barrier.ready"), true);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        if (!active(player)) return;
        long now = player.level().getGameTime();
        if (player.getPersistentData().getLong(NEXT_UPKEEP) > now) return;
        player.getPersistentData().putLong(NEXT_UPKEEP, now + 20);
        pay(player, MiscConfig.FINAL_BARRIER_UPKEEP.get());
    }

    /** A committed protection always completes; insufficient or exactly exhausted mana starts the shared cooldown. */
    private static void pay(Player player, double cost) {
        if (cost <= 0) return;
        var mana = CapabilityRegistry.getMana(player);
        double available = mana == null ? 0 : mana.getCurrentMana();
        if (!Double.isFinite(available) || available < 0) available = 0;
        if (mana != null) mana.removeMana(Math.min(available, cost));
        if (available <= cost || !Double.isFinite(cost)) exhaust(player);
    }

    private static void exhaust(Player player) {
        var mana = CapabilityRegistry.getMana(player);
        if (mana != null) mana.setMana(0);
        player.getPersistentData().putLong(COOLDOWN,
                player.level().getGameTime() + MiscConfig.FINAL_BARRIER_COOLDOWN_SECONDS.get() * 20L);
        player.getPersistentData().putBoolean(READY_PENDING, true);
        CuriosApi.getCuriosInventory(player).ifPresent(inventory -> inventory
                .findCurios(stack -> stack.getItem() instanceof FinalBarrierItem)
                .forEach(result -> FinalBarrierItem.setEnabled(result.stack(), false)));
        player.getInventory().setChanged();
        player.displayClientMessage(Component.translatable("message.ars_misc.final_barrier.exhausted"), true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || !active(player)) return;
        float damage = event.getNewDamage();
        if (damage <= 0) return;
        float absorption = Math.max(0, player.getAbsorptionAmount());
        if (Float.isFinite(damage)) {
            float remaining = Math.max(0, damage - absorption);
            if (remaining <= 0) return;
            // Leave only the absorbed portion for vanilla to consume from golden hearts and record in its statistics.
            event.setNewDamage(Math.min(damage, absorption));
            pay(player, remaining * MiscConfig.FINAL_BARRIER_MANA_PER_DAMAGE.get());
        } else {
            event.setNewDamage(absorption);
            pay(player, Double.POSITIVE_INFINITY);
        }
    }

    public static boolean rescue(Player player) {
        if (!active(player)) return false;
        ((LivingTotemInvoker) player).arsMisc$setDead(false);
        player.deathTime = 0;
        player.hurtTime = 0;
        player.setHealth(player.getMaxHealth());
        pay(player, MiscConfig.FINAL_BARRIER_RESCUE_COST.get());
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && rescue(player)) event.setCanceled(true);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        var old = event.getOriginal().getPersistentData();
        if (old.contains(COOLDOWN)) event.getEntity().getPersistentData().putLong(COOLDOWN, old.getLong(COOLDOWN));
        if (old.contains(NEXT_UPKEEP)) event.getEntity().getPersistentData().putLong(NEXT_UPKEEP, old.getLong(NEXT_UPKEEP));
        if (old.contains(READY_PENDING)) event.getEntity().getPersistentData().putBoolean(READY_PENDING, old.getBoolean(READY_PENDING));
    }
    private FinalBarrier() {}
}
