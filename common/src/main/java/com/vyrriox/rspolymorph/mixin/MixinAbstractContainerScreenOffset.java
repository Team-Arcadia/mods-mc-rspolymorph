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

import com.vyrriox.rspolymorph.client.AccessorScreenOffset;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Exposes {@code AbstractContainerScreen}'s protected {@code leftPos}/{@code topPos} via the
 * {@link AccessorScreenOffset} duck-type interface, so the recipe popup can be anchored in screen
 * coordinates. Vanilla target → remapped (default {@code remap=true}).
 *
 * Author: vyrriox
 */
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreenOffset implements AccessorScreenOffset {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;

    @Override
    public int rspolymorph$leftPos() {
        return leftPos;
    }

    @Override
    public int rspolymorph$topPos() {
        return topPos;
    }
}
