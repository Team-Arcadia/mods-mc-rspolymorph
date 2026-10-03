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

import com.vyrriox.rspolymorph.client.ClientSetup;
import com.vyrriox.rspolymorph.client.RecipeSelectionSideButton;
import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the recipe-selection side button to RS grid screens and builds the per-screen selection
 * widget. Runs at the RETURN of {@code init()} so all native side buttons and slots already exist.
 *
 * Author: vyrriox
 */
@Mixin(value = AbstractBaseScreen.class, remap = false)
public abstract class MixinAbstractBaseScreen {

    @Inject(method = "init", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_init(CallbackInfo ci) {
        AbstractBaseScreen<?> screen = (AbstractBaseScreen<?>) (Object) this;

        // Only on crafting-capable grid screens.
        if (!(screen instanceof AbstractGridScreen<?> gridScreen)) {
            return;
        }

        // Build the selection widget (locates the result slot, registers the active instance)
        // before adding the button, so the button's press handler has a live widget to drive.
        ClientSetup.onGridScreenInit(gridScreen);
        screen.addSideButton(new RecipeSelectionSideButton());
    }
}
