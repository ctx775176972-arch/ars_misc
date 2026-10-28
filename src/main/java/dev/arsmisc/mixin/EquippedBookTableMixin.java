package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.common.block.ScribesBlock;
import dev.arsmisc.spell.EquippedBookScribing;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ScribesBlock.class, remap = false)
public class EquippedBookTableMixin {
    // The native event listener performs an interaction itself, opening the glyph UI
    // or removing the table item. Only skip it during our explicit scribing transaction;
    // the interaction event still reaches protection hooks and other listeners.
    @Inject(method = "rightClick", at = @At("HEAD"), cancellable = true)
    private void explicitScribing(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        if (EquippedBookScribing.activeFor(event.getEntity())) ci.cancel();
    }
}
