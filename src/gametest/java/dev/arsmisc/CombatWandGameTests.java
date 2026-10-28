package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAccelerate;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.setup.registry.*;
import dev.arsmisc.item.CombatWandItem;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class CombatWandGameTests {
    private static dev.arsmisc.recipe.WandConversionRecipe conversion(GameTestHelper h, boolean combat) {
        return (dev.arsmisc.recipe.WandConversionRecipe) h.getLevel().getRecipeManager().byKey(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ars_misc", combat ? "combat_wand" : "restore_enchanters_wand")).orElseThrow().value();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void shapelessConversionRoundTripPreservesAllWandData(GameTestHelper h) {
        var original = ItemsRegistry.WAND.get().getDefaultInstance();
        SpellCasterRegistry.from(original).setSpell(new Spell(MethodProjectile.INSTANCE,AugmentAccelerate.INSTANCE,EffectHarm.INSTANCE)).setSpellName("Main attack").saveToStack(original);
        original.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My Wand"));
        original.set(net.minecraft.core.component.DataComponents.BASE_COLOR,net.minecraft.world.item.DyeColor.BLUE);
        var tag = new net.minecraft.nbt.CompoundTag(); tag.putString("external_data","retained");
        original.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        var forward = conversion(h,true); var reverse = conversion(h,false);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2,2,java.util.List.of(ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY,original));
        h.assertTrue(forward.matches(input,h.getLevel()) && !reverse.matches(input,h.getLevel()),"Forward shapeless recipe mismatch");
        var combat = forward.assemble(input,h.getLevel().registryAccess());
        h.assertTrue(combat.is(ArsMiscMod.COMBAT_WAND.get()) && combat.getComponentsPatch().equals(original.getComponentsPatch()),"Conversion lost wand components");
        var backInput = net.minecraft.world.item.crafting.CraftingInput.of(1,1,java.util.List.of(combat));
        h.assertTrue(reverse.matches(backInput,h.getLevel()),"Reverse recipe missing");
        var restored = reverse.assemble(backInput,h.getLevel().registryAccess());
        h.assertTrue(ItemStack.isSameItemSameComponents(original,restored) && original.is(ItemsRegistry.WAND.get()),"Round trip changed original data");
        var invalid = net.minecraft.world.item.crafting.CraftingInput.of(2,1,java.util.List.of(original,new ItemStack(Items.STICK)));
        h.assertTrue(!forward.matches(invalid,h.getLevel()),"Extra ingredients accepted"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void conversionRecipeSyncPreservesClientRecipeDisplay(GameTestHelper h) {
        var recipe = conversion(h,true);
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            dev.arsmisc.recipe.WandConversionRecipe.STREAM_CODEC.encode(buffer,recipe);
            var decoded = dev.arsmisc.recipe.WandConversionRecipe.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.getResultItem(h.getLevel().registryAccess()).is(ArsMiscMod.COMBAT_WAND.get())
                    && decoded.getIngredients().size()==1 && !decoded.isSpecial(),"Recipe display or network sync failed");
        } finally { buffer.release(); }
        h.succeed();
    }
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "wand-test");
        var p = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(), new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
        p.setGameMode(GameType.SURVIVAL); p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,2)));
        var mana = CapabilityRegistry.getMana(p); mana.setMaxMana(1000); mana.setMana(500);
        return p;
    }
    private static ItemStack wand() {
        var wand = ArsMiscMod.COMBAT_WAND.get().getDefaultInstance();
        SpellCasterRegistry.from(wand).setSpell(new Spell(MethodProjectile.INSTANCE, AugmentAccelerate.INSTANCE, EffectHarm.INSTANCE)).saveToStack(wand);
        return wand;
    }
    private static long shots(GameTestHelper h, net.minecraft.server.level.ServerPlayer player) {
        return h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, player.getBoundingBox().inflate(4)).stream().filter(shot -> shot.getOwner() == player).count();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void inscriptionPreservesCompleteSpellAndRejectsMissingForm(GameTestHelper h) {
        var p = player(h); var wand = ArsMiscMod.COMBAT_WAND.get().getDefaultInstance();
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        p.setItemInHand(InteractionHand.MAIN_HAND,book);
        for(var original:java.util.List.of(new Spell(MethodProjectile.INSTANCE,EffectHarm.INSTANCE),
                new Spell(MethodProjectile.INSTANCE,AugmentAccelerate.INSTANCE,EffectHarm.INSTANCE),
                new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE),new Spell(MethodSelf.INSTANCE,EffectRegeneration.INSTANCE))) {
            SpellCasterRegistry.from(book).setSpell(original).saveToStack(book);
            h.assertTrue(ArsMiscMod.COMBAT_WAND.get().onScribe(h.getLevel(),net.minecraft.core.BlockPos.ZERO,p,InteractionHand.MAIN_HAND,wand),"Valid full spell rejected");
            h.assertTrue(SpellCasterRegistry.from(wand).getSpell().unsafeList().equals(original.unsafeList()),"Inscription changed spell form or augments");
        }
        SpellCasterRegistry.from(book).setSpell(new Spell(EffectHarm.INSTANCE)).saveToStack(book);
        h.assertTrue(!ArsMiscMod.COMBAT_WAND.get().onScribe(h.getLevel(),net.minecraft.core.BlockPos.ZERO,p,InteractionHand.MAIN_HAND,wand),"Accepted spell without cast form");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void selfAndTouchCastWhileShieldRemainsRaised(GameTestHelper h) {
        var p=player(h);var wand=wand();var shield=ItemsRegistry.ENCHANTERS_SHIELD.get().getDefaultInstance();
        p.setItemInHand(InteractionHand.MAIN_HAND,wand);p.setItemInHand(InteractionHand.OFF_HAND,shield);p.startUsingItem(InteractionHand.OFF_HAND);
        SpellCasterRegistry.from(wand).setSpell(new Spell(MethodSelf.INSTANCE,EffectRegeneration.INSTANCE)).saveToStack(wand);
        CombatWandItem.cast(p,0,0);
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION)&&p.isUsingItem()&&p.getUseItem()==shield,
                "Self spell failed or interrupted shield");
        var target=h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,2,2,4);float health=target.getHealth();
        target.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2,2,4)));
        p.getPersistentData().remove("ars_misc:last_combat_wand_cast");
        SpellCasterRegistry.from(wand).setSpell(new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE)).saveToStack(wand);
        CombatWandItem.cast(p,10,0);
        h.assertTrue(target.getHealth()<health&&p.isUsingItem()&&p.getUseItem()==shield,"Touch spell failed or interrupted shield");
        h.assertTrue(shots(h,p)==0,"Non-projectile form spawned a projectile");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void fixedDiscountOnlyAppliesToActualWandAndStacksWithArmor(GameTestHelper h) {
        var p=player(h);var wand=wand();p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        var book=ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        var spell=new Spell(MethodSelf.INSTANCE,EffectCrystalShield.INSTANCE);
        var regular=new com.hollingsworth.arsnouveau.api.spell.SpellResolver(
                com.hollingsworth.arsnouveau.api.spell.SpellContext.fromEntity(spell,p,book));
        var combat=new com.hollingsworth.arsnouveau.api.spell.SpellResolver(
                com.hollingsworth.arsnouveau.api.spell.SpellContext.fromEntity(spell,p,wand));
        int base=regular.getResolveCost();
        h.assertTrue(combat.getResolveCost()==base-20&&combat.getExpendedCost()==base-20,
                "Wand quote or expenditure lacks discount: base="+base+", quote="+combat.getResolveCost()+", spent="+combat.getExpendedCost());
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("casting_device",0,wand.copy());
        h.assertTrue(regular.getResolveCost()==base&&combat.getResolveCost()==base-20,"Held or worn wand leaked or duplicated discount");
        for(var piece:java.util.List.of(ArsMiscMod.NETHERITE_MAGE_HELMET.get(),ArsMiscMod.NETHERITE_MAGE_CHESTPLATE.get(),
                ArsMiscMod.NETHERITE_MAGE_LEGGINGS.get(),ArsMiscMod.NETHERITE_MAGE_BOOTS.get()))
            p.setItemSlot(piece.getEquipmentSlot(),piece.getDefaultInstance());
        h.assertTrue(regular.getResolveCost()==base-40&&combat.getResolveCost()==base-60,"Wand discount did not stack with armor");
        double before=CapabilityRegistry.getMana(p).getCurrentMana();int cost=combat.getExpendedCost();combat.expendMana();
        h.assertTrue(Math.abs(CapabilityRegistry.getMana(p).getCurrentMana()-before+cost)<.001,"Actual wand mana payment differed from quote");
        var cheap=new com.hollingsworth.arsnouveau.api.spell.SpellResolver(com.hollingsworth.arsnouveau.api.spell.SpellContext.fromEntity(
                new Spell(MethodSelf.INSTANCE,EffectRegeneration.INSTANCE),p,wand));
        h.assertTrue(cheap.getResolveCost()==0&&cheap.getExpendedCost()==0,"Discounted cost did not clamp at zero");h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void castsWhileBlockingWithoutStoppingShieldOrReplacingHands(GameTestHelper h) {
        var p = player(h); var wand = wand(); var shield = ItemsRegistry.ENCHANTERS_SHIELD.get().getDefaultInstance();
        p.setItemInHand(InteractionHand.MAIN_HAND,wand); p.setItemInHand(InteractionHand.OFF_HAND,shield);
        p.startUsingItem(InteractionHand.OFF_HAND);
        p.setXRot(10); p.setYRot(20); p.setYHeadRot(30);
        h.assertTrue(ArsMiscMod.COMBAT_WAND.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult()==net.minecraft.world.InteractionResult.PASS, "Wand consumed right click");
        CombatWandItem.cast(p,0,0);
        h.assertTrue(shots(h,p)==1 && CapabilityRegistry.getMana(p).getCurrentMana()<500, "Native projectile or mana charge missing");
        h.assertTrue(p.isUsingItem() && p.getUsedItemHand()==InteractionHand.OFF_HAND && p.getUseItem()==shield && p.getMainHandItem()==wand, "Casting interrupted shield or replaced hands");
        h.assertTrue(p.getXRot()==10 && p.getYRot()==20 && p.getYHeadRot()==30, "Casting changed rotation");
        double mana = CapabilityRegistry.getMana(p).getCurrentMana(); CombatWandItem.cast(p,0,0);
        h.assertTrue(shots(h,p)==1 && CapabilityRegistry.getMana(p).getCurrentMana()==mana, "Duplicate packet bypassed interval");
        p.getPersistentData().remove("ars_misc:last_combat_wand_cast");
        CapabilityRegistry.getMana(p).setMana(0); CombatWandItem.cast(p,0,0);
        h.assertTrue(shots(h,p)==1, "Cast without mana"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void noMainhandWandNoCastAndPhysicalAttacksAreCancelled(GameTestHelper h) {
        var p = player(h); p.setItemInHand(InteractionHand.OFF_HAND,wand());
        CombatWandItem.cast(p,0,0); h.assertTrue(shots(h,p)==0, "Offhand or absent wand accepted");
        var wand = wand(); p.setItemInHand(InteractionHand.MAIN_HAND,wand);
        CombatWandItem.cast(p,Float.NaN,0); h.assertTrue(shots(h,p)==0, "Invalid rotation accepted");
        var cow = h.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.COW,1,2,1);
        var attack = new net.neoforged.neoforge.event.entity.player.AttackEntityEvent(p,cow);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(attack);
        h.assertTrue(attack.isCanceled(), "Physical melee allowed");
        var mining = new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock(p,h.absolutePos(new net.minecraft.core.BlockPos(1,1,1)),net.minecraft.core.Direction.UP,
                net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(mining);
        h.assertTrue(mining.isCanceled(), "Physical mining allowed"); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "ars_misc")
    public static void equippedBookCastRemainsIndependentOfWand(GameTestHelper h) {
        var p = player(h); p.setItemInHand(InteractionHand.MAIN_HAND,wand());
        var book = ItemsRegistry.NOVICE_SPELLBOOK.get().getDefaultInstance();
        SpellCasterRegistry.from(book).setSpell(new Spell(MethodSelf.INSTANCE,EffectRegeneration.INSTANCE)).saveToStack(book);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("spellbook",0,book);
        CombatWandItem.cast(p,0,0);
        dev.arsmisc.spell.EquippedSpellbook.cast(p,0,0);
        h.assertTrue(shots(h,p)==1 && p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), "Wand and equipped book interfered"); h.succeed();
    }
}
