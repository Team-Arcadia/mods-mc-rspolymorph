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

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * All the drawing of the mod, for Minecraft 1.16.1, whose text methods take any ITextProperties: a MatrixStack for 2D, the global render state for
 * textures and items. It extends AbstractGui only to reach its protected gradient. The popup and the
 * tutorial card only ever talk to a Canvas. One instance per widget, reused every frame.
 *
 * Author: vyrriox
 */
public final class Canvas extends AbstractGui {

    private MatrixStack pose;
    /** Items ignore the MatrixStack here: they take their height from the item renderer. */
    private float itemLayer;

    public void begin(MatrixStack pose) {
        this.pose = pose;
    }

    /** Raises what follows above the items of the screen, which draw at a high Z. */
    public void pushLayer(float z) {
        pose.pushPose();
        pose.translate(0.0, 0.0, z);
        itemLayer = z;
    }

    public void popLayer() {
        pose.popPose();
        itemLayer = 0.0f;
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        AbstractGui.fill(pose, x1, y1, x2, y2, color);
    }

    public void gradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        fillGradient(pose, x1, y1, x2, y2, top, bottom);
    }

    public void text(ITextComponent text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            Minecraft.getInstance().font.drawShadow(pose, text, x, y, color);
        } else {
            Minecraft.getInstance().font.draw(pose, text, x, y, color);
        }
    }

    public void item(ItemStack stack, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer renderer = mc.getItemRenderer();
        float previous = renderer.blitOffset;
        renderer.blitOffset = itemLayer;
        renderer.renderAndDecorateItem(stack, x, y);
        renderer.renderGuiItemDecorations(mc.font, stack, x, y);
        renderer.blitOffset = previous;
    }

    public void itemTooltip(ItemStack stack, int mouseX, int mouseY) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen != null) {
            screen.renderTooltip(pose, screen.getTooltipFromItem(stack), mouseX, mouseY);
        }
    }

    /** Draws a whole square texture. Leaves it bound: whoever draws next binds its own. */
    public void texture(ResourceLocation texture, int x, int y, int size) {
        Minecraft.getInstance().getTextureManager().bind(texture);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableBlend();
        AbstractGui.blit(pose, x, y, 0, 0.0f, 0.0f, size, size, size, size);
        RenderSystem.disableBlend();
    }

    public static void narrate(ITextComponent text) {
        NarratorChatListener.INSTANCE.sayNow(text.getString());
    }

    public static int textWidth(ITextComponent text) {
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
