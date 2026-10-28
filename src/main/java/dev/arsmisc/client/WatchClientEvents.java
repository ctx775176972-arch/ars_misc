package dev.arsmisc.client;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid=ArsMiscMod.MOD_ID, bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class WatchClientEvents {
    @SubscribeEvent public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ENCHANTERS_POCKET_WATCH.get(), EnchantersPocketWatchScreen::new);
        event.register(ModMenus.ENCHANTERS_FIELD.get(), EnchantersFieldScreen::new);
    }
    @SubscribeEvent public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) { event.register(CastingKeys.TOGGLE); event.register(CastingKeys.MINING_RANGE); }
    @SubscribeEvent public static void armorColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        event.register((stack, layer) -> layer == 0
                        ? 0xFF584D56 : -1,
                ArsMiscMod.NETHERITE_MAGE_HELMET.get(), ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),
                ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(), ArsMiscMod.NETHERITE_MAGE_BOOTS.get());
    }
    @SubscribeEvent public static void armorTextures(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                NetheriteMageArmorTexture::reload);
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                NetheriteSwordTexture::reload);
    }
}
