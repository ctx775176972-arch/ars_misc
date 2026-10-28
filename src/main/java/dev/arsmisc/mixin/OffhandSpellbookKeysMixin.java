package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.item.IRadialProvider;
import com.hollingsworth.arsnouveau.client.keybindings.KeyHandler;
import com.hollingsworth.arsnouveau.client.registry.ModKeyBindings;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import dev.arsmisc.spell.HeldSpellbookHotkeys;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KeyHandler.class, remap = false)
public class OffhandSpellbookKeysMixin {
    @Inject(method = "checkCasterKeys", at = @At("HEAD"), cancellable = true)
    private static void arsMisc$offhandBookMenu(int key, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        if (key == -1 || mc.player == null || mc.screen != null) return;
        var offhand = mc.player.getOffhandItem();
        if (!(offhand.getItem() instanceof SpellBook book)) return;

        if (!ModKeyBindings.OPEN_BOOK.isUnbound()
                && key == ModKeyBindings.OPEN_BOOK.getKey().getValue()
                && HeldSpellbookHotkeys.useOffhandEditor(mc.player)) {
            book.onOpenBookMenuKeyPressed(offhand, mc.player);
            ci.cancel();
            return;
        }
        if (!ModKeyBindings.OPEN_RADIAL_HUD.isUnbound()
                && key == ModKeyBindings.OPEN_RADIAL_HUD.getKey().getValue()) {
            // A main-hand radial on a different binding must not swallow this book's binding.
            if (mc.player.getMainHandItem().getItem() instanceof IRadialProvider provider
                    && provider.forKey() == key) return;
            book.onRadialKeyPressed(offhand, mc.player);
            ci.cancel();
        }
    }
}
