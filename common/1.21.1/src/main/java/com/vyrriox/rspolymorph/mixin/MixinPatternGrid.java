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

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.platform.Services;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class MixinPatternGrid {

    @Shadow @Final private RecipeMatrix<CraftingRecipe, CraftingInput> craftingRecipe;

    @Shadow abstract RecipeMatrixContainer getCraftingMatrix();
    @Shadow abstract RecipeMatrixContainer getSmithingTableMatrix();

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_init(BlockPos pos, BlockState state, CallbackInfo ci) {
        PatternGridBlockEntity be = (PatternGridBlockEntity) (Object) this;
        RecipeMatrixContainer c = this.getCraftingMatrix();
        if (c != null) {
            RsPolymorph.setOwner(c, be);
        }
        RecipeMatrixContainer st = this.getSmithingTableMatrix();
        if (st != null) {
            RsPolymorph.setOwner(st, be);
        }
    }

    /**
     * Tags the new crafting pattern with the recipe the grid is actually previewing, so that
     * PatternResolver resolves the same recipe at autocrafting time. The matrix's current recipe is
     * the right source: the stored selection may be for other inputs, and RS keeps a current recipe
     * while it still matches even if another recipe would come first in a fresh lookup.
     */
    @Inject(method = "createCraftingPattern()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_tagWithCurrentRecipe(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;

        RecipeHolder<?> current = ((AccessorRecipeMatrix<?, ?>) (Object) this.craftingRecipe).rspolymorph$getCurrentRecipe();
        if (current != null) {
            result.set(RsPolymorph.selectedRecipeComponent(), RsPolymorph.recipeId(current));
        }
    }

    /**
     * Loading a tagged pattern back into the grid restores its recipe, otherwise re-encoding it would
     * silently print whichever recipe the grid selected last.
     */
    @Inject(method = "copyPattern", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_restoreSelection(ItemStack stack, CallbackInfo ci) {
        ResourceLocation recipeId = stack.get(RsPolymorph.selectedRecipeComponent());
        if (recipeId == null) return;

        PatternGridBlockEntity be = (PatternGridBlockEntity) (Object) this;
        Level level = be.getLevel();
        if (level == null || level.isClientSide()) return;

        RecipeHolder<?> recipe = RsPolymorph.findRecipe(level, recipeId);
        if (recipe == null) return;
        RecipeType<?> type = recipe.value().getType();
        if (type != RecipeType.CRAFTING) return;

        Services.GRID_STORE.set(be, RecipeType.CRAFTING, recipeId);
        this.craftingRecipe.updateResult(level);
    }
}
