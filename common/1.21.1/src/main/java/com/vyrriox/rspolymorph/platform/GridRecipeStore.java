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
package com.vyrriox.rspolymorph.platform;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Loader-agnostic persistence of the player's recipe selection per RS2 grid block entity,
 * keyed by {@link RecipeType}. Replaces Polymorph's {@code IBlockEntityRecipeData} capability.
 *
 * Implemented with Data Attachments on each loader (NeoForge {@code AttachmentType} /
 * Fabric {@code AttachmentRegistry}), which ride the block entity's own NBT save pipeline,
 * so the selection survives world save/reload and chunk unload with no hand-written
 * serialization on RS2's classes. Resolved by the common code via {@link Services#GRID_STORE}.
 *
 * Grids with NO block entity (the Quartz Arsenal wireless crafting grid) are NOT handled here —
 * they keep using the in-memory per-matrix store in {@code RsPolymorph} because they have
 * nowhere durable to persist.
 *
 * Author: vyrriox
 */
public interface GridRecipeStore {

    /**
     * Returns the recipe id selected for {@code be} under {@code type}, or {@code null} if none.
     */
    ResourceLocation get(BlockEntity be, RecipeType<?> type);

    /**
     * Persists {@code recipeId} for {@code be} under {@code type}. A {@code null} id clears the
     * selection for that type. Marks the block entity dirty so the change is saved.
     */
    void set(BlockEntity be, RecipeType<?> type, ResourceLocation recipeId);
}
