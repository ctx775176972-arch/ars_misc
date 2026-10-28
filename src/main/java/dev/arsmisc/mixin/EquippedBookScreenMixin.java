package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.arsmisc.client.EquippedBookClient;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpellSlottedScreen.class, remap = false)
public class EquippedBookScreenMixin {
    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/world/InteractionHand;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", remap = true))
    private ItemStack editorTool(ItemStack held) {
        return EquippedBookClient.openingEditor
                || EquippedBookClient.isEditorScreen(net.minecraft.client.Minecraft.getInstance().screen)
                ? EquippedBookClient.editingBook : held;
    }
}
