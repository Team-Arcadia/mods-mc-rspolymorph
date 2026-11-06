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
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.vyrriox.rspolymorph.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Loader-agnostic core for RS Polymorph, which adds recipe selection to the Refined Storage grids.
 * It holds the data component, the grid links and the selection logic, and is shared by the NeoForge
 * and Fabric entrypoints. It keeps no per-grid or per-player state: the links are fields on RS's
 * own objects (see {@link IRsMatrixContainer} and {@link IRsRecipeMatrix}).
 *
 * IMPORTANT: This class must NEVER reference loader-specific types (NeoForge {@code @Mod},
 * Fabric {@code ModInitializer}, {@code DeferredRegister}, {@code PacketDistributor}, ...) or
 * client-only types. Loader wiring lives in the {@code neoforge}/{@code fabric} subprojects;
 * the data-component instance is injected by each loader via {@link #setSelectedRecipeComponent}.
 *
 * The methods under "Version-specific recipe API" are what differs between Minecraft versions, and
 * why this class is forked under {@code common/26.1.2}. Keep everything else identical in both.
 *
 * Author: vyrriox
 */
public final class RsPolymorph {

    public static final String MOD_ID = "rspolymorph";

    private RsPolymorph() {}

    // Data component
    /**
     * The {@code selected_recipe} data component, stored on crafting pattern items to persist the
     * recipe the pattern was printed with. Registered by each loader (NeoForge
     * {@code DeferredRegister} / Fabric {@code Registry.register}) and injected here via
     * {@link #setSelectedRecipeComponent}.
     */
    private static volatile DataComponentType<ResourceLocation> selectedRecipeComponent;

    /** Stable resource id for the data component; both loaders register under this id. */
    public static final ResourceLocation SELECTED_RECIPE_COMPONENT_ID =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "selected_recipe");

    public static void setSelectedRecipeComponent(DataComponentType<ResourceLocation> component) {
        selectedRecipeComponent = component;
    }

    /** The registered {@code selected_recipe} data component. Never null after loader setup. */
    public static DataComponentType<ResourceLocation> selectedRecipeComponent() {
        DataComponentType<ResourceLocation> c = selectedRecipeComponent;
        if (c == null) {
            throw new IllegalStateException(
                    "selected_recipe data component accessed before loader registration");
        }
        return c;
    }

    // Grid links

    /** Records the block entity owning {@code container}. Called from the grid constructors. */
    public static void setOwner(RecipeMatrixContainer container, BlockEntity owner) {
        ((IRsMatrixContainer) container).rspolymorph$setOwner(owner);
    }

    /** The block entity owning {@code container}, or {@code null} for a BlockEntity-free grid. */
    public static BlockEntity ownerOf(RecipeMatrixContainer container) {
        return ((IRsMatrixContainer) container).rspolymorph$getOwner();
    }

    /** The matrix wrapping {@code container}, or {@code null} for a bare client-side container. */
    public static RecipeMatrix<?, ?> matrixOf(RecipeMatrixContainer container) {
        return ((IRsMatrixContainer) container).rspolymorph$getRecipeMatrix();
    }

    // Selection

    /**
     * The recipe selected for {@code matrix}: from the persistent store if the grid has a block
     * entity, otherwise from the matrix itself (e.g. the Quartz Arsenal wireless crafting grid).
     */
    public static ResourceLocation selectionOf(RecipeMatrix<?, ?> matrix) {
        IRsRecipeMatrix<?, ?> rsMatrix = (IRsRecipeMatrix<?, ?>) matrix;
        BlockEntity owner = ownerOf(matrix.getMatrix());
        return owner != null
                ? Services.GRID_STORE.get(owner, rsMatrix.rspolymorph$getRecipeType())
                : rsMatrix.rspolymorph$getSelection();
    }

    /** Stores the selection in the place {@link #selectionOf} reads it from. */
    public static void select(RecipeMatrix<?, ?> matrix, ResourceLocation recipeId) {
        IRsRecipeMatrix<?, ?> rsMatrix = (IRsRecipeMatrix<?, ?>) matrix;
        BlockEntity owner = ownerOf(matrix.getMatrix());
        if (owner != null) {
            Services.GRID_STORE.set(owner, rsMatrix.rspolymorph$getRecipeType(), recipeId);
        } else {
            rsMatrix.rspolymorph$setSelection(recipeId);
        }
    }

    /**
     * Resolves {@code recipeId} for a matrix of the given type and input. The type is checked BEFORE
     * {@code matches}: a recipe of another type expects another input class, and its bridge method
     * would throw ClassCastException.
     *
     * @return the recipe, or {@code null} if it is unknown, of another type, or does not match
     */
    @SuppressWarnings("unchecked")
    public static <T extends Recipe<I>, I extends RecipeInput> RecipeHolder<T> resolve(
            Level level, ResourceLocation recipeId, RecipeType<T> type, I input) {
        RecipeHolder<?> holder = findRecipe(level, recipeId);
        if (holder == null) return null;
        RecipeType<?> actualType = holder.value().getType();
        if (actualType != type) return null;
        return ((Recipe<I>) holder.value()).matches(input, level) ? (RecipeHolder<T>) holder : null;
    }

    /** True if {@code recipeId} is a recipe of the matrix's type that matches its current input. */
    @SuppressWarnings("unchecked")
    public static boolean matchesCurrentInput(RecipeMatrix<?, ?> matrix, ResourceLocation recipeId, Level level) {
        IRsRecipeMatrix<Recipe<RecipeInput>, RecipeInput> rsMatrix =
                (IRsRecipeMatrix<Recipe<RecipeInput>, RecipeInput>) (Object) matrix;
        RecipeInput input = rsMatrix.rspolymorph$getInputProvider().apply(matrix.getMatrix());
        return input != null
                && resolve(level, recipeId, rsMatrix.rspolymorph$getRecipeType(), input) != null;
    }

    // Version-specific recipe API

    /** The recipe with this id, or {@code null}. Server side only on the 26.x line. */
    public static RecipeHolder<?> findRecipe(Level level, ResourceLocation recipeId) {
        return level.getRecipeManager().byKey(recipeId).orElse(null);
    }

    /** The id of {@code holder} as the type stored in selections and on patterns. */
    public static ResourceLocation recipeId(RecipeHolder<?> holder) {
        return holder.id();
    }

    /** Assembles the output of {@code holder} for {@code input}. */
    @SuppressWarnings("unchecked")
    public static ItemStack assemble(RecipeHolder<?> holder, RecipeInput input, Level level) {
        return ((Recipe<RecipeInput>) holder.value()).assemble(input, level.registryAccess());
    }
}
