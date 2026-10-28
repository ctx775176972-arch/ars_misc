package dev.arsmisc.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.MiscConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Recipe-only gating, matching Ars Elemental's optional Nullify Defense recipe. */
public record ModGlyphConditions(String glyph) implements ICondition {
    public static final MapCodec<ModGlyphConditions> CODEC=Codec.STRING.fieldOf("glyph")
            .xmap(ModGlyphConditions::new,ModGlyphConditions::glyph);
    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS=
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS,ArsMiscMod.MOD_ID);
    static { CONDITIONS.register("glyph_recipe_enabled",()->CODEC); }
    public static void register(IEventBus bus) { CONDITIONS.register(bus); }
    @Override public boolean test(IContext context) {
        return switch (glyph) {
            case "soul_harvest" -> MiscConfig.ENABLE_SOUL_HARVEST_RECIPE.get();
            case "avada_kedavra" -> MiscConfig.ENABLE_AVADA_KEDAVRA_RECIPE.get();
            default -> false;
        };
    }
    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
