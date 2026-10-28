package dev.arsmisc.spell;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Binds native editor packets to the exact equipped stack opened by the player. */
public final class EquippedBookEditing {
    private static final Map<ServerPlayer, ItemStack> SESSIONS = new WeakHashMap<>();
    public static void begin(ServerPlayer player) {
        ItemStack book = EquippedSpellbook.find(player);
        if (!book.isEmpty()) SESSIONS.put(player, book);
    }
    public static void end(ServerPlayer player) { SESSIONS.remove(player); }
    public static boolean active(ServerPlayer player) { return SESSIONS.containsKey(player); }
    public static ItemStack book(ServerPlayer player) {
        ItemStack bound = SESSIONS.get(player);
        return bound != null && bound == EquippedSpellbook.find(player) ? bound : ItemStack.EMPTY;
    }
}
