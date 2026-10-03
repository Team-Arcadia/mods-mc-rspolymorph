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

import com.refinedmods.refinedstorage.common.support.widget.AbstractSideButtonWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * Native RS grid side button that opens the recipe-selection popup.
 * Uses RS's sprite system for proper theming. No Polymorph dependency.
 *
 * <p>When the open grid currently has more than one candidate recipe (a non-unique craft) the
 * button advertises it two ways — see issue #3, where players reported not noticing the button and
 * accidentally crafting the wrong output:
 * <ul>
 *   <li>RS's native {@code setWarning(...)} draws a red warning icon on the button and adds a red
 *       line to its tooltip.</li>
 *   <li>A gold halo pulses around the button to draw the eye to it (suppressed once the popup is
 *       open, since the button has then served its purpose).</li>
 * </ul>
 *
 * Author: vyrriox
 */
public class RecipeSelectionSideButton extends AbstractSideButtonWidget {

    /** Gold matching the popup's active-recipe frame; the alpha is applied per frame for the pulse. */
    private static final int PULSE_RGB = 0xFFD24A;
    /** Pulse period in ms. */
    private static final long PULSE_MS = 1300L;

    public RecipeSelectionSideButton() {
        super(button -> {
            RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
            if (widget != null) {
                widget.triggerSelection();
            }
        });
    }

    @Override
    protected Identifier getSprite() {
        return Identifier.fromNamespaceAndPath("rspolymorph", "widget/side_button/polymorph");
    }

    @Override
    protected MutableComponent getTitle() {
        return Component.translatable("rspolymorph.gui.recipe_selection");
    }

    @Override
    protected List<MutableComponent> getSubText() {
        return Collections.singletonList(Component.translatable("rspolymorph.gui.click_to_select"));
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        RsGridRecipeWidget rsWidget = RsGridRecipeWidget.getActiveInstance();
        boolean multiple = (rsWidget != null && rsWidget.hasMultipleRecipes());
        this.active = multiple;
        this.visible = true;

        // Native RS warning: red icon on the button + a red tooltip line, so a non-unique craft is
        // flagged and the popup is discoverable. Cleared (null) when the craft is unique.
        setWarning(multiple ? Component.translatable("rspolymorph.gui.multiple_recipes_warning") : null);

        super.extractContents(graphics, mouseX, mouseY, partialTicks);

        if (multiple && (rsWidget == null || !rsWidget.isPopupOpen())) {
            drawAttentionPulse(graphics);
        }
    }

    /** Draws a 1px gold halo around the button whose alpha pulses smoothly (cosine ease). */
    private void drawAttentionPulse(GuiGraphicsExtractor graphics) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        double phase = (System.currentTimeMillis() % PULSE_MS) / (double) PULSE_MS;
        float pulse = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        int alpha = 70 + (int) (pulse * 185.0f);          // 70..255
        int color = (alpha << 24) | PULSE_RGB;
        graphics.fill(x - 1, y - 1, x + w + 1, y, color);          // top
        graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, color);  // bottom
        graphics.fill(x - 1, y, x, y + h, color);                  // left
        graphics.fill(x + w, y, x + w + 1, y + h, color);          // right
    }
}
