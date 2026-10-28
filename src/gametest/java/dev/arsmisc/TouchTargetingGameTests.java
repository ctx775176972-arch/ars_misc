package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsmisc.spell.EquippedSpellbook;
import dev.arsmisc.spell.TouchTargeting;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class TouchTargetingGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var player = ReactiveShieldGameTests.player(h);
        player.setPos(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(3);
        player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(4.5);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(2000); mana.setMana(1500);
        return player;
    }
    private static boolean hits(HitResult hit, net.minecraft.world.entity.Entity target) {
        return hit instanceof EntityHitResult entity && entity.getEntity() == target;
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void entityRangeIsIndependentAndWallsStillBlockBeyondBlockReach(GameTestHelper h) {
        var p = player(h);
        var far = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 10);
        h.assertTrue(!hits(TouchTargeting.trace(p, 1, false), far), "Default Touch exceeded five blocks");
        p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(20);
        h.assertTrue(!hits(TouchTargeting.trace(p, 1, false), far), "Block reach increased entity reach");
        p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(1);
        p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(7);
        h.assertTrue(hits(TouchTargeting.trace(p, 1, false), far), "Entity reach did not extend Touch independently");
        h.setBlock(new BlockPos(2, 3, 7), Blocks.STONE);
        h.assertTrue(!hits(TouchTargeting.trace(p, 1, false), far), "Touch selected an entity through a distant wall");
        var near = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 5);
        h.assertTrue(hits(TouchTargeting.trace(p, 1, false), near), "Wall blocked a nearer visible entity");
        near.discard(); far.discard(); h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void blockRangeAndSensitiveFluidSelectionRemainSeparate(GameTestHelper h) {
        var p = player(h);
        h.setBlock(new BlockPos(2, 3, 10), Blocks.STONE);
        p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(20);
        h.assertTrue(TouchTargeting.trace(p, 1, false).getType() == HitResult.Type.MISS,
                "Entity reach increased block reach");
        p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(8);
        var hit = TouchTargeting.trace(p, 1, false);
        h.assertTrue(hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && block.getBlockPos().equals(h.absolutePos(new BlockPos(2, 3, 10))), "Block reach was not respected");
        p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(4.5);
        h.setBlock(new BlockPos(2, 3, 5), Blocks.WATER);
        h.assertTrue(TouchTargeting.trace(p, 1, false).getType() == HitResult.Type.MISS,
                "Normal Touch selected a fluid source");
        hit = TouchTargeting.trace(p, 1, true);
        h.assertTrue(hit instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                && block.getBlockPos().equals(h.absolutePos(new BlockPos(2, 3, 5))), "Sensitive Touch lost fluid selection");
        h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void heldAndEquippedBooksCastTouchUsingEntityReach(GameTestHelper h) {
        for (boolean equipped : new boolean[]{false, true}) {
            var p = player(h);
            p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).setBaseValue(7);
            p.getAttribute(Attributes.BLOCK_INTERACTION_RANGE).setBaseValue(1);
            var target = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 10);
            var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
            var spell = new Spell(MethodTouch.INSTANCE, EffectHarm.INSTANCE);
            SpellCasterRegistry.from(book).setSpell(spell).saveToStack(book);
            if (equipped) {
                CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("spellbook", 0, book);
                EquippedSpellbook.cast(p, 0, 0);
            } else {
                p.setItemInHand(InteractionHand.MAIN_HAND, book);
                SpellCasterRegistry.from(book).castSpell(h.getLevel(), p, InteractionHand.MAIN_HAND, null, spell);
            }
            h.assertTrue(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == p
                    && target.getHealth() < target.getMaxHealth(), "Touch failed beyond old reach: equipped=" + equipped);
            h.assertTrue(CapabilityRegistry.getMana(p).getCurrentMana() < 1500, "Touch bypassed native mana cost");
            target.discard();
        }
        h.succeed();
    }
}
