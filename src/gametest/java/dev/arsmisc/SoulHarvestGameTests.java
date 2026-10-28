package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.function.Consumer;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SoulHarvestGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p = ReactiveShieldGameTests.player(h);
        p.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(10);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT).setBaseValue(0);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_DAMAGE_MULTIPLIER).setBaseValue(1);
        BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> p.getAttribute(attribute).setBaseValue(0));
        return p;
    }
    private static net.minecraft.world.entity.monster.Husk target(GameTestHelper h, float health) {
        var target = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 4);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(health);
        return target;
    }
    private static void cast(GameTestHelper h, ServerPlayer p, LivingEntity target) {
        var glyph = EffectSoulHarvest.INSTANCE;
        var context = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, glyph), p, ItemStack.EMPTY);
        glyph.onResolveEntity(new EntityHitResult(target), h.getLevel(), p,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
    }
    private static void assertGrowth(GameTestHelper h, LivingEntity p, double mana) {
        h.assertTrue(!p.getPersistentData().getCompound("ars_misc:soul_harvest").contains("health")
                && Math.abs(SoulHarvestGrowth.manaBonus(p) - mana) < 0.00001, "Incorrect persistent harvest growth");
    }
    private static void assertRegen(GameTestHelper h, LivingEntity p, double expected) {
        h.assertTrue(Math.abs(SoulHarvestGrowth.regenBonus(p)-expected)<0.00001,"Incorrect permanent regeneration growth");
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void legacyHealthIsRemovedWithoutChangingOtherHealthOrManaGrowth(GameTestHelper h) {
        var p=player(h);
        var health=p.getAttribute(Attributes.MAX_HEALTH);
        health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ResourceLocation.parse("ars_misc:soul_harvest_health"),10,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        health.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                ResourceLocation.parse("ars_misc:test_other_health"),6,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
        p.setHealth(p.getMaxHealth());
        var data=new net.minecraft.nbt.CompoundTag();data.putDouble("health",10);data.putDouble("mana",60);data.putDouble("regen",1.2);
        p.getPersistentData().put("ars_misc:soul_harvest",data);
        SoulHarvestGrowth.restore(p);SoulHarvestGrowth.restore(p);
        h.assertTrue(p.getMaxHealth()==26 && p.getHealth()==26
                &&health.getModifier(ResourceLocation.parse("ars_misc:soul_harvest_health"))==null
                &&health.getModifier(ResourceLocation.parse("ars_misc:test_other_health"))!=null,"Legacy health cleanup changed unrelated health");
        assertGrowth(h,p,60);assertRegen(h,p,1.2);
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void manaAttributesUseTenfoldLimitsAndRetainNativeCalculations(GameTestHelper h) {
        var max=(net.minecraft.world.entity.ai.attributes.RangedAttribute)PerkAttributes.MAX_MANA.get();
        var regen=(net.minecraft.world.entity.ai.attributes.RangedAttribute)PerkAttributes.MANA_REGEN_BONUS.get();
        h.assertTrue(max.getMaxValue()==100000 && regen.getMaxValue()==20000,"Mana attribute limits were not multiplied by ten");
        var unrelated=new net.minecraft.world.entity.ai.attributes.RangedAttribute("ars_misc:test_other",0,0,100);
        h.assertTrue(unrelated.getMaxValue()==100,"Unrelated attribute limit was changed");
        var p=player(h);
        p.getAttribute(PerkAttributes.MAX_MANA).setBaseValue(50000);
        p.getAttribute(PerkAttributes.MANA_REGEN_BONUS).setBaseValue(5000);
        h.assertTrue(com.hollingsworth.arsnouveau.api.util.ManaUtil.getMaxMana(p)>=50000
                && com.hollingsworth.arsnouveau.api.util.ManaUtil.getManaRegen(p)>=5000,"Native calculations still clamp at old limits");
        var cap=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p);
        cap.setMaxMana(50000);cap.setMana(40000);
        h.assertTrue(cap.getCurrentMana()==40000,"Mana storage still clamps at old limit");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void defaultsRecipeAndMagicDamageAreRegistered(GameTestHelper h) {
        var glyph = EffectSoulHarvest.INSTANCE;
        var source = glyph.buildDamageSource(h.getLevel(), player(h));
        h.assertTrue(glyph.defaultTier() == SpellTier.TWO && glyph.getDefaultManaCost() == 50
                && glyph.getSchools().contains(SpellSchools.NECROMANCY) && glyph.getCompatibleAugments().isEmpty(), "Incorrect glyph defaults");
        h.assertTrue(source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC)
                && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_RESISTANCE), "Harvest damage unexpectedly bypasses reductions");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("ars_misc:glyph_soul_harvest")).isEmpty()
                && glyph.getGlyph() != null, "Harvest recipe was not disabled by default or glyph is missing");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void spellPowerIsCountedOnceAndNormalMultipliersRemainActive(GameTestHelper h) {
        var p = player(h);
        var target = target(h, 100);
        cast(h, p, target);
        h.assertTrue(Math.abs(target.getHealth() - 90) < 0.001, "Spell power was doubled or a base damage was added");
        target.discard();
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_DAMAGE_MULTIPLIER).setBaseValue(1.5);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT).setBaseValue(1);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT_DAMAGE).setBaseValue(1);
        target = target(h, 100);
        cast(h, p, target);
        h.assertTrue(Math.abs(target.getHealth() - 70) < 0.001, "Native spell multiplier or critical scaling changed");
        target.discard();
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_DAMAGE_MULTIPLIER).setBaseValue(1);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT).setBaseValue(0);
        target = target(h, 100);
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200));
        cast(h, p, target);
        h.assertTrue(Math.abs(target.getHealth() - 92) < 0.001, "Resistance was bypassed");
        assertGrowth(h, p, 0);
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void onlyConfirmedHarvestKillsGrantCumulativeGrowth(GameTestHelper h) {
        var p = player(h);
        double health = p.getMaxHealth(), mana = p.getAttributeValue(PerkAttributes.MAX_MANA);
        double regen = p.getAttributeValue(PerkAttributes.MANA_REGEN_BONUS);
        var survivor = target(h, 100);
        cast(h, p, survivor);
        assertGrowth(h, p, 0);
        survivor.hurt(EffectSoulHarvest.INSTANCE.buildDamageSource(h.getLevel(), p), 1000);
        assertGrowth(h, p, 0);
        var first = target(h, 5); cast(h, p, first); cast(h, p, first);
        assertGrowth(h, p, 30);
        var second = target(h, 5); cast(h, p, second);
        assertGrowth(h, p, 60);
        assertRegen(h,p,1.2);
        h.assertTrue(Math.abs(p.getAttributeValue(PerkAttributes.MANA_REGEN_BONUS)-regen-1.2)<0.00001,"Regeneration attribute did not grow");
        h.assertTrue(Math.abs(p.getMaxHealth() - health) < 0.00001
                && Math.abs(p.getAttributeValue(PerkAttributes.MAX_MANA) - mana - 60) < 0.00001,
                "Harvest growth did not apply to native attributes");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void canceledDeathAndTotemDoNotGrantGrowth(GameTestHelper h) {
        var p = player(h);
        var rescued = target(h, 5);
        Consumer<LivingDeathEvent> listener = event -> {
            if (event.getEntity() == rescued) { event.setCanceled(true); rescued.setHealth(1); }
        };
        NeoForge.EVENT_BUS.addListener(listener);
        try { cast(h, p, rescued); assertGrowth(h, p, 0); }
        finally { NeoForge.EVENT_BUS.unregister(listener); }
        var totem = target(h, 5);
        totem.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        cast(h, p, totem);
        h.assertTrue(totem.isAlive(), "Totem failed to rescue test entity");
        assertGrowth(h, p, 0);
        assertRegen(h,p,0);
        cast(h, p, target(h, 5));
        assertGrowth(h, p, 30);
        assertRegen(h,p,0.6);
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void growthSurvivesReloadCloneAndRepeatedRestoration(GameTestHelper h) {
        var p = player(h); cast(h, p, target(h, 5));
        var restored = player(h);
        restored.load(p.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        SoulHarvestGrowth.restore(restored); SoulHarvestGrowth.restore(restored);
        assertGrowth(h, restored, 30);
        assertRegen(h,restored,0.6);
        var clone = player(h);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(clone, restored, true));
        SoulHarvestGrowth.restore(clone);
        assertGrowth(h, clone, 30);
        assertRegen(h,clone,0.6);
        h.assertTrue(clone.getAttribute(PerkAttributes.MANA_REGEN_BONUS).getModifiers().stream()
                .filter(m->m.id().equals(ResourceLocation.parse("ars_misc:soul_harvest_regen"))).count()==1,
                "Regeneration modifier was lost or duplicated on death clone");
        h.assertTrue(Math.abs(clone.getMaxHealth() - 20) < 0.001
                && clone.getAttribute(PerkAttributes.MAX_MANA).getModifiers().stream()
                .filter(m -> m.id().equals(ResourceLocation.parse("ars_misc:soul_harvest_mana"))).count() == 1,
                "Death clone or repeated restoration duplicated or lost growth");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void configurableGainsAffectFutureKillsOnly(GameTestHelper h) {
        double mana = MiscConfig.SOUL_HARVEST_MANA_PER_KILL.get();
        double regen = MiscConfig.SOUL_HARVEST_REGEN_PER_KILL.get();
        var p = player(h);
        try {
            cast(h, p, target(h, 5));
            MiscConfig.SOUL_HARVEST_MANA_PER_KILL.set(10.0);
            MiscConfig.SOUL_HARVEST_REGEN_PER_KILL.set(2.0);
            cast(h, p, target(h, 5));
            assertGrowth(h, p, mana + 10);
            assertRegen(h,p,regen+2);
        } finally {
            MiscConfig.SOUL_HARVEST_MANA_PER_KILL.set(mana);
            MiscConfig.SOUL_HARVEST_REGEN_PER_KILL.set(regen);
        }
        h.succeed();
    }
}
