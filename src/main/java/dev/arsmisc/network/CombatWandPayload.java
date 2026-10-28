package dev.arsmisc.network;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.item.CombatWandItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record CombatWandPayload(float pitch, float yaw) implements CustomPacketPayload {
    public static final Type<CombatWandPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "combat_wand_cast"));
    public static final StreamCodec<ByteBuf, CombatWandPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CombatWandPayload::pitch, ByteBufCodecs.FLOAT, CombatWandPayload::yaw, CombatWandPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) CombatWandItem.cast(player, payload.pitch, payload.yaw);
        }));
    }
}
