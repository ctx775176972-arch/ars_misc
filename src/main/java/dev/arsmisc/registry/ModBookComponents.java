package dev.arsmisc.registry;

import com.mojang.serialization.Codec;
import dev.arsmisc.ArsMiscMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModBookComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArsMiscMod.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> BLESSING = COMPONENTS.register("spellbook_blessing",
            () -> DataComponentType.<Boolean>builder().persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL).build());
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
