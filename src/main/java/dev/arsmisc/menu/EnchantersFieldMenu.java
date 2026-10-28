package dev.arsmisc.menu;

import dev.arsmisc.item.EnchantersFieldItem;
import dev.arsmisc.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** No movable slots: the server only edits the exact held item used to open this menu. */
public final class EnchantersFieldMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final ItemStack openedStack;
    private final ContainerData data;

    public EnchantersFieldMenu(int id, Inventory inv, RegistryFriendlyByteBuf buffer) {
        this(id, inv, buffer.readEnum(InteractionHand.class));
    }
    public EnchantersFieldMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenus.ENCHANTERS_FIELD.get(), id);
        this.hand = hand;
        openedStack = inv.player.getItemInHand(hand);
        data = inv.player.level().isClientSide ? new SimpleContainerData(3) : new ContainerData() {
            public int get(int i) { return i == 0 ? EnchantersFieldItem.interval(openedStack) : i == 1 ? (EnchantersFieldItem.enabled(openedStack) ? 1 : 0) : EnchantersFieldItem.radiusSetting(openedStack); }
            public void set(int i, int v) {}
            public int getCount() { return 3; }
        };
        addDataSlots(data);
    }
    public int radiusSetting() { return data.get(2); }
    public ItemStack watch() { return openedStack; }
    public int interval() { return data.get(0); }
    public boolean enabled() { return data.get(1) != 0; }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && player.getItemInHand(hand) == openedStack
                && openedStack.getItem() instanceof EnchantersFieldItem;
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (player.level().isClientSide || !stillValid(player)) return false;
        if (button == 1) EnchantersFieldItem.setEnabled(openedStack, !enabled());
        else if (button >= 100 + 1 && button <= 100 + 3600)
            EnchantersFieldItem.setInterval(openedStack, button - 100);
        else if (button >= 10001 && button <= 10000 + EnchantersFieldItem.MAX_RADIUS) EnchantersFieldItem.setRadius(openedStack, button - 10000);
        else return false;
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        broadcastChanges();
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
