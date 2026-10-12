package dev.arsmisc;

import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
    public static final DeferredItem<AdvancedEnchantersSwordItem> ADVANCED_ENCHANTERS_SWORD = ITEMS.register("advanced_enchanters_sword", AdvancedEnchantersSwordItem::new);
    public static final DeferredItem<MiningRingItem> FORTUNE_RING = ITEMS.register("fortune_ring",
            () -> new MiningRingItem(MiningRingItem.Series.FORTUNE, 1));
    public static final DeferredItem<TestAccessoryItem> DECAY_RING = ITEMS.register("decay_ring",
            () -> new TestAccessoryItem("ring", false, "decay_ring"));
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, DecayEffect> DECAY = EFFECTS.register("decay", DecayEffect::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect, LifeSenseEffect> LIFE_SENSE = EFFECTS.register("life_sense", LifeSenseEffect::new);
    public static final DeferredItem<CriticalNecklaceItem> CRITICAL_NECKLACE = ITEMS.register("critical_necklace", CriticalNecklaceItem::new);
    public static final DeferredItem<TestAccessoryItem> CRITICAL_BELT = ITEMS.register("critical_belt",
            () -> new TestAccessoryItem("belt", true, "critical_belt"));
    public static final java.util.List<DeferredItem<MiningRingItem>> FORTUNE_STAGES = java.util.List.of(
            FORTUNE_RING, ITEMS.register("fortune_ring_2", () -> new MiningRingItem(MiningRingItem.Series.FORTUNE, 2)));
    public static final java.util.List<DeferredItem<MiningRingItem>> BLASTING_STAGES = java.util.List.of(
            ITEMS.register("blasting_miner_ring_1", () -> new MiningRingItem(MiningRingItem.Series.BLASTING, 1)),
            ITEMS.register("blasting_miner_ring_2", () -> new MiningRingItem(MiningRingItem.Series.BLASTING, 2)));
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    static {
        TABS.register("accessories", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.ars_misc.accessories"))
                .icon(() -> FORTUNE_STAGES.get(1).get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    FORTUNE_STAGES.forEach(item -> output.accept(item.get()));
                    BLASTING_STAGES.forEach(item -> output.accept(item.get()));


                    output.accept(CRITICAL_BELT.get());
                    output.accept(CRITICAL_NECKLACE.get());
                    output.accept(DECAY_RING.get());
                    output.accept(ENCHANTERS_POCKET_WATCH.get());
                    output.accept(ADVANCED_ENCHANTERS_SWORD.get());
                    output.accept(EffectLifeSense.INSTANCE.getGlyph());
                    output.accept(EffectRegeneration.INSTANCE.getGlyph());
                    output.accept(EffectSacrificialShield.INSTANCE.getGlyph());
                }).build());
    }

    public ArsMiscMod(IEventBus modEventBus) {
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectLifeSense.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectRegeneration.INSTANCE);
        com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.registerSpell(EffectSacrificialShield.INSTANCE);
        ITEMS.register(modEventBus);
        dev.arsmisc.registry.ModMenus.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(ENCHANTERS_POCKET_WATCH.get(), ENCHANTERS_POCKET_WATCH.get())));
        NeoForge.EVENT_BUS.addListener(WatchDocumentation::addEntries);
        EFFECTS.register(modEventBus);
        TABS.register(modEventBus);
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) -> event.enqueueWork(() ->
            com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.register(ADVANCED_ENCHANTERS_SWORD.get(), ADVANCED_ENCHANTERS_SWORD.get())));
        NeoForge.EVENT_BUS.addListener(AdvancedEnchantersSwordItem::modifyAttributes);
        NeoForge.EVENT_BUS.addListener(AccessoryEvents::modifySpell);
        NeoForge.EVENT_BUS.addListener(AccessoryEvents::afterSpellDamage);
        NeoForge.EVENT_BUS.addListener(DecayEvents::afterSpellDamage);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, DecayEvents::onHeal);
    }
}

