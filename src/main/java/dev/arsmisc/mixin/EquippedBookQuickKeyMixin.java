package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.util.StackUtil;
import com.hollingsworth.arsnouveau.client.keybindings.KeyHandler;
import com.hollingsworth.arsnouveau.client.registry.ModKeyBindings;
import com.hollingsworth.arsnouveau.common.network.PacketQuickCast;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyHandler.class, remap = false)
public class EquippedBookQuickKeyMixin {
    @Inject(method = "checkCasterKeys", at = @At("HEAD"), cancellable = true)
    private static void equippedQuickKey(int key, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || StackUtil.getHeldCasterTool(mc.player) != null) return;
        int slot = ModKeyBindings.usedQuickSlot(key);
        if (slot < 0 || EquippedSpellbook.find(mc.player).isEmpty()) return;
        PacketDistributor.sendToServer(new PacketQuickCast(slot));
        ci.cancel();
    }
}
