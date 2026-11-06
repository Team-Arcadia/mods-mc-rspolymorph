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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the grid screen's tooltip pass while the recipe-selection popup is open, so only the
 * popup's own recipe tooltip shows (no overlapping "Stick" double text).
 *
 * Why the RS grid screen classes and not {@code AbstractContainerScreen}: Refined Storage's grid
 * screens draw tooltips through their OWN {@code extractTooltip(GuiGraphicsExtractor,int,int)} override chain
 * (CraftingGridScreen / PatternGridScreen override it; AbstractGridScreen declares it), and those
 * overrides may return before reaching super. Cancelling the entry override's HEAD stops the whole
 * tooltip pass in one shot.
 *
 * Gate on popup-open, NOT mouse-over-popup: the slot whose tooltip RS draws can sit OUTSIDE the popup
 * box. The popup is a modal selector.
 *
 * {@code require = 0}: never fails the load if a target lacks the method on some RS build.
 *
 * Author: vyrriox
 */
@Mixin({CraftingGridScreen.class, PatternGridScreen.class, AbstractGridScreen.class})
public abstract class MixinScreenTooltipSuppress {

    @Inject(
            method = "extractTooltip(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void rspolymorph$suppressSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        if (widget != null && widget.isPopupOpen()
                && widget.isOpenForScreen((AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
        }
    }
}
