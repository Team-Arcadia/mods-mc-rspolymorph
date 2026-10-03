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

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for AbstractGridContainerMenu to expose grid field.
 * Author: vyrriox
 */
@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public interface AccessorAbstractGridContainerMenu {
    @Accessor(value = "grid", remap = false)
    Grid rspolymorph$getGrid();

    @Accessor(value = "playerInventory", remap = false)
    net.minecraft.world.entity.player.Inventory rspolymorph$getPlayerInventory();
}
