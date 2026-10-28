package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.arsmisc.spell.EquippedBookScribing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class EquippedBookScribingMixin {
    @ModifyReturnValue(method = "getItemInHand", at = @At("RETURN"))
    private ItemStack scribingSource(ItemStack held, InteractionHand hand) {
        return EquippedBookScribing.sourceFor((LivingEntity) (Object) this, hand, held);
    }
}
