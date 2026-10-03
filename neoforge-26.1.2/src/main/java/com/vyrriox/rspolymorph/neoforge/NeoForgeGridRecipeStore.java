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
package com.vyrriox.rspolymorph.neoforge;

import com.vyrriox.rspolymorph.platform.GridRecipeStore;
import com.vyrriox.rspolymorph.platform.GridSelectionData;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * NeoForge implementation of {@link GridRecipeStore} backed by a serializable
 * {@link AttachmentType} on the grid block entity. Resolved by the common code via
 * {@code ServiceLoader} (see {@code META-INF/services}).
 *
 * The attachment instance is registered by {@link RsPolymorphNeoForge} and supplied here.
 *
 * Author: vyrriox
 */
public final class NeoForgeGridRecipeStore implements GridRecipeStore {

    /** Set once by the entrypoint after the DeferredRegister has registered the type. */
    private static Supplier<AttachmentType<Map<Identifier, Identifier>>> attachment;

    static void bind(Supplier<AttachmentType<Map<Identifier, Identifier>>> type) {
        attachment = type;
    }

    @Override
    public Identifier get(BlockEntity be, RecipeType<?> type) {
        if (be == null || attachment == null) return null;
        Identifier typeId = GridSelectionData.typeId(type);
        if (typeId == null) return null;
        Map<Identifier, Identifier> map = be.getData(attachment.get());
        return map.get(typeId);
    }

    @Override
    public void set(BlockEntity be, RecipeType<?> type, Identifier recipeId) {
        if (be == null || attachment == null) return;
        Identifier typeId = GridSelectionData.typeId(type);
        if (typeId == null) return;

        // Codec.unboundedMap decodes to an ImmutableMap, so the value from getData (after a
        // serialization round-trip) cannot be mutated in place — map.put/remove would throw
        // UnsupportedOperationException. Copy-on-write into a fresh mutable map, then setData it back.
        Map<Identifier, Identifier> current = be.getData(attachment.get());
        Map<Identifier, Identifier> next = new HashMap<>(current);
        if (recipeId == null) {
            next.remove(typeId);
        } else {
            next.put(typeId, recipeId);
        }
        be.setData(attachment.get(), next);
        be.setChanged();
    }
}
