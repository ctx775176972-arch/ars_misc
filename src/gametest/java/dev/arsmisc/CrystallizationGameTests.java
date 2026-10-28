package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class CrystallizationGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var player = ReactiveShieldGameTests.player(h);
        player.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(0);
        player.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        var crit = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.getHolder(
                net.minecraft.resources.ResourceLocation.parse("apothic_attributes:crit_chance"));
        crit.ifPresent(holder -> { if (player.getAttribute(holder) != null) player.getAttribute(holder).setBaseValue(0); });
        return player;
    }
    private static SpellStats stats(GameTestHelper h, LivingEntity caster, LivingEntity target, int amplify, AbstractAugment... timed) {
        var effect = EffectCrystallization.INSTANCE;
        var context = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, effect), caster, ItemStack.EMPTY);
        var builder = new SpellStats.Builder();
        for (int i = 0; i < amplify; i++) {
            builder.addAugment(AugmentAmplify.INSTANCE);
            AugmentAmplify.INSTANCE.applyModifiers(builder, effect, new EntityHitResult(target), h.getLevel(), caster, context);
        }
        for (var augment : timed) {
            builder.addAugment(augment);
            augment.applyModifiers(builder, effect, new EntityHitResult(target), h.getLevel(), caster, context);
        }
        return builder.build();
    }
    private static void cast(GameTestHelper h, LivingEntity caster, LivingEntity target, SpellStats stats) {
        var context = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, EffectCrystallization.INSTANCE), caster, ItemStack.EMPTY);
        EffectCrystallization.INSTANCE.onResolveEntity(new EntityHitResult(target), h.getLevel(), caster, stats, context, new SpellResolver(context));
    }
    private static int shards(GameTestHelper h, LivingEntity target) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, target.getBoundingBox().inflate(.5)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(Items.AMETHYST_SHARD)).mapToInt(ItemStack::getCount).sum();
    }
    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void directDamageAndKillDropsScaleFromOneToFour(GameTestHelper h) {
        var p = player(h);
        for (int amplify = 0; amplify <= 3; amplify++) {
            var target = h.spawnWithNoFreeWill(EntityType.COW, 2 + amplify * 2, 2, 2);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);
            target.setHealth(100);
            float health = target.getHealth();
            cast(h, p, target, stats(h, p, target, amplify));
            h.assertTrue(Math.abs(health - target.getHealth() - (5 + amplify * 2)) < .001,
                    "Wrong native-style damage scaling: amplify=" + amplify + ", actual=" + (health - target.getHealth()));
            h.assertTrue(!target.hasEffect(ArsMiscMod.CRYSTALLIZATION) && shards(h, target) == 0, "Nonlethal damage granted status or shards");
            target.invulnerableTime = 0; target.setHealth(1);
            cast(h, p, target, stats(h, p, target, amplify));
            h.assertTrue(target.isDeadOrDying() && shards(h, target) == amplify + 1, "Direct kill did not grant exact shard count");
        }
        h.succeed();
    }
    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void bothTimeAugmentsReplaceDamageAndResistanceIsRestored(GameTestHelper h) {
        var p = player(h);
        var target = h.spawnWithNoFreeWill(EntityType.COW, 2, 2, 2);
        var resistance = target.getAttribute(ModRegistry.SPELL_RESISTANCE);
        resistance.setBaseValue(.5);
        for (int amplify = 0; amplify <= 3; amplify++) {
            for (var time : java.util.List.of(AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE)) {
                target.removeEffect(ArsMiscMod.CRYSTALLIZATION);
                float health = target.getHealth();
                cast(h, p, target, stats(h, p, target, amplify, time));
                var effect = target.getEffect(ArsMiscMod.CRYSTALLIZATION);
                int level = Math.min(amplify + 1, 3);
                h.assertTrue(effect != null && effect.getAmplifier() == level - 1 && target.getHealth() == health,
                        "Timed branch damaged target or used wrong status level");
                h.assertTrue(effect.getDuration() == (time == AugmentExtendTime.INSTANCE ? 300 : 100), "Wrong extended/shortened duration");
                h.assertTrue(Math.abs(resistance.getValue() - (.5 - .1 * level)) < .00001, "Resistance reduction is not additive percentage points");
            }
        }
        target.invulnerableTime = 0;
        float health = target.getHealth();
        cast(h, p, target, stats(h, p, target, 0));
        h.assertTrue(Math.abs(health - target.getHealth() - 4) < .001, "Sauce did not apply 20% remaining resistance");
        target.removeEffect(ArsMiscMod.CRYSTALLIZATION);
        h.assertTrue(Math.abs(resistance.getValue() - .5) < .00001, "Debuff removal left resistance modifier");
        resistance.setBaseValue(0);
        target.addEffect(new MobEffectInstance(ArsMiscMod.CRYSTALLIZATION, 200, 2));
        h.assertTrue(Math.abs(resistance.getValue() + .3) < .00001, "Negative resistance was clamped");
        h.succeed();
    }
    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void noRewardForOtherFinisherTotemOrCanceledDrops(GameTestHelper h) {
        var p = player(h);
        var other = h.spawnWithNoFreeWill(EntityType.COW, 2, 2, 2);
        cast(h, p, other, stats(h, p, other, 0));
        other.hurt(other.damageSources().genericKill(), 1000);
        h.assertTrue(shards(h, other) == 0, "Later unrelated finisher granted shards");
        var saved = h.spawnWithNoFreeWill(EntityType.COW, 5, 2, 2);
        saved.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        saved.setHealth(1);
        cast(h, p, saved, stats(h, p, saved, 3));
        h.assertTrue(saved.isAlive() && shards(h, saved) == 0, "Totem survival granted shards");
        var canceled = h.spawnWithNoFreeWill(EntityType.COW, 8, 2, 2);
        canceled.setHealth(1);
        java.util.function.Consumer<LivingDropsEvent> cancel = event -> { if (event.getEntity() == canceled) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(cancel);
        try { cast(h, p, canceled, stats(h, p, canceled, 3)); }
        finally { NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(shards(h, canceled) == 0, "Reward bypassed canceled drops");
        h.succeed();
    }
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void glyphTierRecipeAndAmplifyLimit(GameTestHelper h) {
        var glyph = EffectCrystallization.INSTANCE;
        h.assertTrue(glyph.getConfigTier() == SpellTier.ONE && glyph.getCastingCost() == 15
                && glyph.getAugmentLimit(AugmentAmplify.INSTANCE.getRegistryName()) == 3, "Wrong tier, cost or amplify cap");
        var holder = h.getLevel().getRecipeManager().byKey(glyph.getRegistryName()).orElseThrow();
        var recipe = (com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe) holder.value();
        h.assertTrue(recipe.getInputs().size() == 4 && recipe.getOutput().is(glyph.getGlyph())
                && recipe.getInputs().stream().filter(i -> i.test(new ItemStack(Items.AMETHYST_SHARD))).count() == 3
                && recipe.getInputs().getFirst().test(com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.ABJURATION_ESSENCE.get().getDefaultInstance()), "Wrong scribing ingredients");
        h.succeed();
    }
}
