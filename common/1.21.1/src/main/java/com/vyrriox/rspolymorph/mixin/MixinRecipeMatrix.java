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

import com.vyrriox.rspolymorph.IRsRecipeMatrix;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mixin for RS2 RecipeMatrix — integrates Polymorph recipe selection.
 * Author: vyrriox
 */
@Mixin(value = RecipeMatrix.class, remap = false)
public abstract class MixinRecipeMatrix<T extends Recipe<I>, I extends RecipeInput> implements IRsRecipeMatrix<T, I> {

    @Shadow @Final private RecipeType<T> recipeType;
    @Shadow @Final private RecipeMatrixContainer matrix;
    @Shadow @Final private Function<RecipeMatrixContainer, I> inputProvider;

    @Override
    public RecipeType<T> rspolymorph$getRecipeType() {
        return recipeType;
    }

    @Override
    public Function<RecipeMatrixContainer, I> rspolymorph$getInputProvider() {
        return inputProvider;
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_init(Runnable r, Supplier<Level> levelSupplier, int w, int h,
                                  Function<RecipeMatrixContainer, I> provider, RecipeType<T> type,
                                  CallbackInfo ci) {
        RsPolymorph.registerMatrixToContainer(this.matrix, (RecipeMatrix<?, ?>) (Object) this);
    }

    /**
     * After RS2 has resolved its default recipe, override with the user's selection if one is
     * active for this matrix.
     *
     * Strategy (in priority order):
     *  1. Static selectedRecipeId (the just-applied selection; SP and the first server apply).
     *  2. Per-matrix selection (BlockEntity-free grids such as the wireless crafting grid).
     *  3. Persisted per-grid selection (block-entity-backed grids, via Services.GRID_STORE).
     *
     * Early-exit: if the current recipe already matches the selection, skip the search entirely.
     * This is the common case after the initial selection is applied.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "updateResult", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_afterUpdateResult(Level level, CallbackInfo ci) {

        AccessorRecipeMatrix<T, I> accessor = (AccessorRecipeMatrix<T, I>) this;

        // ── 1) Static selection (singleplayer fast path) ──────────────────────
        ResourceLocation selectedId = RsPolymorph.getSelectedRecipeId();
        if (selectedId != null) {
            // Skip the search if RS2 already resolved the right recipe.
            RecipeHolder<T> current = accessor.rspolymorph$getCurrentRecipe();
            if (current != null && current.id().equals(selectedId)) {
                return;
            }

            I input = this.inputProvider.apply(this.matrix);
            if (input != null) {
                List<RecipeHolder<T>> matches = level.getRecipeManager().getRecipesFor(this.recipeType, input, level);
                for (RecipeHolder<T> holder : matches) {
                    if (holder.id().equals(selectedId)) {
                        ItemStack output = holder.value().assemble(input, level.registryAccess());
                        // Sync currentRecipe FIRST so the next updateResult doesn't revert
                        // to the old recipe via the `currentRecipe.matches(input)` fast path.
                        accessor.rspolymorph$setCurrentRecipe(holder);
                        accessor.rspolymorph$invokeSetResult(holder, output);
                        return;
                    }
                }
            }
            // selectedId didn't match this matrix's inputs — fall through to the fallbacks.
        }

        // ── 2) Per-matrix selection (BlockEntity-free grids: wireless crafting grid) ──
        // Grids with no BlockEntity (e.g. Quartz Arsenal's wireless crafting grid) cannot
        // persist a selection on a block entity. SelectRecipePacket stores their choice keyed by
        // this matrix instead, so re-apply it on every updateResult to survive input changes —
        // exactly as the BlockEntity path does below. The selection is intentionally
        // sticky-while-matching: applied only when it is still a valid recipe for the CURRENT
        // inputs, left untouched on a transient mismatch, and reclaimed deterministically on menu
        // close by MixinAbstractGridContainerMenu — so it is never aggressively purged nor unbounded.
        // Read-side BE guard (mirrors setMatrixSelection's write-side guard): a BE-backed matrix
        // must resolve through the persistent grid store in path 3, never through this store.
        ResourceLocation matrixId = RsPolymorph.getBlockEntityForContainer(this.matrix) == null
                ? RsPolymorph.getMatrixSelection((RecipeMatrix<?, ?>) (Object) this)
                : null;
        if (matrixId != null) {
            RecipeHolder<T> current = accessor.rspolymorph$getCurrentRecipe();
            if (current == null || !current.id().equals(matrixId)) {
                I input = this.inputProvider.apply(this.matrix);
                if (input != null) {
                    List<RecipeHolder<T>> matches = level.getRecipeManager().getRecipesFor(this.recipeType, input, level);
                    for (RecipeHolder<T> holder : matches) {
                        if (holder.id().equals(matrixId)) {
                            ItemStack output = holder.value().assemble(input, level.registryAccess());
                            accessor.rspolymorph$setCurrentRecipe(holder);
                            accessor.rspolymorph$invokeSetResult(holder, output);
                            return;
                        }
                    }
                }
            } else {
                // Already correct — nothing to override.
                return;
            }
        }

        // ── 3) Persisted per-grid selection (block-entity-backed grids) ───────────
        RecipeHolder<T> storedRecipe = (RecipeHolder<T>) RsPolymorph.getRecipe((RecipeMatrix<?, ?>) (Object) this, level);
        if (storedRecipe != null) {
            // Early-exit if the result is already correct.
            RecipeHolder<T> current = accessor.rspolymorph$getCurrentRecipe();
            if (current == null || !current.id().equals(storedRecipe.id())) {
                I input = this.inputProvider.apply(this.matrix);
                if (input != null) {
                    ItemStack output = storedRecipe.value().assemble(input, level.registryAccess());
                    accessor.rspolymorph$setCurrentRecipe(storedRecipe);
                    accessor.rspolymorph$invokeSetResult(storedRecipe, output);
                }
            }
        }
    }
}
