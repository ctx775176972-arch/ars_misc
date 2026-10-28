package dev.arsmisc.spell;

import com.hollingsworth.arsnouveau.api.item.ISpellHotkeyListener;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A caster tool's inherited no-op editor must not hide the offhand spellbook. */
public final class HeldSpellbookHotkeys {
    private HeldSpellbookHotkeys() {}

    public static boolean hasEditor(ItemStack stack) {
        // The native book's editor override is stripped from the dedicated-server distribution.
        if (stack.getItem() instanceof SpellBook) return true;
        if (!(stack.getItem() instanceof ISpellHotkeyListener)) return false;
        try {
            return stack.getItem().getClass().getMethod("onOpenBookMenuKeyPressed", ItemStack.class, Player.class)
                    .getDeclaringClass() != ISpellHotkeyListener.class;
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }

    public static boolean useOffhandEditor(Player player) {
        return player.getOffhandItem().getItem() instanceof SpellBook && !hasEditor(player.getMainHandItem());
    }
}
