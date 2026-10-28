package dev.arsmisc.registry;

import dev.arsmisc.ArsMiscMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Map;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ArsMiscMod.MOD_ID);
    public static final net.minecraft.core.Holder<ArmorMaterial> NETHERITE_MAGE = MATERIALS.register("netherite_mage", () ->
            new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 3, ArmorItem.Type.CHESTPLATE, 8,
                    ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.BOOTS, 3), 30,
                    SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(Items.NETHERITE_INGOT),
                    ArmorMaterials.NETHERITE.value().layers(), 3.0f, 0.1f));
    private ModArmorMaterials() {}
}
