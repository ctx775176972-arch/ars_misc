package dev.arsmisc;

import dev.arsmisc.item.CombatWandItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class CombatWandEvents {
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof CombatWandItem) event.setCanceled(true);
    }
    @SubscribeEvent public static void mine(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof CombatWandItem) event.setCanceled(true);
    }
}
