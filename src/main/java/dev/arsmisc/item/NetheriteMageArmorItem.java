package dev.arsmisc.item;

import net.minecraft.world.item.ArmorItem;
import com.hollingsworth.arsnouveau.api.perk.*;
import com.hollingsworth.arsnouveau.api.registry.PerkRegistry;
import com.hollingsworth.arsnouveau.api.util.PerkUtil;
import com.hollingsworth.arsnouveau.common.items.data.ArmorPerkHolder;
import com.hollingsworth.arsnouveau.setup.registry.DataComponentRegistry;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.registry.ModArmorMaterials;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.List;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.function.Consumer;

/** Native Ars appearance and threads with endgame mage armor attributes. */
public final class NetheriteMageArmorItem extends ArmorItem implements GeoItem, com.hollingsworth.arsnouveau.api.mana.IManaDiscountEquipment {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public NetheriteMageArmorItem(Type type) {
        super(ModArmorMaterials.NETHERITE_MAGE, type, new Properties().durability(7500).rarity(Rarity.UNCOMMON)
                .fireResistant().component(DataComponentRegistry.ARMOR_PERKS.get(), new ArmorPerkHolder().setTier(2)));
    }

    public void registerThreadSlots() {
        var slots = List.of(PerkSlot.THREE, PerkSlot.TWO,
                type == Type.HELMET || type == Type.BOOTS ? PerkSlot.ONE : PerkSlot.TWO);
        PerkRegistry.registerPerkProvider(this, List.of(slots, slots, slots, slots));
    }

    @Override public boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) { return true; }
    @Override public int getManaDiscount(ItemStack stack, com.hollingsworth.arsnouveau.api.spell.Spell spell) { return 10; }

    @Override public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        var modifiers = super.getDefaultAttributeModifiers(stack);
        var group = EquipmentSlotGroup.bySlot(type.getSlot());
        for (var perk : PerkUtil.getPerksFromItem(stack)) {
            modifiers = perk.getPerk().applyAttributeModifiers(modifiers, stack, perk.getSlot().value(), group);
        }
        var id = ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "mage_armor_" + type.getName());
        return modifiers.withModifierAdded(PerkAttributes.MAX_MANA,
                        new AttributeModifier(id, 150, AttributeModifier.Operation.ADD_VALUE), group)
                .withModifierAdded(PerkAttributes.MANA_REGEN_BONUS,
                        new AttributeModifier(id, 6, AttributeModifier.Operation.ADD_VALUE), group);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!level.isClientSide && entity instanceof LivingEntity wearer && wearer.getItemBySlot(type.getSlot()) == stack) {
            com.hollingsworth.arsnouveau.common.perk.RepairingPerk.attemptRepair(stack, wearer);
            for (var perk : PerkUtil.getPerksFromItem(stack)) {
                if (perk.getPerk() instanceof ITickablePerk tickable) tickable.tick(stack, level, wearer, perk);
            }
        }
    }

    @Override public boolean isDamageable(ItemStack stack) {
        return !stack.has(DataComponentRegistry.UNBREAKING.get()) && super.isDamageable(stack);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        var holder = PerkUtil.getPerkHolder(stack);
        if (holder != null) holder.appendPerkTooltip(lines, stack);
        lines.add(Component.translatable("tooltip.ars_misc.netherite_mage_discount"));
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(dev.arsmisc.client.NetheriteMageArmorRenderer.createProvider());
    }
}
