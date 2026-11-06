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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Releases the active selection widget when its screen closes, so a closed grid can neither receive
 * input nor pin its menu and level. {@code removed} is declared on the vanilla screen, not on any RS
 * grid screen, hence the vanilla target.
 *
 * Author: vyrriox
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinContainerScreenRemoved {

    @Inject(method = "removed()V", at = @At("RETURN"))
    private void rspolymorph$releaseWidget(CallbackInfo ci) {
        RsGridRecipeWidget.clearIfActive((AbstractContainerScreen<?>) (Object) this);
    }
}
