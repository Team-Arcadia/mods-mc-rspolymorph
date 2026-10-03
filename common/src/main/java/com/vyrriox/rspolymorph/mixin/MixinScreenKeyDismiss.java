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

import com.vyrriox.rspolymorph.client.RsGridRecipeWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets keyboard-only / Steam Deck players dismiss the first-open tutorial card with Space or Enter.
 * Vanilla Esc is deliberately left untouched so it keeps its close-the-screen meaning. HEAD-cancel
 * on the container screen's {@code keyPressed}; {@code require = 0} so a differing signature (e.g. the
 * MC 26.x KeyEvent-based key handling) is a clean no-op, never a class-load failure. Vanilla target →
 * remapped (Loom remaps for Fabric, NeoForge runs Mojmap). No-op when no tutorial is up, so it never
 * interferes with the grid search box.
 *
 * Author: vyrriox
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinScreenKeyDismiss {

    @Inject(
            method = "keyPressed(III)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void rspolymorph$keyDismissTutorial(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (keyCode != GLFW.GLFW_KEY_SPACE && keyCode != GLFW.GLFW_KEY_ENTER && keyCode != GLFW.GLFW_KEY_KP_ENTER) {
            return;
        }
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        if (widget != null && widget.dismissTutorialByKey()) {
            cir.setReturnValue(true); // consumed — don't pass the key to the screen
        }
    }
}
