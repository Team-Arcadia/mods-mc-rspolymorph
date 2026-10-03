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

import com.refinedmods.refinedstorage.common.grid.AbstractCraftingGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor for AbstractCraftingGridContainerMenu to expose the craftingGrid field.
 *
 * The wired CraftingGridBlockEntity and the Quartz Arsenal WirelessCraftingGrid both
 * implement RS2's {@link CraftingGrid} interface, but only the wired one is a BlockEntity.
 * The wireless grid is a transient, player-bound object with no BlockEntity, so the
 * BlockEntity-keyed selection path cannot reach it. This accessor lets the server-side
 * packet handler resolve the open grid's {@link CraftingGrid} directly from the menu and
 * drive its RecipeMatrix without needing a BlockEntity.
 *
 * Targets the RS2 base class (not any Quartz Arsenal class), so it applies to every
 * crafting grid menu and keeps Quartz Arsenal an optional, soft dependency.
 *
 * Author: vyrriox
 */
@Mixin(value = AbstractCraftingGridContainerMenu.class, remap = false)
public interface AccessorAbstractCraftingGridContainerMenu {
    @Accessor(value = "craftingGrid", remap = false)
    CraftingGrid rspolymorph$getCraftingGrid();
}
