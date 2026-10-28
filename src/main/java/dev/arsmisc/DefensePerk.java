package dev.arsmisc;

import com.hollingsworth.arsnouveau.api.perk.Perk;
import com.hollingsworth.arsnouveau.api.perk.PerkSlot;
import com.hollingsworth.arsnouveau.api.util.PerkUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** One highest-level bonus per thread type across the native armor thread slots. */
@EventBusSubscriber(modid = ArsMiscMod.MOD_ID)
public final class DefensePerk extends Perk {
    public static final DefensePerk STURDY = new DefensePerk("thread_sturdy", "Fortification", Attributes.ARMOR, 20);
    public static final DefensePerk RESILIENT = new DefensePerk("thread_resilient", "Resilience", Attributes.ARMOR_TOUGHNESS, 4);
    private final String name;
    private final Holder<Attribute> attribute;
    private final int perLevel;

    private DefensePerk(String id, String name, Holder<Attribute> attribute, int perLevel) {
        super(ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, id));
        this.name = name;
        this.attribute = attribute;
        this.perLevel = perLevel;
    }

    @Override public PerkSlot minimumSlot() { return PerkSlot.ONE; }
    @Override public String getLangName() { return name; }
    @Override public String getName() {
        return Component.translatable("item.ars_misc." + getRegistryName().getPath()).getString();
    }
    @Override public String getLangDescription() {
        return "Grants " + perLevel + (this == STURDY ? "% armor" : " armor toughness")
                + " per thread slot level. Only the highest equipped level applies.";
    }

    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !living.level().isClientSide) refresh(living);
    }

    public static void refresh(LivingEntity entity) {
        int sturdy = 0;
        int resilient = 0;
        for (var armor : entity.getArmorSlots()) {
            var holder = PerkUtil.getPerkHolder(armor);
            if (holder == null) continue;
            for (var instance : holder.getPerkInstances(armor)) {
                if (instance.getPerk() == STURDY) sturdy = Math.max(sturdy, instance.getSlot().value());
                if (instance.getPerk() == RESILIENT) resilient = Math.max(resilient, instance.getSlot().value());
            }
        }
        STURDY.update(entity, sturdy);
        RESILIENT.update(entity, resilient);
    }

    private void update(LivingEntity entity, int level) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return;
        double amount = Math.max(0, level) * perLevel / (this == STURDY ? 100.0 : 1.0);
        var operation = this == STURDY ? AttributeModifier.Operation.ADD_MULTIPLIED_BASE : AttributeModifier.Operation.ADD_VALUE;
        var current = instance.getModifier(getRegistryName());
        if (current != null && current.amount() == amount && current.operation() == operation) return;
        instance.removeModifier(getRegistryName());
        if (amount > 0) instance.addTransientModifier(new AttributeModifier(getRegistryName(), amount, operation));
    }
}
