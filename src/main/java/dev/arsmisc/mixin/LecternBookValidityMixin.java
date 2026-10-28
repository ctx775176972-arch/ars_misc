package dev.arsmisc.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBookValidityMixin {
    @ModifyReturnValue(method = "hasBook", at = @At("RETURN"))
    private boolean arsMisc$acceptLecternBooks(boolean original) {
        // Placement already accepts this tag. Keep the menu valid for the same
        // books so the vanilla Take Book button remains usable.
        var book = ((LecternBlockEntity)(Object)this).getBook();
        return original || (!book.isEmpty() && book.is(ItemTags.LECTERN_BOOKS));
    }
}
