package dev.arsmisc;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ArsMiscMod.MOD_ID)
public final class ArsMiscMod {
    public static final String MOD_ID = "ars_misc";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredItem<dev.arsmisc.item.EnchantersPocketWatchItem> ENCHANTERS_POCKET_WATCH = ITEMS.register("enchanters_pocket_watch", dev.arsmisc.item.EnchantersPocketWatchItem::new);
    public static final DeferredItem<dev.arsmisc.item.EnchantersFieldItem> ENCHANTERS_FIELD = ITEMS.register("enchanters_field", dev.arsmisc.item.EnchantersFieldItem::new);
    public static final DeferredItem<dev.arsmisc.item.FinalBarrierItem> FINAL_BARRIER = ITEMS.register("final_barrier", dev.arsmisc.item.FinalBarrierItem::new);
    public static final DeferredItem<dev.arsmisc.item.NetheriteEnchantersSwordItem> NETHERITE_ENCHANTERS_SWORD = ITEMS.register("netherite_enchanters_sword", dev.arsmisc.item.NetheriteEnchantersSwordItem::new);
    public static final DeferredItem<PaladinFocusItem> PALADIN_FOCUS = ITEMS.register("paladin_focus", PaladinFocusItem::new);
    public static final DeferredItem<dev.arsmisc.item.CombatWandItem> COMBAT_WAND = ITEMS.register("combat_wand", dev.arsmisc.item.CombatWandItem::new);
    public static final DeferredItem<dev.arsmisc.item.NetheriteMageArmorItem> NETHERITE_MAGE_HELMET = ITEMS.register("netherite_mage_helmet",
            () -> new dev.arsmisc.item.NetheriteMageArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET));
    public static final DeferredItem<dev.arsmisc.item.NetheriteMageArmorItem> NETHERITE_MAGE_CHESTPLATE = ITEMS.register("netherite_mage_chestplate",
            () -> new dev.arsmisc.item.NetheriteMageArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<dev.arsmisc.item.NetheriteMageArmorItem> NETHERITE_MAGE_LEGGINGS = ITEMS.register("netherite_mage_leggings",
            () -> new dev.arsmisc.item.NetheriteMageArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<dev.arsmisc.item.NetheriteMageArmorItem> NETHERITE_MAGE_BOOTS = ITEMS.register("netherite_mage_boots",
            () -> new dev.arsmisc.item.NetheriteMageArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final DeferredItem<MiningRingItem> FORTUNE_RING = ITEMS.register("fortune_ring",
            () -> new MiningRingItem(MiningRingItem.Series.FORTUNE, 1));
    public static final java.util.function.Supplier<com.hollingsworth.arsnouveau.common.items.PerkItem> STURDY_THREAD =
            () -> com.hollingsworth.arsnouveau.api.registry.PerkRegistry.getPerkItemMap().get(DefensePerk.STURDY.getRegistryName());
    public static final java.util.function.Supplier<com.hollingsworth.arsnouveau.common.items.PerkItem> RESILIENT_THREAD =
            () -> com.hollingsworth.arsnouveau.api.registry.PerkRegistry.getPerkItemMap().get(DefensePerk.RESILIENT.getRegistryName());
    public static final java.util.function.Supplier<com.hollingsworth.arsnouveau.common.items.PerkItem> CREATIVE_FLIGHT_THREAD =
            () -> com.hollingsworth.arsnouveau.api.registry.PerkRegistry.getPerkItemMap().get(CreativeFlightPerk.INSTANCE.getRegistryName());
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, DecayEffect> DECAY = EFFECTS.register("decay", DecayEffect::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, LootSenseEffect> LOOT_SENSE = EFFECTS.register("loot_sense", LootSenseEffect::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, LifeSenseEffect> LIFE_SENSE = EFFECTS.register("life_sense", LifeSenseEffect::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, CrystalShieldEffect> CRYSTAL_SHIELD = EFFECTS.register("crystal_shield", CrystalShieldEffect::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, CrystallizationEffect> CRYSTALLIZATION = EFFECTS.register("crystallization", CrystallizationEffect::new);
    public static final DeferredItem<CriticalNecklaceItem> CRITICAL_NECKLACE = ITEMS.register("critical_necklace", CriticalNecklaceItem::new);
    public static final DeferredItem<SpellDamageBeltItem> CRITICAL_BELT = ITEMS.register("critical_belt", SpellDamageBeltItem::new);
    public static final DeferredItem<CriticalGlassesItem> CRITICAL_GLASSES = ITEMS.register("critical_glasses", CriticalGlassesItem::new);
    public static final java.util.List<DeferredItem<MiningRingItem>> FORTUNE_STAGES = java.util.List.of(
            FORTUNE_RING, ITEMS.register("fortune_ring_2", () -> new MiningRingItem(MiningRingItem.Series.FORTUNE, 2)));
    public static final java.util.List<DeferredItem<MiningRingItem>> BLASTING_STAGES = java.util.List.of(
            ITEMS.register("blasting_miner_ring_1", () -> new MiningRingItem(MiningRingItem.Series.BLASTING, 1)),
            ITEMS.register("blasting_miner_ring_2", () -> new MiningRingItem(MiningRingItem.Series.BLASTING, 2)));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    static {
        TABS.register("accessories", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ars_misc.accessories"))
                .icon(() -> EffectCrystallization.INSTANCE.getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    FORTUNE_STAGES.forEach(item -> output.accept(item.get()));
                    BLASTING_STAGES.forEach(item -> output.accept(item.get()));


                    output.accept(CRITICAL_BELT.get());
                    output.accept(CRITICAL_GLASSES.get());
                    output.accept(CRITICAL_NECKLACE.get());
                    output.accept(STURDY_THREAD.get());
                    output.accept(RESILIENT_THREAD.get());
                    output.accept(CREATIVE_FLIGHT_THREAD.get());
                    output.accept(ENCHANTERS_POCKET_WATCH.get());
                    output.accept(ENCHANTERS_FIELD.get());
                    output.accept(FINAL_BARRIER.get());
                    output.accept(PALADIN_FOCUS.get());
                    output.accept(COMBAT_WAND.get());
                    output.accept(NETHERITE_MAGE_HELMET.get());
                    output.accept(NETHERITE_MAGE_CHESTPLATE.get());
                    output.accept(NETHERITE_MAGE_LEGGINGS.get());
                    output.accept(NETHERITE_MAGE_BOOTS.get());
                    output.accept(NETHERITE_ENCHANTERS_SWORD.get());
                }).build());
    }

    public ArsMiscMod(IEventBus modEventBus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, MiscConfig.SPEC);
        com.hollingsworth.arsnouveau.api.registry.PerkRegistry.registerPerk(DefensePerk.STURDY);
        com.hollingsworth.arsnouveau.api.registry.PerkRegistry.registerPerk(DefensePerk.RESILIENT);
        com.hollingsworth.arsnouveau.api.registry.PerkRegistry.registerPerk(CreativeFlightPerk.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectDecay.INSTANCE);
        dev.arsmisc.registry.ModBookComponents.register(modEventBus);
        dev.arsmisc.registry.ModBookRecipes.register(modEventBus);
        dev.arsmisc.registry.ModGlyphConditions.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            top.theillusivec4.curios.api.CuriosApi.registerCurioPredicate(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "spellbook"),
                slot -> slot.stack().getItem() instanceof com.hollingsworth.arsnouveau.common.items.SpellBook)));
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectLifeSense.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectLootSense.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectRegeneration.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectFlight.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectSacrificialShield.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectCrystalShield.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectCrystallization.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectSoulHarvest.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectAvadaKedavra.INSTANCE);
        dev.arsmisc.registry.ModArmorMaterials.MATERIALS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            NETHERITE_MAGE_HELMET.get().registerThreadSlots();
            NETHERITE_MAGE_CHESTPLATE.get().registerThreadSlots();
            NETHERITE_MAGE_LEGGINGS.get().registerThreadSlots();
            NETHERITE_MAGE_BOOTS.get().registerThreadSlots();
        }));
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(COMBAT_WAND.get(), COMBAT_WAND.get())));
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(NETHERITE_ENCHANTERS_SWORD.get(), NETHERITE_ENCHANTERS_SWORD.get())));
        dev.arsmisc.registry.ModMenus.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(ENCHANTERS_POCKET_WATCH.get(), ENCHANTERS_POCKET_WATCH.get())));
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(ENCHANTERS_FIELD.get(), ENCHANTERS_FIELD.get())));
        modEventBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
            event.registrar("1").playToServer(dev.arsmisc.network.ToggleCastingPayload.TYPE, dev.arsmisc.network.ToggleCastingPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> dev.arsmisc.network.ToggleCastingPayload.toggle(context.player()))));
        NeoForge.EVENT_BUS.addListener(WatchDocumentation::addEntries);
        EFFECTS.register(modEventBus);
        TABS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(AccessoryEvents::modifySpell);
        NeoForge.EVENT_BUS.addListener(AccessoryEvents::afterSpellDamage);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, DecayEvents::onHeal);
    }
}

