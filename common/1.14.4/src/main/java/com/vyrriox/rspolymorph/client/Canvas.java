/*
 * Copyright 2026 vyrriox / Team Arcadia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vyrriox.rspolymorph.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * All the drawing of the mod, for Minecraft 1.14.4: no MatrixStack in the GUI, everything goes
 * through the global render state. The popup and the
 * tutorial card only ever talk to a Canvas. One instance per widget, reused every frame.
 *
 * Author: vyrriox
 */
public final class Canvas {

    /** Only to reach the protected gradient of AbstractGui. */
    private static final class Gui extends AbstractGui {

        void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
            fillGradient(x1, y1, x2, y2, top, bottom);
        }
    }

    private final Gui gui = new Gui();

    /** Raises what follows above the items of the screen, which draw at a high Z. */
    public void pushLayer(float z) {
        // This Minecraft leaves the screen's item lighting on after drawing it, which would darken the overlays.
        RenderHelper.turnOff();
        GlStateManager.pushMatrix();
        GlStateManager.translatef(0.0f, 0.0f, z);
    }

    public void popLayer() {
        GlStateManager.popMatrix();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        AbstractGui.fill(x1, y1, x2, y2, color);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        gui.gradient(x1, y1, x2, y2, top, bottom);
    }

    public void text(ITextComponent text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            Minecraft.getInstance().font.drawShadow(text.getString(), x, y, color);
        } else {
            Minecraft.getInstance().font.draw(text.getString(), x, y, color);
        }
    }

    public void item(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer renderer = mc.getItemRenderer();
        // The global matrix raises items and their counts too here: adding the renderer's offset as well would
        // push the counts past the far plane of the GUI.
        RenderHelper.turnOnGui();
        renderer.renderAndDecorateItem(stack, x, y);
        renderer.renderGuiItemDecorations(mc.font, stack, x, y);
        // The decorations leave the lighting on, which would darken whatever is drawn next.
        RenderHelper.turnOff();
    }

    public void itemTooltip(ItemStack stack, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(screen.getTooltipFromItem(stack), mouseX, mouseY);
        }
    }

    /** Draws a whole square texture. Leaves it bound: whoever draws next binds its own. */
    public void texture(ResourceLocation texture, int x, int y, int size) {
        Minecraft.getInstance().getTextureManager().bind(texture);
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        AbstractGui.blit(x, y, 0, 0.0f, 0.0f, size, size, size, size);
        GlStateManager.disableBlend();
    }

    public static void narrate(ITextComponent text) {
        NarratorChatListener.INSTANCE.sayNow(text.getString());
    }

    public static int textWidth(ITextComponent text) {
        return Minecraft.getInstance().font.width(text.getString());
    }

    public static int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public static int screenWidth() {
        return Minecraft.getInstance().window.getGuiScaledWidth();
    }

    public static int screenHeight() {
        return Minecraft.getInstance().window.getGuiScaledHeight();
    }

    /** Cursor position in GUI coordinates, whatever the screen was told for this frame. */
    public static int mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.xpos() * mc.window.getGuiScaledWidth() / mc.window.getScreenWidth());
    }

    public static int mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.ypos() * mc.window.getGuiScaledHeight() / mc.window.getScreenHeight());
    }
}
