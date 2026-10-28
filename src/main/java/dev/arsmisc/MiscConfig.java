package dev.arsmisc;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MiscConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT;
    public static final ModConfigSpec.BooleanValue ENABLE_SOUL_HARVEST_RECIPE;
    public static final ModConfigSpec.BooleanValue ENABLE_AVADA_KEDAVRA_RECIPE;
    public static final ModConfigSpec.DoubleValue SOUL_HARVEST_MANA_PER_KILL;
    public static final ModConfigSpec.DoubleValue SOUL_HARVEST_REGEN_PER_KILL;
    public static final ModConfigSpec.DoubleValue FINAL_BARRIER_UPKEEP;
    public static final ModConfigSpec.DoubleValue FINAL_BARRIER_MANA_PER_DAMAGE;
    public static final ModConfigSpec.DoubleValue FINAL_BARRIER_RESCUE_COST;
    public static final ModConfigSpec.IntValue FINAL_BARRIER_COOLDOWN_SECONDS;
    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("loot");
        DISABLE_ADDITIONS_SPELL_PARCHMENT_LOOT = builder
                .comment("Remove Ars Additions imbued spell parchments from generated loot. Crafting, use, and existing items are unchanged.")
                .define("disable_ars_additions_spell_parchment", true);
        builder.pop();
        builder.push("glyph_recipes");
        ENABLE_SOUL_HARVEST_RECIPE = builder.comment("Enable the Soul Harvest Scribe's Table recipe. The glyph remains available in creative mode. Reload recipes or restart after changing.")
                .define("enable_soul_harvest_recipe", false);
        ENABLE_AVADA_KEDAVRA_RECIPE = builder.comment("Enable the Avada Kedavra Scribe's Table recipe. The glyph remains available in creative mode. Reload recipes or restart after changing.")
                .define("enable_avada_kedavra_recipe", false);
        builder.pop();
        builder.push("soul_harvest");
        SOUL_HARVEST_MANA_PER_KILL = builder.comment("Permanent maximum mana gained per confirmed Soul Harvest kill. Changes affect future kills.")
                .defineInRange("max_mana_per_kill", 30.0, 0, Double.MAX_VALUE);
        SOUL_HARVEST_REGEN_PER_KILL = builder.comment("Permanent mana regeneration gained per confirmed Soul Harvest kill. Changes affect future kills.")
                .defineInRange("mana_regen_per_kill", 0.6, 0, Double.MAX_VALUE);
        builder.pop();
        builder.push("final_barrier");
        FINAL_BARRIER_UPKEEP = builder.comment("Mana spent per second while Final Barrier is enabled. Also collected on first activation.")
                .defineInRange("mana_per_second", 10.0, 0, Double.MAX_VALUE);
        FINAL_BARRIER_MANA_PER_DAMAGE = builder.comment("Mana per health point protected after armor, effects and absorption. Spell cost discounts do not apply.")
                .defineInRange("mana_per_damage", 20.0, 0, Double.MAX_VALUE);
        FINAL_BARRIER_RESCUE_COST = builder.comment("Mana spent when cancelling death and restoring full health.")
                .defineInRange("rescue_mana_cost", 1000.0, 0, Double.MAX_VALUE);
        FINAL_BARRIER_COOLDOWN_SECONDS = builder.comment("Player-wide cooldown after mana is exhausted. Reactivation is manual.")
                .defineInRange("exhaustion_cooldown_seconds", 10, 0, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }
    private MiscConfig() {}
}
