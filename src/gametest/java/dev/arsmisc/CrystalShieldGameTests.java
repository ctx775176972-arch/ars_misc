package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectDispel;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class CrystalShieldGameTests {
    private static void cast(GameTestHelper h, LivingEntity caster, LivingEntity target, int amplify, boolean extended) {
        var glyph = EffectCrystalShield.INSTANCE;
        var context = SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, glyph), caster, ItemStack.EMPTY);
        var stats = new SpellStats.Builder();
        for (int i = 0; i < amplify; i++) stats.addAugment(AugmentAmplify.INSTANCE);
        if (extended) {
            stats.addAugment(AugmentExtendTime.INSTANCE);
            AugmentExtendTime.INSTANCE.applyModifiers(stats, glyph, new EntityHitResult(target), h.getLevel(), caster, context);
        }
        glyph.onResolveEntity(new EntityHitResult(target), h.getLevel(), caster, stats.build(), context, new SpellResolver(context));
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void shieldSnapshotsPowerReducesDamageAndDispelRestoresMovement(GameTestHelper h) {
        var caster = h.makeMockPlayer(GameType.SURVIVAL);
        caster.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(6);
        var target = h.spawnWithNoFreeWill(EntityType.COW, 1, 2, 1);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        double speed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
        cast(h, caster, target, 3, true);
        var effect = target.getEffect(ArsMiscMod.CRYSTAL_SHIELD);
        h.assertTrue(effect != null && effect.getAmplifier() == 3 && effect.getDuration() == 800, "Wrong shield level or extended duration");
        h.assertTrue(Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 0.75) < 0.00001, "Slowness scaled with effect level");
        caster.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(0);
        h.assertTrue(Math.abs(CrystalShieldEvents.reduction(target) - 0.36) < 0.00001, "Caster power was not snapshotted");
        target.hurt(target.damageSources().generic(), 10);
        h.assertTrue(Math.abs(target.getHealth() - 93.6) < 0.001, "Shield did not reduce actual incoming damage");
        cast(h, caster, target, 0, false);
        h.assertTrue(target.getEffect(ArsMiscMod.CRYSTAL_SHIELD).getAmplifier() == 3
                && Math.abs(CrystalShieldEvents.reduction(target) - 0.36) < 0.00001, "Weak recast replaced the stronger shield or its power");
        var context = SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, EffectDispel.INSTANCE), caster, ItemStack.EMPTY);
        EffectDispel.INSTANCE.onResolveEntity(new EntityHitResult(target), h.getLevel(), caster,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
        h.assertTrue(!target.hasEffect(ArsMiscMod.CRYSTAL_SHIELD)
                && Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed) < 0.00001, "Dispel retained shield defenses or slowness");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void shieldedGolemDoesNotDropExtraAmethyst(GameTestHelper h) {
        var caster = h.makeMockPlayer(GameType.SURVIVAL);
        var target = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 1, 2, 1);
        cast(h, caster, target, 3, false);
        h.assertTrue(target.getMaxHealth() == 100, "Golem is not a 100-health example");
        target.hurt(target.damageSources().genericKill(), 10000);
        h.runAfterDelay(1, () -> {
            int shards = h.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(3)).stream()
                    .map(ItemEntity::getItem).filter(s -> s.is(Items.AMETHYST_SHARD)).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(shards == 0, "Crystal Shield must not add amethyst drops, got " + shards);
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void baseShieldAndCraftingLimitMatchPrototype(GameTestHelper h) {
        var caster = h.makeMockPlayer(GameType.SURVIVAL);
        cast(h, caster, caster, 0, false);
        h.assertTrue(caster.getEffect(ArsMiscMod.CRYSTAL_SHIELD).getDuration() == 600
                && Math.abs(CrystalShieldEvents.reduction(caster) - 0.15) < 0.00001, "Base shield is not 30 seconds and 15% reduction");
        var parts = new java.util.ArrayList<AbstractSpellPart>();
        parts.add(EffectCrystalShield.INSTANCE);
        for (int i = 0; i < 3; i++) parts.add(AugmentAmplify.INSTANCE);
        parts.addFirst(com.hollingsworth.arsnouveau.common.spell.method.MethodSelf.INSTANCE);
        var validator=com.hollingsworth.arsnouveau.api.ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator();
        h.assertTrue(validator.validate(parts).isEmpty(), "Three Amplify glyphs were rejected");
        parts.add(AugmentAmplify.INSTANCE);
        h.assertTrue(!validator.validate(parts).isEmpty(), "Fourth Amplify was accepted");
        h.succeed();
    }
}
