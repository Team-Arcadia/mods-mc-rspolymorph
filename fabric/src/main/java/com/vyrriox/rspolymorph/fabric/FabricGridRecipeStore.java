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
package com.vyrriox.rspolymorph.fabric;

import com.vyrriox.rspolymorph.platform.GridRecipeStore;
import com.vyrriox.rspolymorph.platform.GridSelectionData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric implementation of {@link GridRecipeStore} backed by a persistent attachment on the grid
 * block entity. Resolved by the common code via {@code ServiceLoader} (see {@code META-INF/services}).
 *
 * The attachment is registered statically here (Fabric attachments can be created at class-init
 * time, unlike NeoForge's registry-bound types).
 *
 * Author: vyrriox
 */
public final class FabricGridRecipeStore implements GridRecipeStore {

    private static final AttachmentType<Map<ResourceLocation, ResourceLocation>> GRID_SELECTION =
            AttachmentRegistry.create(
                    GridSelectionData.ATTACHMENT_ID,
                    builder -> builder
                            .initializer(GridSelectionData::empty)
                            .persistent(GridSelectionData.CODEC));

    @Override
    public ResourceLocation get(BlockEntity be, RecipeType<?> type) {
        if (be == null) return null;
        ResourceLocation typeId = GridSelectionData.typeId(type);
        if (typeId == null) return null;
        Map<ResourceLocation, ResourceLocation> map = be.getAttached(GRID_SELECTION);
        return map == null ? null : map.get(typeId);
    }

    @Override
    public void set(BlockEntity be, RecipeType<?> type, ResourceLocation recipeId) {
        if (be == null) return;
        ResourceLocation typeId = GridSelectionData.typeId(type);
        if (typeId == null) return;

        Map<ResourceLocation, ResourceLocation> map = be.getAttached(GRID_SELECTION);
        // Copy-on-write: never mutate a possibly-shared default instance in place.
        Map<ResourceLocation, ResourceLocation> next = map == null ? new HashMap<>() : new HashMap<>(map);
        if (recipeId == null) {
            next.remove(typeId);
        } else {
            next.put(typeId, recipeId);
        }
        be.setAttached(GRID_SELECTION, next);
        be.setChanged();
    }
}
