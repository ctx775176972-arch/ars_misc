package dev.arsmisc.spell;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

/** Player Touch targeting with independent entity and block reach. */
public final class TouchTargeting {
    private TouchTargeting() {}

    public static HitResult trace(Player player, float partialTick, boolean sensitive) {
        double entityRange = range(player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), 2, 5);
        double blockRange = range(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), 0.5, 5);
        var blockHit = player.pick(blockRange, partialTick, sensitive);
        var eye = player.getEyePosition(partialTick);
        var direction = player.getViewVector(partialTick);
        var end = eye.add(direction.scale(entityRange));
        // Check occlusion over the entire entity ray even when block reach is shorter.
        var obstruction = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                sensitive ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE, player));
        double maxDistanceSquared = entityRange * entityRange;
        if (obstruction.getType() == HitResult.Type.BLOCK) {
            maxDistanceSquared = Math.min(maxDistanceSquared, eye.distanceToSqr(obstruction.getLocation()));
        }
        var bounds = player.getBoundingBox().expandTowards(direction.scale(entityRange)).inflate(1);
        var entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, bounds,
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity.isPickable(),
                maxDistanceSquared);
        return entityHit != null ? entityHit : blockHit;
    }

    private static double range(double attribute, double bonus, double fallback) {
        double result = attribute + bonus;
        return Double.isFinite(result) ? Math.max(0, result) : fallback;
    }
}
