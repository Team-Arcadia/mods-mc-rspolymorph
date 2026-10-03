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
package com.vyrriox.rspolymorph.mixin;

import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridScreen;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.grid.screen.CraftingGridScreen;
import com.vyrriox.rspolymorph.client.RsGridRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the grid screen's hovered-slot tooltip while the recipe-selection popup is open AND
 * the cursor is over it, so only the popup's own recipe-name tooltip shows (no overlapping "Stick"
 * double text).
 *
 * Why the RS grid screen classes and not {@code AbstractContainerScreen}: Refined Storage's grid
 * screens render tooltips through their OWN {@code renderTooltip(GuiGraphics,int,int)} override
 * chain (CraftingGridScreen / PatternGridScreen override it; AbstractGridScreen declares it). The
 * grid screen's {@code render()} dispatches to that override via {@code invokevirtual}, so a cancel
 * on the vanilla {@code AbstractContainerScreen.renderTooltip} never fires for these screens.
 * Cancelling the entry override's HEAD stops the whole tooltip pass (its own resource-slot tooltips
 * and the vanilla hovered-slot tooltip it draws via super) in one shot.
 *
 * {@code require = 0}: fail-soft — never fails the load if a target lacks the method on some RS
 * build. Vanilla-named target on RS classes → remapped (default {@code remap=true}; Loom remaps it
 * at build for Fabric, NeoForge runs Mojmap).
 *
 * Author: vyrriox
 */
@Mixin({CraftingGridScreen.class, PatternGridScreen.class, AbstractGridScreen.class})
public abstract class MixinScreenTooltipSuppress {

    @Inject(
            method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void rspolymorph$suppressSlotTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        // Gate on popup-open, NOT mouse-over-popup. RS's CraftingGridScreen.renderTooltip draws the
        // underlying matrix slot's tooltip ("Stick") itself and returns before super, and that slot
        // can sit OUTSIDE the popup box — so a mouse-over gate is skipped exactly when the overlap
        // happens. The popup is a modal selector: while it is open, suppress the screen's whole
        // tooltip pass; the popup renders its own recipe-name tooltip for its hovered entry.
        if (widget != null && widget.isPopupOpen()) {
            ci.cancel();
        }
    }
}
