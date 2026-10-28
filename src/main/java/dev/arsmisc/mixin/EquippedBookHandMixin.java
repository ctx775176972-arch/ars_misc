package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.arsmisc.client.EquippedBookClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class EquippedBookHandMixin {
    @ModifyReturnValue(method = "getItemInHand", at = @At("RETURN"))
    private ItemStack editorView(ItemStack held, net.minecraft.world.InteractionHand hand) {
        var mc = Minecraft.getInstance();
        if ((Object) this == mc.player && hand == net.minecraft.world.InteractionHand.MAIN_HAND
                && EquippedBookClient.isEditorScreen(mc.screen)) return EquippedBookClient.editingBook;
        return held;
    }
}
