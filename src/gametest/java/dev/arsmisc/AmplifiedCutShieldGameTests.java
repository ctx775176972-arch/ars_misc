package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class AmplifiedCutShieldGameTests {
    private static ServerPlayer blocker(GameTestHelper h, Item shield) {
        var player = ReactiveShieldGameTests.player(h);
        player.setItemInHand(InteractionHand.OFF_HAND, shield.getDefaultInstance());
        player.startUsingItem(InteractionHand.OFF_HAND);
        return player;
    }

    private static void hit(GameTestHelper h, ServerPlayer target, AbstractEffect effect, boolean amplify, boolean front) {
        var caster = ReactiveShieldGameTests.player(h);
        caster.setPos(target.position().add(0, 0, front ? 2 : -2));
        var spell = new Spell(MethodTouch.INSTANCE, effect);
        var context = SpellContext.fromEntity(spell, caster, ItemStack.EMPTY);
        var builder = new SpellStats.Builder();
        if (amplify) builder.addAugment(AugmentAmplify.INSTANCE);
        boolean pvp = h.getLevel().getServer().isPvpAllowed();
        h.getLevel().getServer().setPvpAllowed(true);
        try {
            effect.onResolveEntity(new EntityHitResult(target), h.getLevel(), caster,
                    builder.build(), context, new SpellResolver(context));
        } finally { h.getLevel().getServer().setPvpAllowed(pvp); }
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void amplifiedCutBlocksFirstHitThenDisablesVanillaAndEnchantersShields(GameTestHelper h) {
        for (var shield : java.util.List.of(Items.SHIELD, ItemsRegistry.ENCHANTERS_SHIELD.get())) {
            var target = blocker(h, shield);
            hit(h, target, EffectCut.INSTANCE, true, true);
            h.assertTrue(target.getHealth() == 20, "Shield-breaking hit bypassed the original block");
            h.assertTrue(!target.isUsingItem() && target.getCooldowns().isOnCooldown(shield), "Amplified Cut did not disable shield");
            for (int i = 0; i < 99; i++) target.getCooldowns().tick();
            h.assertTrue(target.getCooldowns().isOnCooldown(shield), "Shield cooldown was shorter than five seconds");
            target.getCooldowns().tick();
            h.assertTrue(!target.getCooldowns().isOnCooldown(shield), "Shield cooldown exceeded five seconds");
        }
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void plainCutOtherSpellsAndRearHitsDoNotDisableShields(GameTestHelper h) {
        var plain = blocker(h, Items.SHIELD);
        hit(h, plain, EffectCut.INSTANCE, false, true);
        h.assertTrue(plain.isBlocking() && !plain.getCooldowns().isOnCooldown(Items.SHIELD), "Plain Cut disabled shield");
        var harm = blocker(h, Items.SHIELD);
        hit(h, harm, EffectHarm.INSTANCE, true, true);
        h.assertTrue(harm.isBlocking() && !harm.getCooldowns().isOnCooldown(Items.SHIELD), "Another glyph disabled shield");
        var rear = blocker(h, Items.SHIELD);
        hit(h, rear, EffectCut.INSTANCE, true, false);
        h.assertTrue(rear.getHealth() < 20 && rear.isBlocking() && !rear.getCooldowns().isOnCooldown(Items.SHIELD),
                "A rear hit disabled an unblocking shield");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void cancelledAndZeroBlocksDoNotDisableShields(GameTestHelper h) {
        for (boolean cancel : new boolean[]{true, false}) {
            var target = blocker(h, Items.SHIELD);
            java.util.function.Consumer<LivingShieldBlockEvent> listener = event -> {
                if (event.getEntity() == target) {
                    if (cancel) event.setCanceled(true); else event.setBlockedDamage(0);
                }
            };
            NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, listener);
            try { hit(h, target, EffectCut.INSTANCE, true, true); }
            finally { NeoForge.EVENT_BUS.unregister(listener); }
            h.assertTrue(!target.getCooldowns().isOnCooldown(Items.SHIELD), "An uncommitted block disabled shield");
        }
        h.succeed();
    }
}
