package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import com.hollingsworth.arsnouveau.api.perk.PerkAttributes;
import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.util.DamageUtil;
import com.hollingsworth.arsnouveau.common.crafting.recipes.ApparatusRecipeInput;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentFortune;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsmisc.recipe.SwordUpgradeRecipe;
import dev.arsmisc.spell.SwordSpellDamage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("ars_misc") @PrefixGameTestTemplate(false)
public final class NetheriteSwordGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var p=ReactiveShieldGameTests.player(h);
        p.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(0);
        BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse("apothic_attributes:crit_chance"))
                .ifPresent(attribute -> p.getAttribute(attribute).setBaseValue(0));
        p.setItemInHand(InteractionHand.MAIN_HAND,ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get().getDefaultInstance());
        p.doTick();return p;
    }
    private static LivingEntity target(GameTestHelper h,boolean armor) {
        var t=h.spawnWithNoFreeWill(EntityType.HUSK,2,2,4);
        t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.setHealth(1000);
        t.getAttribute(Attributes.ARMOR).setBaseValue(armor?30:0);
        t.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(armor?20:0);
        return t;
    }
    private static SpellContext context(ServerPlayer p,boolean marked) {
        var c=SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE),p,p.getMainHandItem());
        if (marked) SwordSpellDamage.mark(c,p);
        return c;
    }
    private static float hit(GameTestHelper h,ServerPlayer p,LivingEntity target,SpellContext context) {
        float before=target.getHealth();
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),p,new SpellStats.Builder().build(),context,
                new SpellResolver(context),target,EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),p),5);
        return before-target.getHealth();
    }
    private static void close(GameTestHelper h,double actual,double expected,String message) {
        h.assertTrue(Math.abs(actual-expected)<.001,message+": "+actual+" != "+expected);
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void emptySwordUsesTheSameNativeValidationMessageAndSpendsNoMana(GameTestHelper h) {
        var p=player(h);
        var packets=new java.util.ArrayList<net.minecraft.network.protocol.Packet<?>>();
        p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(),false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) { packets.add(packet); }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener) { packets.add(packet); }
        };
        var nativeSword=com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.ENCHANTERS_SWORD.get();
        var target=target(h,false);
        p.setItemInHand(InteractionHand.MAIN_HAND,nativeSword.getDefaultInstance());
        nativeSword.hurtEnemy(p.getMainHandItem(),target,p);
        var nativeMessage=emptySpellMessage(h,packets);packets.clear();
        p.setItemInHand(InteractionHand.MAIN_HAND,ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get().getDefaultInstance());
        double mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p).getCurrentMana();
        ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get().hurtEnemy(p.getMainHandItem(),target,p);
        h.assertTrue(nativeMessage.equals(emptySpellMessage(h,packets)),"Empty new sword does not send native empty-spell feedback");
        close(h,com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p).getCurrentMana(),mana,"Empty sword spent mana");
        close(h,target.getHealth(),1000,"Empty spell damaged target");h.succeed();
    }
    private static net.minecraft.network.chat.Component emptySpellMessage(GameTestHelper h,java.util.List<net.minecraft.network.protocol.Packet<?>> packets) {
        var packet=packets.stream().filter(p->p instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket payload
                &&payload.payload() instanceof com.hollingsworth.arsnouveau.common.network.PacketNoSpamChatMessage)
                .map(p->(com.hollingsworth.arsnouveau.common.network.PacketNoSpamChatMessage)
                        ((net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket)p).payload()).findFirst().orElseThrow();
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
        try {packet.toBytes(buffer);return net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buffer);}
        finally {buffer.release();}
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void vanillaNetheriteStatsAndMainhandMultiplierReplaceFlatPower(GameTestHelper h) {
        var p=player(h);var sword=p.getMainHandItem();
        close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),8,"Attack damage");
        close(h,p.getAttributeValue(Attributes.ATTACK_SPEED),1.6,"Attack speed");
        close(h,p.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS),0,"Old flat spell power retained");
        close(h,p.getAttributeValue(ModRegistry.SPELL_DAMAGE_MULTIPLIER),1.15,"Spell multiplier");
        h.assertTrue(sword.getMaxDamage()==Items.NETHERITE_SWORD.getDefaultInstance().getMaxDamage()
                && sword.has(DataComponents.FIRE_RESISTANT),"Wrong netherite durability/fire resistance");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,sword);p.doTick();
        close(h,p.getAttributeValue(ModRegistry.SPELL_DAMAGE_MULTIPLIER),1,"Offhand grants multiplier");
        h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void actualSwordHitCastsTouchAndPaysNativeMana(GameTestHelper h) {
        var p=player(h);var item=ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get();var stack=p.getMainHandItem();
        var effectOnly=new Spell(EffectHarm.INSTANCE);
        h.assertTrue(item.isScribedSpellValid(item.getSpellCaster(stack),p,InteractionHand.MAIN_HAND,stack,effectOnly)
                && !item.isScribedSpellValid(item.getSpellCaster(stack),p,InteractionHand.MAIN_HAND,stack,new Spell(MethodProjectile.INSTANCE,EffectHarm.INSTANCE)),"Sword scribing rules changed");
        var mutable=effectOnly.mutable();item.scribeModifiedSpell(item.getSpellCaster(stack),p,InteractionHand.MAIN_HAND,stack,mutable);
        h.assertTrue(mutable.recipe.equals(List.of(MethodTouch.INSTANCE,EffectHarm.INSTANCE)),"Unexpected shape or free augment");
        var spell=new Spell(mutable.recipe);SpellCasterRegistry.from(stack).setSpell(spell).saveToStack(stack);
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p);
        mana.setMana(500);var c=SpellContext.fromEntity(spell,p,stack);int cost=new SpellResolver(c).getExpendedCost();
        var t=target(h,true);item.hurtEnemy(stack,t,p);
        close(h,1000-t.getHealth(),14.95,"Actual sword-triggered damage");close(h,mana.getCurrentMana(),500-cost,"Native mana expense");
        mana.setMana(0);t=target(h,false);item.hurtEnemy(stack,t,p);
        close(h,t.getHealth(),1000,"Sword cast without mana");
        mana.setMana(500);t=target(h,true);p.attack(t);
        h.assertTrue(mana.getCurrentMana()<500 && t.getHealth()<1000,"Vanilla player attack did not trigger the sword spell");
        h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void onlyMarkedSwordCastsPierceAndInheritAttack(GameTestHelper h) {
        var p=player(h);
        float raw=hit(h,p,target(h,false),context(p,true));
        float armor=hit(h,p,target(h,true),context(p,true));
        float ordinary=hit(h,p,target(h,false),context(p,false));
        float ordinaryArmor=hit(h,p,target(h,true),context(p,false));
        close(h,raw,14.95,"Inherited base damage did not multiply");close(h,armor,raw,"Armor was not bypassed");
        close(h,ordinary,5.75,"Merely holding sword inherited Attack Damage");
        h.assertTrue(ordinaryArmor<ordinary,"Unmarked cast bypassed armor");
        var c=context(p,true);c.setCasterTool(ArsMiscMod.COMBAT_WAND.get().getDefaultInstance());
        close(h,hit(h,p,target(h,false),c),ordinary,"Marker leaked to another tool");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void protectionResistanceSourceAndFortuneRemainNative(GameTestHelper h) {
        var p=player(h);var t=target(h,true);
        t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,0));
        close(h,hit(h,p,t,context(p,true)),14.95*.8,"Resistance was bypassed");
        t=target(h,true);var protectedBook=Items.BOOK.getDefaultInstance();
        protectedBook.enchant(h.getLevel().holderOrThrow(Enchantments.PROTECTION),10);t.setItemSlot(EquipmentSlot.CHEST,protectedBook);
        float protectedDamage=hit(h,p,t,context(p,true));h.assertTrue(protectedDamage>0&&protectedDamage<14.95,"Protection was bypassed");
        t=target(h,true);var c=context(p,true);
        var original=EffectHarm.INSTANCE.buildDamageSource(h.getLevel(),p);
        EffectHarm.INSTANCE.attemptDamage(h.getLevel(),p,new SpellStats.Builder().addAugment(AugmentFortune.INSTANCE).build(),c,
                new SpellResolver(c),t,original,5);
        var source=t.getLastDamageSource();
        h.assertTrue(source.typeHolder().equals(original.typeHolder()) && source.getEntity()==p
                && source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)
                && source instanceof DamageUtil.SpellDamageSource spellSource && spellSource.getLuckLevel()==1,
                "Spell type, attribution, tags or Fortune lost");h.succeed();
    }
    @GameTest(template="combat_arena",templateNamespace="ars_misc")
    public static void inheritedAttackSnapshotsAcrossDelayedChildContextsAndUsesNativeMultipliers(GameTestHelper h) {
        var p=player(h);p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,1));
        close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),14,"Strength did not change Attack Damage");
        var c=context(p,true).clone();
        p.removeEffect(MobEffects.DAMAGE_BOOST);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.doTick();
        close(h,hit(h,p,target(h,true),c),19,"Delayed context lost attack snapshot or tool provenance");
        p.getAttribute(PerkAttributes.SPELL_DAMAGE_BONUS).setBaseValue(4);
        p.getAttribute(ModRegistry.SPELL_DAMAGE_MULTIPLIER).setBaseValue(1.15);
        p.getAttribute(ModRegistry.SPELL_CRIT).setBaseValue(1);p.getAttribute(ModRegistry.SPELL_CRIT_DAMAGE).setBaseValue(1);
        close(h,hit(h,p,target(h,true),c),(5+14+4)*1.15*2,"Power, damage multiplier or critical order changed");h.succeed();
    }
    @GameTest(template="empty",templateNamespace="ars_misc")
    public static void bothUpgradeRecipesPreserveDataAndUseRequestedIngredients(GameTestHelper h) {
        for(var route:List.of("mage","netherite")) {
            var recipe=(SwordUpgradeRecipe)h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("ars_misc:netherite_enchanters_sword_from_"+route)).orElseThrow().value();
            var source=route.equals("mage")?com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry.ENCHANTERS_SWORD.get().getDefaultInstance():Items.NETHERITE_SWORD.getDefaultInstance();
            source.setDamageValue(50);source.enchant(h.getLevel().holderOrThrow(Enchantments.SHARPNESS),5);
            source.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Named sword"));
            if (route.equals("mage")) SpellCasterRegistry.from(source).setSpell(new Spell(MethodTouch.INSTANCE,EffectHarm.INSTANCE)).saveToStack(source);
            var extra=source.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY);
            var attrs=net.minecraft.world.item.component.ItemAttributeModifiers.builder();
            extra.modifiers().forEach(entry -> attrs.add(entry.attribute(),entry.modifier(),entry.slot()));
            attrs.add(Attributes.ATTACK_DAMAGE,new AttributeModifier(ResourceLocation.parse("ars_misc:test_external_attack"),2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
            source.set(DataComponents.ATTRIBUTE_MODIFIERS,attrs.build());
            var materials=recipe.pedestalItems().stream().map(i->i.getItems()[0].copy()).toList();
            var input=new ApparatusRecipeInput(source,materials,null);
            h.assertTrue(recipe.matches(input,h.getLevel(),null)&&recipe.sourceCost()==0,"Valid upgrade rejected");
            h.assertTrue(!recipe.matches(new ApparatusRecipeInput(source,materials.subList(0,materials.size()-1),null),h.getLevel(),null),"Missing ingredient accepted");
            if (route.equals("mage")) h.assertTrue(materials.size()==2&&materials.stream().anyMatch(s->s.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),"Wrong mage materials");
            else h.assertTrue(materials.size()==5&&materials.stream().filter(s->s.is(Items.GOLD_BLOCK)).count()==2
                    &&materials.stream().filter(s->s.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:source_gem_block")))).count()==2
                    &&materials.stream().anyMatch(s->s.is(Items.DIAMOND)),"Wrong netherite materials");
            var result=recipe.assemble(input,h.getLevel().registryAccess());
            h.assertTrue(result.is(ArsMiscMod.NETHERITE_ENCHANTERS_SWORD.get())&&result.getDamageValue()==50&&result.getMaxDamage()==2031
                    &&result.has(DataComponents.FIRE_RESISTANT)&&result.getHoverName().getString().equals("Named sword")
                    &&result.getEnchantmentLevel(h.getLevel().holderOrThrow(Enchantments.SHARPNESS))==5,"Upgrade lost center data");
            if (route.equals("mage")) h.assertTrue(SpellCasterRegistry.from(result).getSpell().recipe().equals(SpellCasterRegistry.from(source).getSpell().recipe()),"Inscription lost");
            var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,result);p.doTick();
            close(h,p.getAttributeValue(Attributes.ATTACK_DAMAGE),10,"Extra attack modifier lost or base duplicated");
            close(h,p.getAttributeValue(PerkAttributes.SPELL_DAMAGE_BONUS),0,"Old +4 spell power survived upgrade");
            close(h,p.getAttributeValue(ModRegistry.SPELL_DAMAGE_MULTIPLIER),1.15,"New multiplier missing after component copy");
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),h.getLevel().registryAccess());
            try {SwordUpgradeRecipe.STREAM_CODEC.encode(buffer,recipe);h.assertTrue(SwordUpgradeRecipe.STREAM_CODEC.decode(buffer).matches(input,h.getLevel(),null),"Recipe sync mismatch");}finally{buffer.release();}
        }
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse("ars_misc:glyph_arcane_pierce"))
                &&h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("ars_misc:glyph_arcane_pierce")).isEmpty(),"Removed glyph is still registered");
        h.succeed();
    }
}
