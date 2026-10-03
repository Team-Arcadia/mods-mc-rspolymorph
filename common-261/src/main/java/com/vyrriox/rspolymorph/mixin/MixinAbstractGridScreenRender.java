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
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes clicks on the RS grid screen to the recipe-selection popup / tutorial card.
 *
 * The popup RENDER lives in {@link MixinScreenPopupRender} (on vanilla
 * {@code AbstractContainerScreen.extractRenderState}), NOT here: the 26.x extract pipeline runs
 * extractContents -> extractCarriedItem -> extractSnapbackItem -> extractTooltip, so drawing at
 * {@code AbstractGridScreen.extractContents} (the first phase) left the popup buried under the
 * carried-item / tooltip strata extracted afterwards. See {@link MixinScreenPopupRender}.
 *
 * IMPORTANT (MC 26.x): mixin {@code @Inject} only resolves methods DECLARED in the target class,
 * not inherited ones. {@code AbstractGridScreen} overrides
 * {@code mouseClicked(MouseButtonEvent, boolean)}, so we target that — NOT the old
 * {@code mouseClicked(DDI)Z}, which is not declared here and would fail to apply (critical
 * injection error at class load).
 *
 * Author: vyrriox
 */
@Mixin(value = AbstractGridScreen.class, remap = false)
public abstract class MixinAbstractGridScreenRender {

    @Inject(
            method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"),
            cancellable = true,
            remap = true
    )
    private void rspolymorph$popupClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        // Route the click when EITHER the popup or the first-open tutorial card is up — both are
        // handled by handleClick. Gating only on isPopupOpen() left the tutorial undismissable.
        if (widget == null || (!widget.isPopupOpen() && !widget.isShowingTutorial())) return;
        if (widget.handleClick(event.x(), event.y())) {
            cir.setReturnValue(true); // consumed — don't let the click reach slots
        }
    }
}
