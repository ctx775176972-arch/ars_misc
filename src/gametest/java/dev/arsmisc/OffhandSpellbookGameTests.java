package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.network.PacketSetCasterSlot;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsmisc.spell.HeldSpellbookHotkeys;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class OffhandSpellbookGameTests {
    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void emptyToolEditorFallsBackToOffhandAndRealEditorKeepsPriority(GameTestHelper h) {
        var p = ReactiveShieldGameTests.player(h);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance());
        for (var item : new Item[]{Items.DIAMOND_SWORD, ItemsRegistry.ENCHANTERS_SWORD.get(),
                ItemsRegistry.WAND.get(), ArsMiscMod.COMBAT_WAND.get(), Items.SHIELD}) {
            p.setItemInHand(InteractionHand.MAIN_HAND, item.getDefaultInstance());
            h.assertTrue(HeldSpellbookHotkeys.useOffhandEditor(p), "Tool swallowed the offhand editor: " + item);
        }
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance());
        h.assertTrue(!HeldSpellbookHotkeys.useOffhandEditor(p), "Main-hand spellbook lost editor priority");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        h.assertTrue(!HeldSpellbookHotkeys.useOffhandEditor(p), "Fallback activated without an offhand book");
        h.succeed();
    }

    @GameTest(template="empty", templateNamespace="ars_misc")
    public static void nativeSelectionAndEditorPacketsUpdateOffhandWithoutChangingSword(GameTestHelper h) {
        var p = ReactiveShieldGameTests.player(h);
        var sword = ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance();
        SpellCasterRegistry.from(sword).setSpell(new Spell(MethodTouch.INSTANCE, EffectHarm.INSTANCE)).saveToStack(sword);
        var savedSword = sword.copy();
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        p.setItemInHand(InteractionHand.MAIN_HAND, sword);
        p.setItemInHand(InteractionHand.OFF_HAND, book);
        new PacketSetCasterSlot(2).onServerReceived(h.getLevel().getServer(), p);
        h.assertTrue(SpellCasterRegistry.from(book).getCurrentSlot() == 2, "Radial selection missed offhand book");
        var spell = new Spell(MethodTouch.INSTANCE, com.hollingsworth.arsnouveau.common.spell.effect.EffectCut.INSTANCE);
        new PacketUpdateCaster(spell, 2, "Offhand spell", false).onServerReceived(h.getLevel().getServer(), p);
        var caster = SpellCasterRegistry.from(book);
        h.assertTrue(caster.getSpell(2).recipe().equals(spell.recipe()) && caster.getSpellName().equals("Offhand spell"),
                "Native editor did not save the offhand spell");
        h.assertTrue(ItemStack.isSameItemSameComponents(sword, savedSword), "Offhand editing changed the main-hand sword");
        h.succeed();
    }
}
