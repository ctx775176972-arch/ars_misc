package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.network.PacketQuickCast;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketQuickCast.class, remap = false)
public class EquippedBookQuickCastMixin {
    @Shadow int slot;
    @Shadow float xRot;
    @Shadow float yRot;
    @Inject(method = "onServerReceived", at = @At("HEAD"), cancellable = true)
    private void equippedFallback(MinecraftServer server, ServerPlayer player, CallbackInfo ci) {
        if (SpellCasterRegistry.from(player.getMainHandItem()) != null || SpellCasterRegistry.from(player.getOffhandItem()) != null) return;
        var book = EquippedSpellbook.find(player);
        var caster = SpellCasterRegistry.from(book);
        if (caster == null || slot < 0 || slot >= caster.getMaxSlots()) return;
        // Quick casting must not change the prepared slot permanently.
        int original = caster.getCurrentSlot();
        try {
            caster.setCurrentSlot(slot).saveToStack(book);
            EquippedSpellbook.cast(player, xRot, yRot);
        } finally {
            var updated = SpellCasterRegistry.from(book);
            if (updated != null) updated.setCurrentSlot(original).saveToStack(book);
        }
        ci.cancel();
    }
}
