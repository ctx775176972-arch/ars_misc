package dev.arsmisc.network;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.spell.EquippedSpellbook;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record EquippedBookPayload(int action, int slot, float pitch, float yaw) implements CustomPacketPayload {
    public static final Type<EquippedBookPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "equipped_book"));
    public static final StreamCodec<ByteBuf, EquippedBookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, EquippedBookPayload::action, ByteBufCodecs.INT, EquippedBookPayload::slot,
            ByteBufCodecs.FLOAT, EquippedBookPayload::pitch, ByteBufCodecs.FLOAT, EquippedBookPayload::yaw,
            EquippedBookPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
            if (payload.action == 0) EquippedSpellbook.cast(player, payload.pitch, payload.yaw);
            else if (payload.action == 4) dev.arsmisc.spell.EquippedBookEditing.begin(player);
            else if (payload.action == 5) dev.arsmisc.spell.EquippedBookEditing.end(player);
            else EquippedSpellbook.select(player, payload.action, payload.slot);
        }));
    }
}
