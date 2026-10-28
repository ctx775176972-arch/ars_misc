package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.network.PacketSetCasterSlot;
import dev.arsmisc.spell.EquippedBookEditing;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketSetCasterSlot.class, remap = false)
public class EquippedBookSlotMixin {
    @Shadow public int slot;
    @Inject(method = "onServerReceived", at = @At("HEAD"), cancellable = true)
    private void editorSelection(MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        if (!EquippedBookEditing.active(player)) return;
        if (!EquippedBookEditing.book(player).isEmpty()) EquippedSpellbook.select(player, 3, slot);
        ci.cancel();
    }
}
