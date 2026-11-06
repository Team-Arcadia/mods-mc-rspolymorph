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

import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.vyrriox.rspolymorph.IRsMatrixContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stores the owning block entity and the wrapping recipe matrix on the container itself.
 *
 * Author: vyrriox
 */
@Mixin(value = RecipeMatrixContainer.class, remap = false)
public abstract class MixinRecipeMatrixContainer implements IRsMatrixContainer {

    @Unique
    private BlockEntity rspolymorph$owner;

    @Unique
    private RecipeMatrix<?, ?> rspolymorph$recipeMatrix;

    @Override
    public BlockEntity rspolymorph$getOwner() {
        return rspolymorph$owner;
    }

    @Override
    public void rspolymorph$setOwner(BlockEntity owner) {
        this.rspolymorph$owner = owner;
    }

    @Override
    public RecipeMatrix<?, ?> rspolymorph$getRecipeMatrix() {
        return rspolymorph$recipeMatrix;
    }

    @Override
    public void rspolymorph$setRecipeMatrix(RecipeMatrix<?, ?> matrix) {
        this.rspolymorph$recipeMatrix = matrix;
    }
}
