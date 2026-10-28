package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class EquippedBookGameTests {
    private static com.hollingsworth.arsnouveau.common.block.tile.ScribesTile table(GameTestHelper h) {
        var state = com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SCRIBES_BLOCK.get().defaultBlockState()
                .setValue(com.hollingsworth.arsnouveau.common.block.ScribesBlock.PART,
                        com.hollingsworth.arsnouveau.common.block.ThreePartBlock.HEAD);
        h.setBlock(new net.minecraft.core.BlockPos(2, 2, 4), state);
        return (com.hollingsworth.arsnouveau.common.block.tile.ScribesTile) h.getLevel()
                .getBlockEntity(h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 4)));
    }

    private static void castAtTable(net.minecraft.server.level.ServerPlayer player) {
        player.getPersistentData().remove("ars_misc:last_book_cast");
        EquippedSpellbook.cast(player, 25, 0);
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void scribingResolvesTableFootAndRespectsRangeAndInteractionDenial(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(EffectRegeneration.INSTANCE)).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var foot = com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SCRIBES_BLOCK.get().defaultBlockState()
                .setValue(com.hollingsworth.arsnouveau.common.block.ScribesBlock.PART,
                com.hollingsworth.arsnouveau.common.block.ThreePartBlock.FOOT)
                .setValue(com.hollingsworth.arsnouveau.common.block.ScribesBlock.FACING, net.minecraft.core.Direction.SOUTH);
        var footPos = h.absolutePos(new net.minecraft.core.BlockPos(2, 2, 4));
        var headPos = footPos.relative(com.hollingsworth.arsnouveau.common.block.ScribesBlock.getConnectedDirection(foot));
        h.getLevel().setBlock(footPos, foot, 2);
        h.getLevel().setBlock(headPos, foot.setValue(com.hollingsworth.arsnouveau.common.block.ScribesBlock.PART,
                com.hollingsworth.arsnouveau.common.block.ThreePartBlock.HEAD), 2);
        var master = (com.hollingsworth.arsnouveau.common.block.tile.ScribesTile) h.getLevel().getBlockEntity(headPos);
        master.setStack(ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance());
        h.assertTrue(((com.hollingsworth.arsnouveau.common.block.tile.ScribesTile) h.getLevel().getBlockEntity(footPos)).getLogicTile() == master,
                "Table fixture does not resolve its head");
        var range = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE);
        double oldRange = range.getBaseValue(); range.setBaseValue(0.5);
        castAtTable(player);
        h.assertTrue(SpellCasterRegistry.from(master.getStack()).getSpell().isEmpty(), "Scribed beyond interaction range");
        range.setBaseValue(oldRange);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock> deny = event -> {
            if (event.getEntity() == player) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try {
            castAtTable(player);
            h.assertTrue(SpellCasterRegistry.from(master.getStack()).getSpell().isEmpty(), "Ignored interaction protection");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny); }
        castAtTable(player);
        h.assertTrue(SpellCasterRegistry.from(master.getStack()).getSpell().size() == 2, "Table foot did not resolve master");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void equippedBookScribesNativeSwordWithoutCastingOrReplacingHands(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        // Sword scribing accepts effects without a form, even though that is not a castable book spell.
        SpellCasterRegistry.from(book).setSpell(new Spell(EffectRegeneration.INSTANCE), 2).setCurrentSlot(2).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var held = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var shield = new ItemStack(Items.SHIELD); player.setItemInHand(InteractionHand.OFF_HAND, shield);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(1000); mana.setMana(500);
        var table = table(h); var sword = ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance(); table.setStack(sword);
        castAtTable(player);
        var spell = SpellCasterRegistry.from(table.getStack()).getSpell();
        h.assertTrue(spell.size() == 2
                && spell.get(0) == com.hollingsworth.arsnouveau.common.spell.method.MethodTouch.INSTANCE
                && spell.get(1) == EffectRegeneration.INSTANCE, "Native sword did not scribe the equipped selected recipe: " + spell.recipe()
                + "; source=" + SpellCasterRegistry.from(EquippedSpellbook.find(player)).getSpell().recipe());
        h.assertTrue(mana.getCurrentMana() == 500 && !player.hasEffect(MobEffects.REGENERATION), "Scribing cast or spent mana");
        h.assertTrue(player.getMainHandItem() == held && player.getOffhandItem() == shield, "Scribing source leaked into hands");
        h.assertTrue(SpellCasterRegistry.from(held).getSpell().isEmpty(), "Held book changed");
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getCurrentSlot() == 2, "Scribing changed selected slot");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void rejectedAndCanceledScribingNeverCastAndEmptyTableStillCasts(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE)).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var held = new ItemStack(Items.STICK); player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(1000); mana.setMana(500);
        var table = table(h); table.setStack(ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance());
        castAtTable(player);
        h.assertTrue(!player.hasEffect(MobEffects.REGENERATION) && mana.getCurrentMana() == 500, "Rejected scribing fell through into casting");
        java.util.function.Consumer<com.hollingsworth.arsnouveau.api.event.ScribesTableInteractEvent> cancel = event -> {
            if (event.player == player) event.setCanceled(true);
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try {
            SpellCasterRegistry.from(book).setSpell(new Spell(EffectRegeneration.INSTANCE)).saveToStack(book);
            castAtTable(player);
            h.assertTrue(SpellCasterRegistry.from(table.getStack()).getSpell().isEmpty(), "Canceled scribing changed target");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel); }
        h.assertTrue(player.getMainHandItem() == held, "Canceled scribing leaked book source");
        table.setStack(ItemStack.EMPTY);
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE)).saveToStack(book);
        castAtTable(player);
        h.assertTrue(player.hasEffect(MobEffects.REGENERATION) && mana.getCurrentMana() < 500, "Empty table blocked ordinary casting");
        h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ars-book-test");
        var player = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        return player;
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void selectionUsesEquippedBookAndRejectsInvalidSlots(GameTestHelper h) {
        var player = player(h);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var held = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        EquippedSpellbook.select(player, 3, 2);
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getCurrentSlot() == 2, "Equipped selection failed");
        h.assertTrue(SpellCasterRegistry.from(held).getCurrentSlot() == 0, "Held book changed");
        EquippedSpellbook.select(player, 3, Integer.MAX_VALUE);
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getCurrentSlot() == 2, "Invalid slot accepted");
        EquippedSpellbook.select(player, 2, 0);
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getCurrentSlot() == 1, "Previous slot failed");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void equippedCastSpendsNativeManaAndPreservesHandsAndRotation(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE)).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var sword = new ItemStack(Items.DIAMOND_SWORD); var offhand = new ItemStack(Items.APPLE);
        player.setItemInHand(InteractionHand.MAIN_HAND, sword); player.setItemInHand(InteractionHand.OFF_HAND, offhand);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(1000); mana.setMana(500);
        player.setXRot(10); player.setYRot(20); player.setYHeadRot(30);
        EquippedSpellbook.cast(player, -40, 80);
        h.assertTrue(player.hasEffect(MobEffects.REGENERATION) && mana.getCurrentMana() < 500, "Native cast failed");
        h.assertTrue(player.getMainHandItem() == sword && player.getOffhandItem() == offhand, "Hands replaced");
        h.assertTrue(player.getXRot() == 10 && player.getYRot() == 20 && player.getYHeadRot() == 30, "Rotation leaked");
        double after = mana.getCurrentMana(); EquippedSpellbook.cast(player, 0, 0);
        h.assertTrue(mana.getCurrentMana() == after, "Repeated packet bypassed short cooldown");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void emptyBookAndInsufficientManaDoNotCast(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        EquippedSpellbook.cast(player, 0, 0);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE)).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        CapabilityRegistry.getMana(player).setMana(0);
        EquippedSpellbook.cast(player, Float.NaN, 0);
        EquippedSpellbook.cast(player, 0, 0);
        h.assertTrue(!player.hasEffect(MobEffects.REGENERATION), "Insufficient mana still cast");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void editorSavesOnlyBoundBookAndRejectsReplacement(GameTestHelper h) {
        var player = player(h);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        var held = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var inventory = CuriosApi.getCuriosInventory(player).orElseThrow();
        inventory.setEquippedCurio("spellbook", 0, book);
        book = EquippedSpellbook.find(player);
        dev.arsmisc.spell.EquippedBookEditing.begin(player);
        var spell = new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE);
        new com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster(spell, 1, "Equipped", true)
                .onServerReceived(h.getLevel().getServer(), player);
        h.assertTrue(SpellCasterRegistry.from(book).getSpell(1).size() == 2 && SpellCasterRegistry.from(held).getSpell(1).isEmpty(), "Editor wrote wrong book");
        new com.hollingsworth.arsnouveau.common.network.PacketSetCasterSlot(2).onServerReceived(h.getLevel().getServer(), player);
        h.assertTrue(SpellCasterRegistry.from(book).getCurrentSlot() == 2 && SpellCasterRegistry.from(held).getCurrentSlot() == 0, "Editor selection changed held book");
        inventory.setEquippedCurio("spellbook", 0, ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance());
        new com.hollingsworth.arsnouveau.common.network.PacketUpdateCaster(spell, 1, "Stale", true)
                .onServerReceived(h.getLevel().getServer(), player);
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getSpell(1).isEmpty(), "Stale editor wrote replacement");
        dev.arsmisc.spell.EquippedBookEditing.end(player);
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void nativeQuickCastUsesEquippedSlotWithoutChangingPreparedSpell(GameTestHelper h) {
        var player = player(h); player.setGameMode(GameType.SURVIVAL);
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE, EffectRegeneration.INSTANCE), 1).saveToStack(book);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("spellbook", 0, book);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(1000); mana.setMana(500);
        new com.hollingsworth.arsnouveau.common.network.PacketQuickCast(1, 0, 0).onServerReceived(h.getLevel().getServer(), player);
        h.assertTrue(player.hasEffect(MobEffects.REGENERATION) && mana.getCurrentMana() < 500, "Native quick cast did not use equipped recipe");
        h.assertTrue(SpellCasterRegistry.from(EquippedSpellbook.find(player)).getCurrentSlot() == 0, "Quick cast changed prepared spell");
        player.removeEffect(MobEffects.REGENERATION);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance());
        new com.hollingsworth.arsnouveau.common.network.PacketQuickCast(1, 0, 0).onServerReceived(h.getLevel().getServer(), player);
        h.assertTrue(!player.hasEffect(MobEffects.REGENERATION), "Equipped quick cast overrode held book");
        h.succeed();
    }
}
