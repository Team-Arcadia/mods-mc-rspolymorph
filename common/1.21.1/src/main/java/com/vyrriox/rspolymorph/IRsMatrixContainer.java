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

import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Duck-type interface implemented (via {@code MixinRecipeMatrixContainer}) by RS's
 * {@code RecipeMatrixContainer}. The links live on the container itself, so they share its lifetime
 * and no global map can pin a grid or a level.
 *
 * Author: vyrriox
 */
public interface IRsMatrixContainer {
    /** The block entity owning this container, or {@code null} for a BlockEntity-free grid. */
    BlockEntity rspolymorph$getOwner();

    void rspolymorph$setOwner(BlockEntity owner);

    /** The recipe matrix wrapping this container, or {@code null} for a bare client-side container. */
    RecipeMatrix<?, ?> rspolymorph$getRecipeMatrix();

    void rspolymorph$setRecipeMatrix(RecipeMatrix<?, ?> matrix);
}
