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

import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import refinedstorage.gui.GuiBase;
import refinedstorage.gui.sidebutton.SideButton;

import java.util.List;

/**
 * The button of the grid's side bar. It only shows when several recipes match, so every time it
 * shows it is a warning: a red mark and a gold halo that pulses until the popup opens.
 *
 * A side button of this Refined Storage is no widget: the screen adds a plain button with the same id
 * for it and draws the icon in its foreground layer, with the origin at the corner of its main area.
 * So hiding it means hiding that plain button.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
final class RecipeSelectionSideButton extends SideButton {

    private static final ResourceLocation ICON =
            new ResourceLocation(RsPolymorph.MOD_ID, "textures/gui/sprites/widget/side_button/polymorph.png");
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int ICON_SIZE = 16;
    private static final int SIZE = 20;

    /** Gold of the popup's active-recipe frame; the alpha is applied per frame for the pulse. */
    private static final int PULSE_RGB = 0xFFD24A;
    private static final long PULSE_MS = 1300L;

    private final GuiScreen screen;
    private final GridRecipeWidget widget;
    private final Canvas canvas;
    private final String tooltip;
    private GuiButton backing;
    private boolean shown;
    private boolean drawn;

    RecipeSelectionSideButton(GuiBase gui, GridRecipeWidget widget, Canvas canvas) {
        this.screen = gui;
        this.widget = widget;
        this.canvas = canvas;
        this.tooltip = Canvas.translate("rspolymorph.gui.recipe_selection")
                + "\n" + TextFormatting.GRAY + Canvas.translate("rspolymorph.gui.click_to_select")
                + "\n" + TextFormatting.RED + Canvas.translate("rspolymorph.gui.multiple_recipes_warning");
    }

    private List<GuiButton> screenButtons() {
        return ReflectionHelper.getPrivateValue(GuiScreen.class, screen, "buttonList", "field_146292_n");
    }

    /**
     * An init of the screen without a resize leaves the old plain buttons in place, and the ids start
     * over: left there, the old one would fire with the new one's id and a click would toggle twice.
     */
    void detach() {
        if (backing != null) {
            screenButtons().remove(backing);
        }
    }

    /** The plain button the screen made for this one: found once per init of the screen. */
    GuiButton backing() {
        if (backing == null) {
            for (GuiButton button : screenButtons()) {
                if (button.id == getId()) {
                    backing = button;
                }
            }
        }
        return backing;
    }

    void setShown(boolean value) {
        shown = value;
        backing().visible = value;
    }

    boolean isShown() {
        return shown;
    }

    /** @return true if a press at this point would hit the button */
    boolean hit(Minecraft mc, int mouseX, int mouseY) {
        return backing().mousePressed(mc, mouseX, mouseY);
    }

    int screenX() {
        return backing().xPosition;
    }

    int screenY() {
        return backing().yPosition;
    }

    /** The left edge of the screen's main area, where the coordinates of a side button start. */
    int originX() {
        return screenX() - getX();
    }

    int originY() {
        return screenY() - getY();
    }

    /** No text at all while hidden: the screen draws the tooltip of anything under the cursor. */
    @Override
    public String getTooltip(GuiBase gui) {
        return shown ? tooltip : null;
    }

    @Override
    public void draw(GuiBase gui, int x, int y) {
        drawn = true;
        if (!shown) {
            return;
        }
        canvas.texture(ICON, x, y, ICON_SIZE);
        canvas.text("!", x + ICON_SIZE - 4, y, WARNING_COLOR, true);
        if (!widget.isPopupOpen()) {
            drawPulse();
        }
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** @return false when the last frame did not draw this button: it is no longer on the screen */
    boolean consumeDrawn() {
        boolean was = drawn;
        drawn = false;
        return was;
    }

    /** A 1 px gold halo whose alpha follows a cosine. */
    private void drawPulse() {
        double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
        float pulse = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        int color = ((70 + (int) (pulse * 185.0f)) << 24) | PULSE_RGB;
        int x = getX();
        int y = getY();
        canvas.fill(x - 1, y - 1, x + SIZE + 1, y, color);
        canvas.fill(x - 1, y + SIZE, x + SIZE + 1, y + SIZE + 1, color);
        canvas.fill(x - 1, y, x, y + SIZE, color);
        canvas.fill(x + SIZE, y, x + SIZE + 1, y + SIZE, color);
    }

    @Override
    public void actionPerformed() {
        widget.togglePopup();
    }
}
