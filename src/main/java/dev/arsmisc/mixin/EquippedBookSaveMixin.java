package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.network.*;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.arsmisc.spell.EquippedBookEditing;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {PacketUpdateCaster.class, PacketUpdateParticleTimeline.class}, remap = false)
public class EquippedBookSaveMixin {
    @ModifyExpressionValue(method = "onServerReceived", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    private ItemStack equippedTool(ItemStack original, MinecraftServer server, ServerPlayer player) {
        return EquippedBookEditing.active(player) ? EquippedBookEditing.book(player) : original;
    }
}
