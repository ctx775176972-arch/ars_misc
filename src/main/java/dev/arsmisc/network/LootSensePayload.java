package dev.arsmisc.network;

import dev.arsmisc.ArsMiscMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.List;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record LootSensePayload(ResourceLocation dimension, List<BlockPos> blocks, List<Integer> entities) implements CustomPacketPayload {
    public static final int MAX_TARGETS = 4096;
    public static final Type<LootSensePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "loot_sense"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LootSensePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeResourceLocation(payload.dimension);
                buffer.writeCollection(payload.blocks, (buf, pos) -> buf.writeBlockPos(pos));
                buffer.writeCollection(payload.entities, (buf, id) -> buf.writeVarInt(id));
            }, buffer -> new LootSensePayload(buffer.readResourceLocation(),
                    buffer.readCollection(net.minecraft.network.FriendlyByteBuf.limitValue(ArrayListFactory.BLOCKS, MAX_TARGETS), buf -> buf.readBlockPos()),
                    buffer.readCollection(net.minecraft.network.FriendlyByteBuf.limitValue(ArrayListFactory.ENTITIES, MAX_TARGETS), buf -> buf.readVarInt())));

    private static final class ArrayListFactory {
        static final java.util.function.IntFunction<java.util.ArrayList<BlockPos>> BLOCKS = java.util.ArrayList::new;
        static final java.util.function.IntFunction<java.util.ArrayList<Integer>> ENTITIES = java.util.ArrayList::new;
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() ->
                dev.arsmisc.client.LootSenseClient.receive(payload)));
    }
}
