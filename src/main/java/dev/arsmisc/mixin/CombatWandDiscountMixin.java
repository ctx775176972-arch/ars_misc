package dev.arsmisc.mixin;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.util.ManaUtil;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsmisc.item.CombatWandItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ManaUtil.class)
public abstract class CombatWandDiscountMixin {
    @WrapMethod(method="getPlayerDiscounts")
    private static int arsMisc$castingToolDiscount(LivingEntity caster,Spell spell,ItemStack castingTool,Operation<Integer> original) {
        int discount=original.call(caster,spell,castingTool);
        // Only the actual casting tool receives this bonus; holding or wearing a wand cannot discount another tool.
        if(caster!=null && castingTool.getItem() instanceof CombatWandItem)
            return (int)Math.clamp((long)discount+CombatWandItem.MANA_DISCOUNT,Integer.MIN_VALUE,Integer.MAX_VALUE);
        return discount;
    }
}
