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

import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.function.Function;

/**
 * Interface for the RS RecipeMatrix to expose its type and input info, and to hold the selection of
 * a grid that has no block entity to persist it on.
 * Author: vyrriox
 */
public interface IRsRecipeMatrix<T extends Recipe<I>, I extends RecipeInput> {
    RecipeType<T> rspolymorph$getRecipeType();
    Function<RecipeMatrixContainer, I> rspolymorph$getInputProvider();

    /** In-memory selection of a BlockEntity-free grid (it lives and dies with this matrix). */
    ResourceLocation rspolymorph$getSelection();
    void rspolymorph$setSelection(ResourceLocation recipeId);
}
