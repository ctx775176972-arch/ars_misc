package dev.arsmisc.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.util.function.Supplier;

/** Associates one native spell harvest with its player without copying player state onto the fake player. */
public final class SpellBlockLootOwner {
    private static final class Harvest {
        final Player player;
        final ServerLevel level;
        final BlockPos pos;
        FakePlayer breaker;
        Harvest(Player player, ServerLevel level, BlockPos pos) {
            this.player = player; this.level = level; this.pos = pos;
        }
    }
    private static final ThreadLocal<Harvest> CURRENT = new ThreadLocal<>();
    private SpellBlockLootOwner() {}

    public static <T> T duringHarvest(Player player, ServerLevel level, BlockPos pos, Supplier<T> action) {
        Harvest previous=CURRENT.get();
        CURRENT.set(new Harvest(player,level,pos.immutable()));
        try { return action.get(); }
        finally {
            if(previous==null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static Player ownerFor(ServerLevel level, BlockPos pos, Entity breaker) {
        Harvest harvest=CURRENT.get();
        return harvest!=null && breaker instanceof FakePlayer && harvest.level==level
                && harvest.pos.equals(pos) ? harvest.player : null;
    }

    public static void bindBreaker(FakePlayer breaker) {
        Harvest harvest = CURRENT.get();
        if (harvest != null && breaker.level() == harvest.level) harvest.breaker = breaker;
    }

    public static Player effectOwner(Entity entity) {
        Harvest harvest = CURRENT.get();
        return harvest != null && harvest.breaker == entity ? harvest.player : null;
    }
}
