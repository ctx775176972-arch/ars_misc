package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellDamageBeltGameTests {
    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void equippedBeltClearsOnlySpellHurtCooldownAndRetainsDefenses(GameTestHelper h) {
        var p = ReactiveShieldGameTests.player(h);
        p.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT).setBaseValue(0);
        BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> p.getAttribute(attribute).setBaseValue(0));
        var target = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 4);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.setHealth(100);
        var context = SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, EffectHarm.INSTANCE), p, ItemStack.EMPTY);
        var resolver = new SpellResolver(context);
        var source = EffectHarm.INSTANCE.buildDamageSource(h.getLevel(), p);
        java.util.function.BooleanSupplier hit = () -> EffectHarm.INSTANCE.attemptDamage(h.getLevel(), p,
                new SpellStats.Builder().build(), context, resolver, target, source, 5);
        h.assertTrue(hit.getAsBoolean() && !hit.getAsBoolean(), "Unequipped spell ignored hurt cooldown");
        var inv = CuriosApi.getCuriosInventory(p).orElseThrow();
        inv.setEquippedCurio("belt", 0, ArsMiscMod.CRITICAL_BELT.get().getDefaultInstance());
        float before = target.getHealth();
        h.assertTrue(hit.getAsBoolean() && hit.getAsBoolean() && Math.abs(before-target.getHealth()-10)<0.001,
                "Equipped belt did not allow repeated full spell hits");
        inv.setSlotActive("belt", 0, false);
        h.assertTrue(!hit.getAsBoolean(), "Inactive belt cleared hurt cooldown");
        inv.setSlotActive("belt", 0, true);
        h.assertTrue(!target.hurt(h.getLevel().damageSources().playerAttack(p), 5),
                "Belt also bypassed cooldown for ordinary melee");
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
        before = target.getHealth();
        h.assertTrue(hit.getAsBoolean() && Math.abs(before-target.getHealth()-4)<0.001,
                "Belt bypassed Resistance");
        target.setInvulnerable(true);
        before = target.getHealth();
        h.assertTrue(!hit.getAsBoolean() && target.getHealth()==before, "Belt bypassed actual entity invulnerability");
        h.succeed();
    }
}
