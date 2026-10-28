package dev.arsmisc.network;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.MiningRingItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public record ToggleMiningRangePayload() implements CustomPacketPayload {
    public static final Type<ToggleMiningRangePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "toggle_mining_range"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleMiningRangePayload> CODEC = StreamCodec.unit(new ToggleMiningRangePayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, CODEC,
                (payload, context) -> context.enqueueWork(() -> toggle(context.player())));
    }

    public static boolean toggle(Player player) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var rings = inventory.findCurios(stack -> stack.getItem() instanceof MiningRingItem ring
                            && ring.series == MiningRingItem.Series.BLASTING).stream()
                    .filter(result -> result.slotContext().identifier().equals("ring")
                            && inventory.isSlotActive("ring", result.slotContext().index()))
                    .map(result -> result.stack()).toList();
            if (rings.isEmpty()) return false;
            boolean next = rings.stream().noneMatch(MiningRingItem::rangeEnabled);
            rings.forEach(stack -> MiningRingItem.setRangeEnabled(stack, next));
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
            player.displayClientMessage(Component.translatable("message.ars_misc.mining_range."
                    + (next ? "enabled" : "disabled")), true);
            return true;
        }).orElse(false);
    }
}
