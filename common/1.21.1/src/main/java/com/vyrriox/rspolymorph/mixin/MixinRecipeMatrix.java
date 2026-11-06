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

import com.vyrriox.rspolymorph.IRsMatrixContainer;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Mixin for the RS RecipeMatrix: applies the selected recipe to the grid's result.
 * Author: vyrriox
 */
@Mixin(value = RecipeMatrix.class, remap = false)
public abstract class MixinRecipeMatrix<T extends Recipe<I>, I extends RecipeInput> implements IRsRecipeMatrix<T, I> {

    @Shadow @Final private RecipeType<T> recipeType;
    @Shadow @Final private RecipeMatrixContainer matrix;
    @Shadow @Final private Function<RecipeMatrixContainer, I> inputProvider;

    /** Selection of a BlockEntity-free grid; collected together with this matrix. */
    @Unique
    private ResourceLocation rspolymorph$selection;

    @Override
    public RecipeType<T> rspolymorph$getRecipeType() {
        return recipeType;
    }

    @Override
    public Function<RecipeMatrixContainer, I> rspolymorph$getInputProvider() {
        return inputProvider;
    }

    @Override
    public ResourceLocation rspolymorph$getSelection() {
        return rspolymorph$selection;
    }

    @Override
    public void rspolymorph$setSelection(ResourceLocation recipeId) {
        this.rspolymorph$selection = recipeId;
    }

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_init(Runnable r, Supplier<Level> levelSupplier, int w, int h,
                                  Function<RecipeMatrixContainer, I> provider, RecipeType<T> type,
                                  CallbackInfo ci) {
        ((IRsMatrixContainer) this.matrix).rspolymorph$setRecipeMatrix((RecipeMatrix<?, ?>) (Object) this);
    }

    /**
     * After RS has resolved its default recipe, override it with the selection when that selection
     * is still valid for the current input.
     *
     * The guard mirrors the start of RS's updateResult: on the client or while the matrix is muted
     * (RS mutes it during every take, up to 576 slot changes for one shift-craft), RS returned at
     * once and this injection must not do the work RS just skipped.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "updateResult", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_afterUpdateResult(Level level, CallbackInfo ci) {
        if (level.isClientSide() || this.matrix.isMuted()) return;

        ResourceLocation selectedId = RsPolymorph.selectionOf((RecipeMatrix<?, ?>) (Object) this);
        if (selectedId == null) return;

        // RS has just validated currentRecipe against the input, so an equal id needs no lookup.
        AccessorRecipeMatrix<T, I> accessor = (AccessorRecipeMatrix<T, I>) this;
        RecipeHolder<T> current = accessor.rspolymorph$getCurrentRecipe();
        if (current != null && RsPolymorph.recipeId(current).equals(selectedId)) return;
        if (this.matrix.isEmpty()) return;

        I input = this.inputProvider.apply(this.matrix);
        if (input == null) return;
        RecipeHolder<T> selected = RsPolymorph.resolve(level, selectedId, this.recipeType, input);
        if (selected == null) return;

        ItemStack output = RsPolymorph.assemble(selected, input, level);
        // Sync currentRecipe too, otherwise the next updateResult reverts to the old recipe through
        // RS's `currentRecipe.matches(input)` check.
        accessor.rspolymorph$setCurrentRecipe(selected);
        accessor.rspolymorph$invokeSetResult(selected, output);
    }
}
