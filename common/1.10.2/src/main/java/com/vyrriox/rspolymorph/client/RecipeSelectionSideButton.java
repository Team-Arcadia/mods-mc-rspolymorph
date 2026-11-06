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

import com.raoulvdberge.refinedstorage.gui.GuiBase;
import com.raoulvdberge.refinedstorage.gui.sidebutton.SideButton;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The button of the grid's side bar. It only shows when several recipes match, so every time it
 * shows it is a warning: a red mark and a gold halo that pulses until the popup opens.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
final class RecipeSelectionSideButton extends SideButton {

    private static final ResourceLocation ICON =
            new ResourceLocation(RsPolymorph.MOD_ID, "textures/gui/sprites/widget/side_button/polymorph.png");
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int ICON_SIZE = 16;

    /** Gold of the popup's active-recipe frame; the alpha is applied per frame for the pulse. */
    private static final int PULSE_RGB = 0xFFD24A;
    private static final long PULSE_MS = 1300L;

    private final GridRecipeWidget widget;
    private final Canvas canvas;
    private final String tooltip;
    private boolean drawn;

    RecipeSelectionSideButton(GuiBase gui, GridRecipeWidget widget, Canvas canvas) {
        super(gui);
        this.widget = widget;
        this.canvas = canvas;
        this.tooltip = Canvas.translate("rspolymorph.gui.recipe_selection")
                + "\n" + TextFormatting.GRAY + Canvas.translate("rspolymorph.gui.click_to_select")
                + "\n" + TextFormatting.RED + Canvas.translate("rspolymorph.gui.multiple_recipes_warning");
    }

    @Override
    public String getTooltip() {
        return tooltip;
    }

    @Override
    protected void drawButtonIcon(int iconX, int iconY) {
        canvas.texture(ICON, iconX, iconY, ICON_SIZE);
        canvas.text("!", iconX + ICON_SIZE - 4, iconY, WARNING_COLOR, true);
    }

    /** The base class draws without looking at {@code visible}, and marks the button as hovered. */
    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        drawn = true;
        if (!visible) {
            hovered = false;
            return;
        }
        super.drawButton(mc, mouseX, mouseY);
        if (!widget.isPopupOpen()) {
            drawPulse();
        }
    }

    void setShown(boolean shown) {
        visible = shown;
    }

    boolean isShown() {
        return visible;
    }

    /** @return true if a press at this point would hit the button */
    boolean hit(Minecraft mc, int mouseX, int mouseY) {
        return mousePressed(mc, mouseX, mouseY);
    }

    int screenX() {
        return xPosition;
    }

    int screenY() {
        return yPosition;
    }

    /** The left edge of the screen's main area. */
    int originX() {
        return gui.getGuiLeft();
    }

    int originY() {
        return gui.getGuiTop();
    }

    /** The widget in the screen's button list. */
    GuiButton backing() {
        return this;
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
        int x = xPosition;
        int y = yPosition;
        canvas.fill(x - 1, y - 1, x + width + 1, y, color);
        canvas.fill(x - 1, y + height, x + width + 1, y + height + 1, color);
        canvas.fill(x - 1, y, x, y + height, color);
        canvas.fill(x + width, y, x + width + 1, y + height, color);
    }

    @Override
    public void actionPerformed() {
        widget.togglePopup();
    }
}
