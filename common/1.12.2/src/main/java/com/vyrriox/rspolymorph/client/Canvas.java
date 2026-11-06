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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * Every use of the drawing API, so that the popup, the button and the card stay the same on another
 * Minecraft version. Holds no state but the Gui base it needs for gradients.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
final class Canvas extends Gui {

    void pushLayer(float z) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0f, 0.0f, z);
    }

    void popLayer() {
        GlStateManager.popMatrix();
    }

    void fill(int x1, int y1, int x2, int y2, int argb) {
        drawRect(x1, y1, x2, y2, argb);
    }

    void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        drawGradientRect(x1, y1, x2, y2, top, bottom);
    }

    void text(String text, int x, int y, int rgb, boolean shadow) {
        GlStateManager.disableLighting();
        Minecraft.getMinecraft().fontRenderer.drawString(text, x, y, rgb, shadow);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    void texture(ResourceLocation texture, int x, int y, int size) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
        drawModalRectWithCustomSizedTexture(x, y, 0.0f, 0.0f, size, size, size, size);
        GlStateManager.disableBlend();
    }

    void item(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
        mc.getRenderItem().renderItemOverlays(mc.fontRenderer, stack, x, y);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** Skips {@code ClientEvents.onTooltip}, which hides the tooltips of the screen under an overlay. */
    void itemTooltip(GuiScreen screen, ItemStack stack, int mouseX, int mouseY) {
        ClientEvents.drawingOwnTooltip = true;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            List<String> lines = screen.getItemToolTip(stack);
            ScaledResolution scaled = new ScaledResolution(mc);
            GuiUtils.drawHoveringText(stack, lines, mouseX, mouseY, scaled.getScaledWidth(),
                    scaled.getScaledHeight(), -1, mc.fontRenderer);
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
        } finally {
            ClientEvents.drawingOwnTooltip = false;
        }
    }

    static String translate(String key) {
        return I18n.format(key);
    }

    static int textWidth(String text) {
        return Minecraft.getMinecraft().fontRenderer.getStringWidth(text);
    }

    static int lineHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT;
    }
}
