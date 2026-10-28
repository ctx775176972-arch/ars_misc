package dev.arsmisc.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.network.ToggleCastingPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid=ArsMiscMod.MOD_ID,value=Dist.CLIENT)
public final class CastingKeys {
    public static final KeyMapping TOGGLE=new KeyMapping("key.ars_misc.toggle_casting_accessories",InputConstants.Type.KEYSYM,-1,"key.categories.ars_misc");
    public static final KeyMapping MINING_RANGE=new KeyMapping("key.ars_misc.toggle_mining_range",InputConstants.Type.KEYSYM,-1,"key.categories.ars_misc");
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        while(TOGGLE.consumeClick())if(Minecraft.getInstance().player!=null && Minecraft.getInstance().screen==null)
            PacketDistributor.sendToServer(new ToggleCastingPayload());
        while(MINING_RANGE.consumeClick())if(Minecraft.getInstance().player!=null && Minecraft.getInstance().screen==null)
            PacketDistributor.sendToServer(new dev.arsmisc.network.ToggleMiningRangePayload());
    }
}
