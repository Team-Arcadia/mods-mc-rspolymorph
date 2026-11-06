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

import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.api.network.grid.IGrid;
import com.refinedmods.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.refinedmods.refinedstorage.screen.BaseScreen;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.vyrriox.rspolymorph.client.GridRecipeWidget;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Connects a {@link GridRecipeWidget} to each crafting or pattern grid screen. The screen owns its
 * widget, and left out when Polymorph owns the grids (see {@code RsPolymorphMixinPlugin}), so no
 * button or popup of this mod exists then. Methods Refined Storage overrides from vanilla need {@code remap = true} (their names are
 * SRG names outside of development), its own methods do not.
 *
 * Author: vyrriox
 */
@Mixin(value = GridScreen.class, remap = false)
public abstract class MixinGridScreen {

    @Shadow
    @Final
    private IGrid grid;

    @Unique
    private GridRecipeWidget rspolymorph$widget;

    /** Runs again on every resize, which also clears the screen's buttons. */
    @Inject(method = "onPostInit", at = @At("RETURN"))
    private void rspolymorph$attachWidget(int x, int y, CallbackInfo ci) {
        if (rspolymorph$widget == null
                && grid instanceof GridNetworkNode node
                && (grid.getGridType() == GridType.CRAFTING || grid.getGridType() == GridType.PATTERN)) {
            rspolymorph$widget = new GridRecipeWidget((BaseScreen<?>) (Object) this, node);
        }
        if (rspolymorph$widget != null) {
            rspolymorph$widget.attachButton();
        }
    }

    @Inject(method = "tick(II)V", at = @At("RETURN"))
    private void rspolymorph$tickWidget(int x, int y, CallbackInfo ci) {
        if (rspolymorph$widget != null) {
            rspolymorph$widget.tick();
        }
    }

    /** Moves the cursor out of the screen's reach under an overlay: no slot hover and no tooltip. */
    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 0, argsOnly = true, remap = true)
    private int rspolymorph$hideMouseX(int mouseX) {
        return rspolymorph$widget != null && rspolymorph$widget.hidesHover() ? -1 : mouseX;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"), ordinal = 1, argsOnly = true, remap = true)
    private int rspolymorph$hideMouseY(int mouseY) {
        return rspolymorph$widget != null && rspolymorph$widget.hidesHover() ? -1 : mouseY;
    }

    @Inject(method = "render", at = @At("TAIL"), remap = true)
    private void rspolymorph$renderOverlays(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (rspolymorph$widget != null) {
            rspolymorph$widget.canvas().begin(graphics);
            rspolymorph$widget.renderOverlays();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = true)
    private void rspolymorph$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (rspolymorph$widget != null && rspolymorph$widget.mouseClicked(mouseX, mouseY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, remap = true)
    private void rspolymorph$mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (rspolymorph$widget != null && rspolymorph$widget.consumeRelease()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, remap = true)
    private void rspolymorph$keyPressed(int key, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (rspolymorph$widget != null && rspolymorph$widget.keyPressed(key)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true, remap = true)
    private void rspolymorph$charTyped(char typed, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (rspolymorph$widget != null && rspolymorph$widget.consumeChar()) {
            cir.setReturnValue(true);
        }
    }
}
