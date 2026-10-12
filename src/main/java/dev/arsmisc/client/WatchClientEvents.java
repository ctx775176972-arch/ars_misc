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
    }
}
