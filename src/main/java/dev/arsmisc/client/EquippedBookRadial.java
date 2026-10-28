package dev.arsmisc.client;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.GuiRadialMenu;
import com.hollingsworth.arsnouveau.client.gui.radial_menu.RadialMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Uses the equipped-book binding instead of Ars' handheld radial binding. */
public final class EquippedBookRadial extends GuiRadialMenu<AbstractSpellPart> {
    private boolean released;
    public EquippedBookRadial(RadialMenu<AbstractSpellPart> menu) {
        super(menu); setHoldToOpenGUI(false);
    }
    @Override public void tick() {
        super.tick();
        if (!released && !selectionKeyDown()) {
            released = true;
            super.mouseClicked(0, 0, 0);
            if (Minecraft.getInstance().screen == this) onClose();
        }
    }
    static boolean selectionKeyDown() {
        if (EquippedBookClient.SELECT.isUnbound()) return false;
        var key = EquippedBookClient.SELECT.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        return key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(window, key.getValue());
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        // Consume OS key repeats; the opening key must never toggle this screen.
        if (EquippedBookClient.SELECT.matches(key, scan)) return true;
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (EquippedBookClient.SELECT.matchesMouse(button)) return true;
        return super.mouseClicked(x, y, button);
    }
}
