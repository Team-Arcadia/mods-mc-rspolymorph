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

import net.minecraft.resources.ResourceLocation;

/**
 * Added by {@code MixinGridNetworkNode} to every Refined Storage grid node: each node keeps its own
 * selected recipe.
 *
 * Author: vyrriox
 */
public interface IRsPolymorphGrid {

    /**
     * Selects a recipe if it exists, is a crafting recipe and matches the current matrix, then
     * refreshes the result slot.
     *
     * @return false if the recipe was refused, the node is then unchanged
     */
    boolean rspolymorph$select(ResourceLocation recipeId);
}
