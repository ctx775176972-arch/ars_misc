package dev.arsmisc;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class CrystalShieldEffect extends MobEffect {
    public static final String POWER_KEY = "ars_misc:crystal_shield_power";
    public static final ResourceLocation SLOW_ID = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "crystal_shield_slow");

    public CrystalShieldEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAC83DC);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,
                SLOW_ID,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, amplifier -> -0.25);
    }
}
