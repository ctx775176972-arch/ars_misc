package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectWindshear;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsmisc.item.EnchantersFieldItem;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class SpellPenetrationGameTests {
    private static Holder<Attribute> attribute(String id) {
        return BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("apothic_attributes:" + id)).orElseThrow();
    }
    private static boolean installed() {
        return BuiltInRegistries.ATTRIBUTE.containsKey(ResourceLocation.parse("apothic_attributes:prot_pierce"));
    }
    private static void equip(ServerPlayer player, String id, double amount) {
        var stack = new ItemStack(Items.LEATHER_CHESTPLATE);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                .add(attribute(id), new AttributeModifier(ResourceLocation.parse("ars_misc:test_penetration"),
                        amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST).build());
        player.setItemSlot(EquipmentSlot.CHEST, stack);
        player.doTick();
    }
    private static LivingEntity target(GameTestHelper h, boolean protection, boolean armor) {
        var entity = h.spawnWithNoFreeWill(EntityType.HUSK, 2, 2, 4);
        entity.getAttribute(Attributes.ARMOR).setBaseValue(armor ? 20 : 0);
        entity.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0);
        if (protection) {
            var chest = new ItemStack(Items.LEATHER_CHESTPLATE);
            // An empty component falls back to the armor item's default modifiers.
            chest.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(Attributes.ARMOR, new AttributeModifier(ResourceLocation.parse("ars_misc:test_zero_armor"),
                            0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.CHEST).build());
            chest.enchant(h.getLevel().holderOrThrow(Enchantments.PROTECTION), 10);
            entity.setItemSlot(EquipmentSlot.CHEST, chest);
        }
        entity.tick();
        h.assertTrue(entity.getArmorValue() == (armor ? 20 : 0), "Test target acquired unintended armor");
        entity.setHealth(entity.getMaxHealth());
        return entity;
    }
    private static float hit(GameTestHelper h, ServerPlayer player, boolean protection, boolean armor, boolean wind) {
        var target = target(h, protection, armor);
        target.setOnGround(false);
        AbstractEffect effect = wind ? EffectWindshear.INSTANCE : EffectHarm.INSTANCE;
        var spell = new Spell(MethodTouch.INSTANCE, effect);
        var context = SpellContext.fromEntity(spell, player, ItemStack.EMPTY);
        float before = target.getHealth();
        effect.onResolveEntity(new EntityHitResult(target), h.getLevel(), player,
                new SpellStats.Builder().build(), context, new SpellResolver(context));
        h.assertTrue(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == player,
                "Spell did not preserve the real player as attacker");
        float damage = before - target.getHealth();
        target.discard();
        return damage;
    }
    private static ServerPlayer player(GameTestHelper h) {
        var player = ReactiveShieldGameTests.player(h);
        player.getAttribute(com.alexthw.sauce.registry.ModRegistry.SPELL_CRIT).setBaseValue(0);
        // Apotheosis has a separate critical-strike roll, including spell damage.
        player.getAttribute(attribute("crit_chance")).setBaseValue(0);
        return player;
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void equippedProtectionPierceAndShredIncreaseNativeHarmAndWindshear(GameTestHelper h) {
        if (!installed()) { h.succeed(); return; }
        var p = player(h);
        for (boolean wind : new boolean[]{false, true}) {
            float raw = hit(h, p, false, false, wind);
            float protectedDamage = hit(h, p, true, false, wind);
            equip(p, "prot_pierce", 5);
            h.assertTrue(p.getAttributeValue(attribute("prot_pierce")) == 5, "Worn equipment attribute was not applied");
            float pierce = hit(h, p, true, false, wind);
            equip(p, "prot_shred", 0.5);
            float shred = hit(h, p, true, false, wind);
            equip(p, "prot_pierce", 10);
            float full = hit(h, p, true, false, wind);
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); p.doTick();
            float removed = hit(h, p, true, false, wind);
            ArsMiscMod.LOGGER.info("Spell penetration test: wind={}, raw={}, protection={}, pierce={}, shred={}, full={}, unequipped={}",
                    wind, raw, protectedDamage, pierce, shred, full, removed);
            h.assertTrue(protectedDamage > 0 && protectedDamage < pierce && pierce < raw
                    && Math.abs(pierce - shred) < 0.001 && Math.abs(full - raw) < 0.001
                    && Math.abs(removed - protectedDamage) < 0.001,
                    "Protection penetration failed: wind=" + wind + " raw=" + raw + " protected=" + protectedDamage
                            + " pierce=" + pierce + " shred=" + shred + " full=" + full + " removed=" + removed);
        }
        h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void equippedArmorPenetrationAffectsHarmWhileWindshearAlreadyBypassesArmor(GameTestHelper h) {
        if (!installed()) { h.succeed(); return; }
        var p = player(h);
        float raw = hit(h, p, false, false, false);
        float armored = hit(h, p, false, true, false);
        equip(p, "armor_pierce", 20);
        float pierced = hit(h, p, false, true, false);
        equip(p, "armor_shred", 1);
        float shredded = hit(h, p, false, true, false);
        p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); p.doTick();
        float windRaw = hit(h, p, false, false, true);
        float windArmored = hit(h, p, false, true, true);
        h.assertTrue(armored < raw && Math.abs(pierced - raw) < 0.001 && Math.abs(shredded - raw) < 0.001
                && Math.abs(windRaw - windArmored) < 0.001, "Armor penetration or native armor bypass failed");
        h.succeed();
    }

    @GameTest(template="combat_arena", templateNamespace="ars_misc")
    public static void equippedBookAndAutomaticFieldUseWearersProtectionPenetration(GameTestHelper h) {
        if (!installed()) { h.succeed(); return; }
        var p = player(h);
        equip(p, "prot_pierce", 10);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodTouch.INSTANCE, EffectHarm.INSTANCE)).saveToStack(book);
        CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var victim = target(h, true, false);
        p.setPos(victim.getX(), victim.getY(), victim.getZ() - 2);
        var mana = CapabilityRegistry.getMana(p); mana.setMaxMana(1000); mana.setMana(500);
        float before = victim.getHealth();
        EquippedSpellbook.cast(p, 0, 0);
        h.assertTrue(victim.getLastDamageSource() != null && victim.getLastDamageSource().getEntity() == p,
                "Equipped book failed to target the victim or lost the player");
        float bookDamage = before - victim.getHealth(); victim.discard();
        var field = ArsMiscMod.ENCHANTERS_FIELD.get().getDefaultInstance();
        new SpellCaster().setSpell(new Spell(EffectHarm.INSTANCE)).saveToStack(field);
        victim = target(h, true, false); before = victim.getHealth(); p.tickCount = 0;
        ArsMiscMod.ENCHANTERS_FIELD.get().curioTick(new SlotContext("casting_device", p, 0, false, true), field);
        h.assertTrue(victim.getLastDamageSource() != null && victim.getLastDamageSource().getEntity() == p,
                "Automatic field lost the real player as attacker");
        float fieldDamage = before - victim.getHealth(); victim.discard();
        float raw = hit(h, p, false, false, false);
        ArsMiscMod.LOGGER.info("Equipped casting penetration test: book={}, field={}, raw={}", bookDamage, fieldDamage, raw);
        h.assertTrue(Math.abs(bookDamage - raw) < 0.001 && Math.abs(fieldDamage - raw) < 0.001,
                "Equipped casting did not inherit worn protection penetration: book=" + bookDamage
                        + " field=" + fieldDamage + " raw=" + raw);
        h.succeed();
    }
}
