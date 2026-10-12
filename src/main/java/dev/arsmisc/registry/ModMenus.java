package dev.arsmisc.registry;

import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.menu.EnchantersPocketWatchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ArsMiscMod.MOD_ID);
    public static final DeferredHolder<MenuType<?>, MenuType<EnchantersPocketWatchMenu>> ENCHANTERS_POCKET_WATCH =
            MENUS.register("enchanters_pocket_watch", () -> IMenuTypeExtension.create(EnchantersPocketWatchMenu::new));
    public static void register(IEventBus bus) { MENUS.register(bus); }
}
