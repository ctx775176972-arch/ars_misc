package dev.arsmisc.client;

import com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.*;
import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.common.items.SpellBook;
import com.mojang.blaze3d.platform.InputConstants;
import dev.arsmisc.ArsMiscMod;
import dev.arsmisc.network.EquippedBookPayload;
import dev.arsmisc.spell.EquippedSpellbook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ArsMiscMod.MOD_ID, value = Dist.CLIENT)
public final class EquippedBookClient {
    public static final KeyMapping CAST = key("cast_equipped_book", 82);
    public static final KeyMapping NEXT = key("next_equipped_spell");
    public static final KeyMapping PREVIOUS = key("previous_equipped_spell");
    public static final KeyMapping SELECT = key("select_equipped_spell", 86);
    public static final KeyMapping EDIT = key("edit_equipped_book", 67);
    public static boolean openingEditor;
    public static net.minecraft.world.item.ItemStack editingBook = net.minecraft.world.item.ItemStack.EMPTY;
    public static net.minecraft.client.gui.screens.Screen editorScreen;
    /** Native particle/sound pages share SpellSlottedScreen and the same editing session. */
    public static boolean isEditorScreen(net.minecraft.client.gui.screens.Screen screen) {
        return editorScreen != null && !editingBook.isEmpty()
                && screen instanceof com.hollingsworth.arsnouveau.client.gui.book.SpellSlottedScreen slotted
                && (screen == editorScreen || slotted.bookStack != null && slotted.bookStack.is(editingBook.getItem()));
    }
    private static int castCooldown;
    private static boolean selectionHeld;
    private static KeyMapping key(String name) {
        return key(name, -1);
    }
    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.ars_misc." + name, net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, "key.categories.ars_misc");
    }
    private static boolean pressed(KeyMapping key) {
        boolean clicked = false;
        while (key.consumeClick()) clicked = true;
        return clicked;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (editorScreen != null && (!isEditorScreen(mc.screen) || mc.player == null || EquippedSpellbook.find(mc.player).getItem() != editingBook.getItem())) {
            if (isEditorScreen(mc.screen)) mc.setScreen(null);
            if (mc.player != null) PacketDistributor.sendToServer(new EquippedBookPayload(5, 0, 0, 0));
            editorScreen = null; editingBook = net.minecraft.world.item.ItemStack.EMPTY;
        }
        if (editorScreen != null && mc.player != null) {
            editorScreen = mc.screen;
            editingBook = EquippedSpellbook.find(mc.player);
        }
        boolean cast = pressed(CAST), next = pressed(NEXT), previous = pressed(PREVIOUS), select = pressed(SELECT), edit = pressed(EDIT);
        boolean newSelection = select && !selectionHeld;
        // KeyMapping.isDown() is reset when a screen opens. Track the physical key
        // through that transition so an OS repeat cannot reopen a closed radial.
        selectionHeld = EquippedBookRadial.selectionKeyDown();
        if (castCooldown > 0) castCooldown--;
        boolean castRequested = cast || CAST.isDown();
        if (mc.player == null || mc.screen != null || mc.player.isSpectator()) return;
        var book = EquippedSpellbook.find(mc.player);
        if (book.isEmpty()) return;
        if (edit) {
            editingBook = book;
            PacketDistributor.sendToServer(new EquippedBookPayload(4, 0, 0, 0));
            openingEditor = true;
            try {
                editorScreen = new com.hollingsworth.arsnouveau.client.gui.book.GuiSpellBook(net.minecraft.world.InteractionHand.MAIN_HAND);
                mc.setScreen(editorScreen);
            } finally { openingEditor = false; }
        } else if (newSelection && book.getItem() instanceof SpellBook item) {
            RadialMenu<AbstractSpellPart> menu = new RadialMenu<>(slot -> {
                PacketDistributor.sendToServer(new EquippedBookPayload(3, slot, 0, 0));
            }, item.getRadialMenuSlotsForSpellpart(book), SecondaryIconPosition.NORTH, RenderUtils::drawSpellPart, 0);
            mc.setScreen(new EquippedBookRadial(menu));
        } else if (next || previous) {
            PacketDistributor.sendToServer(new EquippedBookPayload(next ? 1 : 2, 0, 0, 0));
        } else if (castRequested && castCooldown == 0) {
            var camera = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
            PacketDistributor.sendToServer(new EquippedBookPayload(0, 0, camera.getXRot(), camera.getYRot()));
            castCooldown = 4;
        }
    }
    @EventBusSubscriber(modid = ArsMiscMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) {
            event.register(CAST); event.register(NEXT); event.register(PREVIOUS); event.register(SELECT); event.register(EDIT);
        }
        @SubscribeEvent public static void hud(RegisterGuiLayersEvent event) {
            event.registerAbove(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath(ArsMiscMod.MOD_ID, "equipped_book"), (graphics, delta) -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
                var book = EquippedSpellbook.find(mc.player);
                var caster = SpellCasterRegistry.from(book);
                if (caster == null) return;
                int x = 8, y = graphics.guiHeight() / 2 + 30;
                String name = (caster.getCurrentSlot() + 1) + "  " + caster.getSpellName();
                int textLimit = Math.max(20, Math.min(140, graphics.guiWidth() - 60));
                if (mc.font.width(name) > textLimit) name = mc.font.plainSubstrByWidth(name, textLimit - mc.font.width("…")) + "…";
                graphics.drawString(mc.font, name, x, y + 4, 0xFFFFFF);
                int iconX = x + mc.font.width(name) + 6;
                var spell = caster.getSpell();
                var form = spell.getCastMethod();
                if (form != null) {
                    RenderUtils.drawSpellPart(form, graphics, iconX, y, 16, false);
                    iconX += 18;
                }
                for (var glyph : spell.recipe()) {
                    if (glyph instanceof com.hollingsworth.arsnouveau.api.spell.AbstractEffect
                            && !(glyph instanceof com.hollingsworth.arsnouveau.api.spell.IFilter)) {
                        RenderUtils.drawSpellPart(glyph, graphics, iconX, y, 16, false);
                        break;
                    }
                }
            });
        }
    }
}
