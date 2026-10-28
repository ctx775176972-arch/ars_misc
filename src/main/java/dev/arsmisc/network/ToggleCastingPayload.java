package dev.arsmisc.network;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.item.EnchantersFieldItem;
import dev.arsmisc.item.EnchantersPocketWatchItem;
import dev.arsmisc.item.FinalBarrierItem;
import dev.arsmisc.FinalBarrier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/** The server selects active equipped casting accessories and applies one shared state. */
public record ToggleCastingPayload() implements CustomPacketPayload {
    public static final Type<ToggleCastingPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "toggle_casting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleCastingPayload> STREAM_CODEC = StreamCodec.unit(new ToggleCastingPayload());
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static boolean supported(ItemStack stack) {
        return stack.getItem() instanceof EnchantersFieldItem
                || stack.getItem() instanceof EnchantersPocketWatchItem || stack.getItem() instanceof FinalBarrierItem;
    }

    private static boolean enabled(ItemStack stack) {
        if (stack.getItem() instanceof EnchantersFieldItem) return EnchantersFieldItem.enabled(stack);
        if (stack.getItem() instanceof FinalBarrierItem) return FinalBarrierItem.enabled(stack);
        return EnchantersPocketWatchItem.enabled(stack);
    }

    private static void setEnabled(ItemStack stack, boolean value) {
        if (stack.getItem() instanceof EnchantersFieldItem) EnchantersFieldItem.setEnabled(stack, value);
        else EnchantersPocketWatchItem.setEnabled(stack, value);
    }

    public static boolean toggle(Player player) {
        if (player.level().isClientSide || !player.isAlive() || player.isSpectator()) return false;
        return CuriosApi.getCuriosInventory(player).map(inventory -> {
            var equipped = inventory.findCurios(ToggleCastingPayload::supported).stream()
                    .filter(result -> inventory.isSlotActive(result.slotContext().identifier(), result.slotContext().index()))
                    .map(result -> result.stack()).toList();
            if (equipped.isEmpty()) return false;
            boolean next = equipped.stream().noneMatch(ToggleCastingPayload::enabled);
            boolean barrierRefused = false;
            for (var stack : equipped) {
                if (stack.getItem() instanceof FinalBarrierItem) {
                    if (!FinalBarrier.requestState(player, stack, next)) barrierRefused = true;
                } else setEnabled(stack, next);
            }
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
            if (!barrierRefused) player.displayClientMessage(Component.translatable("message.ars_misc.casting_accessories."
                    + (next ? "enabled" : "disabled")), true);
            return true;
        }).orElse(false);
    }
}
