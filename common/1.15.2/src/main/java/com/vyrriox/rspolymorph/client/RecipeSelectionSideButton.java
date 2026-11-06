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

import com.refinedmods.refinedstorage.RS;
import com.refinedmods.refinedstorage.screen.BaseScreen;
import com.refinedmods.refinedstorage.screen.widget.sidebutton.SideButton;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

/**
 * Refined Storage side button that opens the recipe popup, shown only while several recipes match
 * the matrix. It warns the way Refined Storage warns in this line, in red (a mark on the button and
 * a tooltip line), and a gold halo pulses around it until the popup is open.
 *
 * Author: vyrriox
 */
public final class RecipeSelectionSideButton extends SideButton {

    private static final ResourceLocation ICON =
            new ResourceLocation(RsPolymorph.MOD_ID, "textures/gui/sprites/widget/side_button/polymorph.png");
    private static final ITextComponent WARNING_MARK = new StringTextComponent("!");
    private static final int WARNING_COLOR = 0xFFFF5555;
    private static final int ICON_SIZE = 16;

    /** Gold of the popup's active-recipe frame; the alpha is applied per frame for the pulse. */
    private static final int PULSE_RGB = 0xFFD24A;
    private static final long PULSE_MS = 1300L;

    private final GridRecipeWidget widget;
    private final String tooltip;

    RecipeSelectionSideButton(BaseScreen<?> screen, GridRecipeWidget widget) {
        super(screen);
        this.widget = widget;
        this.tooltip = I18n.get("rspolymorph.gui.recipe_selection")
                + "\n" + TextFormatting.GRAY + I18n.get("rspolymorph.gui.click_to_select")
                + "\n" + TextFormatting.RED + I18n.get("rspolymorph.gui.multiple_recipes_warning");
    }

    @Override
    public String getTooltip() {
        return tooltip;
    }

    @Override
    protected void renderButtonIcon(int x, int y) {
        Canvas canvas = widget.canvas();
        canvas.texture(ICON, x, y, ICON_SIZE);
        canvas.text(WARNING_MARK, x + ICON_SIZE - 4, y, WARNING_COLOR, true);
        // The hover layer of the button blits next and relies on Refined Storage's icons being bound.
        screen.bindTexture(RS.ID, "icons.png");
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        super.renderButton(mouseX, mouseY, partialTicks);
        if (!widget.isPopupOpen()) {
            drawPulse(widget.canvas());
        }
    }

    /** A 1 px gold halo whose alpha follows a cosine. */
    private void drawPulse(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
        float pulse = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        int color = ((70 + (int) (pulse * 185.0f)) << 24) | PULSE_RGB;
        canvas.fill(x - 1, y - 1, x + w + 1, y, color);
        canvas.fill(x - 1, y + h, x + w + 1, y + h + 1, color);
        canvas.fill(x - 1, y, x, y + h, color);
        canvas.fill(x + w, y, x + w + 1, y + h, color);
    }

    @Override
    public void onPress() {
        widget.togglePopup();
    }
}
