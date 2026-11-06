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
package com.vyrriox.rspolymorph;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.vyrriox.rspolymorph.coremod.GridHooks;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * Added by the coremod to every Refined Storage grid node, along with the methods it generates here: each
 * node keeps its own selected recipe.
 *
 * Author: vyrriox
 */
public interface IRsPolymorphGrid {

    @Nullable
    ResourceLocation rspolymorph$getSelected();

    void rspolymorph$setSelected(@Nullable ResourceLocation recipeId);

    @Nullable
    ICraftingRecipe rspolymorph$getCurrentRecipe();

    /**
     * Selects a recipe if it exists, is a crafting recipe and matches the current matrix, then
     * refreshes the result slot.
     *
     * @return false if the recipe was refused, the node is then unchanged
     */
    default boolean rspolymorph$select(ResourceLocation recipeId) {
        return GridHooks.select((GridNetworkNode) this, recipeId);
    }
}
