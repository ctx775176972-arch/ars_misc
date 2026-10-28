package dev.arsmisc.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.LootSenseEffect;
import dev.arsmisc.network.LootSensePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import java.util.OptionalDouble;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, value = Dist.CLIENT)
public final class LootSenseClient {
    private static LootSensePayload targets;
    private static long expires;
    // Vanilla NO_DEPTH_TEST is a no-op; AFTER_LEVEL can still have depth testing enabled.
    private static final RenderStateShard.DepthTestStateShard THROUGH_WALLS = new RenderStateShard.DepthTestStateShard("loot_sense_through_walls", GL11.GL_ALWAYS) {
        private boolean previousDepthTest;
        @Override public void setupRenderState() {
            previousDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
            RenderSystem.disableDepthTest();
        }
        @Override public void clearRenderState() {
            if (previousDepthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
        }
    };
    private static final RenderType OUTLINE = RenderType.create("ars_misc_loot_sense", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(3.5)))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(THROUGH_WALLS)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    public static void receive(LootSensePayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().equals(payload.dimension())) return;
        targets = payload;
        expires = level.getGameTime() + 40;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { targets = null; }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (targets == null || player == null || mc.level == null || mc.options.hideGui || player.isSpectator()
                || !player.hasEffect(ArsMiscMod.LOOT_SENSE) || mc.level.getGameTime() > expires
                || !mc.level.dimension().location().equals(targets.dimension())) return;
        var camera = event.getCamera().getPosition();
        // AFTER_LEVEL supplies a fresh pose stack and the world view separately.
        // Keep vertices camera-relative in doubles before the float vertex transform.
        var pose = new PoseStack();
        pose.mulPose(event.getModelViewMatrix());
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(OUTLINE);
        for (var pos : targets.blocks()) {
            if (mc.level.hasChunkAt(pos) && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= LootSenseEffect.RANGE * LootSenseEffect.RANGE
                    && !mc.level.getBlockState(pos).isAir()) {
                var shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
                var bounds = shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
                LevelRenderer.renderLineBox(pose, lines, bounds.move(-camera.x, -camera.y, -camera.z).inflate(0.002), 1f, .85f, .25f, 1f);
            }
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        for (int id : targets.entities()) {
            var entity = mc.level.getEntity(id);
            if (entity != null && entity.isAlive() && player.distanceToSqr(entity) <= LootSenseEffect.RANGE * LootSenseEffect.RANGE) {
                var position = entity.getPosition(partialTick);
                var bounds = entity.getBoundingBox().move(position.subtract(entity.position()))
                        .move(-camera.x, -camera.y, -camera.z).inflate(.02);
                LevelRenderer.renderLineBox(pose, lines, bounds, 1f, .85f, .25f, 1f);
            }
        }
        buffers.endBatch(OUTLINE);
    }
    private LootSenseClient() {}
}
