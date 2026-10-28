package dev.arsmisc;

import com.alexthw.sauce.registry.ModRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class CrystallizationEffect extends MobEffect {
    public CrystallizationEffect() {
        super(MobEffectCategory.HARMFUL, 0xad80db);
        addAttributeModifier(ModRegistry.SPELL_RESISTANCE,
                ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "crystallization_resistance"),
                AttributeModifier.Operation.ADD_VALUE, amplifier -> -0.1 * (Math.clamp(amplifier, 0, 2) + 1));
    }
}
