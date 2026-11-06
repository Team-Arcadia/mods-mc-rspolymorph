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

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * All the drawing of the mod. An older Minecraft draws with a PoseStack instead of GuiGraphics: it
 * forks this class, the popup and the tutorial card only ever talk to a Canvas. One instance per
 * widget, reused every frame.
 *
 * Author: vyrriox
 */
public final class Canvas {

    private GuiGraphics graphics;

    public void begin(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    /** Raises what follows above the items of the screen, which draw at a high Z. */
    public void pushLayer(float z) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, z);
    }

    public void popLayer() {
        graphics.pose().popPose();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        graphics.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public void text(Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
    }

    public void item(ItemStack stack, int x, int y) {
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }

    public void itemTooltip(ItemStack stack, int mouseX, int mouseY) {
        graphics.renderTooltip(Minecraft.getInstance().font, stack, mouseX, mouseY);
    }

    /** Draws a whole square texture: 1.20.1 has no GUI sprite atlas yet. */
    public void texture(ResourceLocation texture, int x, int y, int size) {
        RenderSystem.enableBlend();
        graphics.blit(texture, x, y, 0, 0.0f, 0.0f, size, size, size, size);
        RenderSystem.disableBlend();
    }

    public static int textWidth(Component text) {
        return Minecraft.getInstance().font.width(text);
    }

    public static int lineHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    public static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static int screenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    /** Cursor position in GUI coordinates, whatever the screen was told for this frame. */
    public static int mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
    }

    public static int mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
    }
}
