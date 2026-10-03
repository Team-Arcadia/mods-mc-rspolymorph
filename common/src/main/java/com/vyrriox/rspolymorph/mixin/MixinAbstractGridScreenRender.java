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

import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.vyrriox.rspolymorph.client.RsGridRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the standalone recipe-selection popup over the RS grid screen and routes clicks to it,
 * replacing the role Polymorph's {@code SelectionWidget} + its render mixin used to play.
 *
 * The class target is RS's {@code AbstractGridScreen} ({@code remap=false}), but {@code render}
 * and {@code mouseClicked} are inherited vanilla methods, so those injectors use {@code remap=true}
 * to resolve the obfuscated names on both loaders.
 *
 * Author: vyrriox
 */
@Mixin(value = AbstractGridScreen.class, remap = false)
public abstract class MixinAbstractGridScreenRender {

    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At("RETURN"),
            remap = true
    )
    private void rspolymorph$renderPopup(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        if (widget != null && widget.isOpenForScreen((AbstractContainerScreen<?>) (Object) this)) {
            widget.renderPopup(graphics, mouseX, mouseY);
        }
    }

    @Inject(
            method = "mouseClicked(DDI)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void rspolymorph$popupClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        // Route the click when EITHER the popup or the first-open tutorial card is up — both are
        // handled by handleClick. Gating only on isPopupOpen() left the tutorial undismissable.
        if (widget == null || (!widget.isPopupOpen() && !widget.isShowingTutorial())) return;
        if (widget.handleClick(mouseX, mouseY)) {
            cir.setReturnValue(true); // consumed — don't let the click reach slots
        }
    }

    // NOTE: no Screen#removed() hook — removed() is an inherited vanilla method that no RS screen
    // class declares, so a mixin on AbstractGridScreen cannot target it (it would fail to apply and
    // crash at class load). The active widget is instead bounded by replacement: every grid open
    // runs init() -> ClientSetup.onGridScreenInit() which overwrites activeInstance, so at most one
    // closed screen is retained until the next grid opens (one object, not a growing leak).
}
