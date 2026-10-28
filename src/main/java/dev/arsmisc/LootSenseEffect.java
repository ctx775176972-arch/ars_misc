package dev.arsmisc;

import dev.arsmisc.network.LootSensePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class LootSenseEffect extends MobEffect {
    public static final int RANGE = 75;
    public LootSenseEffect() { super(MobEffectCategory.BENEFICIAL, 0xe7ae45); }

    public static boolean hasLootTable(Object target) {
        return target instanceof RandomizableContainer container && container.getLootTable() != null
                || target instanceof ContainerEntity vehicle && vehicle.getLootTable() != null;
    }

    /** Iterate only existing chunk block entities; never open inventories or generate loot/chunks. */
    public static LootSensePayload scan(ServerPlayer player) {
        var blocks = new ArrayList<BlockPos>();
        var level = player.serverLevel();
        int minX = (player.getBlockX() - RANGE) >> 4, maxX = (player.getBlockX() + RANGE) >> 4;
        int minZ = (player.getBlockZ() - RANGE) >> 4, maxZ = (player.getBlockZ() + RANGE) >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            var chunk = level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false);
            if (!(chunk instanceof net.minecraft.world.level.chunk.LevelChunk loaded)) continue;
            for (var block : loaded.getBlockEntities().values()) {
                if (blocks.size() >= LootSensePayload.MAX_TARGETS) break;
                if (!block.isRemoved() && hasLootTable(block)
                        && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(block.getBlockPos())) <= RANGE * RANGE)
                    blocks.add(block.getBlockPos().immutable());
            }
        }
        var entities = level.getEntities(player, player.getBoundingBox().inflate(RANGE), entity ->
                hasLootTable(entity) && entity.isAlive() && player.distanceToSqr(entity) <= RANGE * RANGE)
                .stream().limit(LootSensePayload.MAX_TARGETS).map(Entity::getId).toList();
        return new LootSensePayload(level.dimension().location(), blocks, entities);
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0
                && player.isAlive() && !player.isSpectator() && player.hasEffect(ArsMiscMod.LOOT_SENSE))
            PacketDistributor.sendToPlayer(player, scan(player));
    }
}
