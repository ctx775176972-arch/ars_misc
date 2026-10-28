package dev.arsmisc.client;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.item.CombatWandItem;
import dev.arsmisc.network.CombatWandPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, value = Dist.CLIENT)
public final class CombatWandInput {
    private static int cooldown;
    private static boolean holdingWand() {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof CombatWandItem;
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && holdingWand()) {
            event.setCanceled(true); event.setSwingHand(false);
            var mc = Minecraft.getInstance();
            if (cooldown == 0 && mc.screen == null && mc.player.isAlive() && !mc.player.isSpectator()) sendCast();
        }
    }
    private static void sendCast() {
        var mc = Minecraft.getInstance();
        var camera = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        PacketDistributor.sendToServer(new CombatWandPayload(camera.getXRot(), camera.getYRot()));
        cooldown = CombatWandItem.CAST_INTERVAL;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (cooldown > 0) cooldown--;
        var mc = Minecraft.getInstance();
        if (mc.screen != null || !holdingWand() || !mc.player.isAlive() || mc.player.isSpectator()) return;
        // Read the attack binding directly: vanilla suppresses ordinary attacks while blocking.
        if (mc.options.keyAttack.isDown() && cooldown == 0) {
            sendCast();
        }
    }
}
