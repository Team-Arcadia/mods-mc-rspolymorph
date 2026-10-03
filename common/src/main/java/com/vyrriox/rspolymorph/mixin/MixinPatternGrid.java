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
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternGridBlockEntity.class, remap = false)
public abstract class MixinPatternGrid {
    
    @Shadow abstract RecipeMatrixContainer getCraftingMatrix();
    @Shadow abstract RecipeMatrixContainer getSmithingTableMatrix();

    @Inject(method = "<init>(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_init(BlockPos pos, BlockState state, CallbackInfo ci) {
        PatternGridBlockEntity be = (PatternGridBlockEntity) (Object) this;
        RecipeMatrixContainer c = this.getCraftingMatrix();
        if (c != null) {
            RsPolymorph.registerContainerBlockEntity(c, be);
        }
        RecipeMatrixContainer st = this.getSmithingTableMatrix();
        if (st != null) {
            RsPolymorph.registerContainerBlockEntity(st, be);
        }
    }

    /**
     * Tags the newly-created crafting pattern ItemStack with the Polymorph-selected recipe ID
     * so that PatternResolver can prefer the correct recipe at autocrafting time.
     *
     * Reads the selection from two sources (in priority order):
     *  1. Static selectedRecipeId — fast path for singleplayer (shared JVM).
     *  2. The persisted per-grid selection store ({@code Services.GRID_STORE}) — works on
     *     dedicated servers where the static field may already be cleared by the time RS2's
     *     createCraftingPattern packet arrives.
     */
    @Inject(method = "createCraftingPattern()Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_tagWithSelectedRecipe(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result == null || result.isEmpty()) return;

        // 1) Static fast path (singleplayer)
        ResourceLocation selectedId = RsPolymorph.getSelectedRecipeId();

        // 2) Persisted selection from the grid store (dedicated server)
        if (selectedId == null) {
            PatternGridBlockEntity be = (PatternGridBlockEntity) (Object) this;
            selectedId = Services.GRID_STORE.get(be, RecipeType.CRAFTING);
        }

        if (selectedId != null) {
            result.set(RsPolymorph.selectedRecipeComponent(), selectedId);
        }
    }
}
