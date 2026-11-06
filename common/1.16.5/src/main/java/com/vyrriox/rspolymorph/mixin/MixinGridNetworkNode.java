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

import com.refinedmods.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.refinedmods.refinedstorage.apiimpl.network.node.NetworkNode;
import com.refinedmods.refinedstorage.inventory.item.BaseItemHandler;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.util.math.BlockPos;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps the selected recipe on the grid node. Refined Storage keeps {@code currentRecipe} as long as
 * it matches the matrix, so setting it before {@code onCraftingMatrixChanged} is enough: the result
 * slot, normal crafts and shift crafts all follow it. A selection that no longer matches is kept but
 * ignored, and applies again when its ingredients come back. Left out when Polymorph owns the grids
 * (see {@code RsPolymorphMixinPlugin}).
 *
 * Author: vyrriox
 */
@Mixin(value = GridNetworkNode.class, remap = false)
public abstract class MixinGridNetworkNode extends NetworkNode implements IRsPolymorphGrid {

    @Shadow
    private ICraftingRecipe currentRecipe;

    @Shadow
    @Final
    private CraftingInventory matrix;

    @Shadow
    @Final
    private BaseItemHandler patterns;

    @Unique
    private ResourceLocation rspolymorph$selected;

    private MixinGridNetworkNode(World world, BlockPos pos) {
        super(world, pos);
    }

    @Shadow
    public abstract void onCraftingMatrixChanged();

    @Shadow
    public abstract boolean isProcessingPattern();

    @Override
    public boolean rspolymorph$select(ResourceLocation recipeId) {
        if (RecipeAccess.findCrafting(world, recipeId, matrix) == null) {
            return false;
        }
        rspolymorph$selected = recipeId;
        onCraftingMatrixChanged();
        return true;
    }

    /**
     * Refined Storage loads a pattern put in the output slot back into the matrix, from a listener of
     * the patterns handler. Listeners run in order, so this one sees the loaded matrix.
     */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void rspolymorph$listenToPatterns(CallbackInfo ci) {
        patterns.addListener((handler, slot, reading) -> {
            ItemStack pattern = handler.getStackInSlot(slot);
            if (reading || slot != 1 || pattern.isEmpty() || isProcessingPattern()) {
                return;
            }
            ResourceLocation tagged = RecipeAccess.taggedRecipe(pattern);
            if (tagged == null) {
                return;
            }
            // The grid already shows it after a print, which also lands here: nothing to pin then.
            ICraftingRecipe recipe = RecipeAccess.findCrafting(world, tagged, matrix);
            if (recipe != null && recipe != currentRecipe) {
                rspolymorph$select(tagged);
            }
        });
    }

    @Inject(method = "onCraftingMatrixChanged", at = @At("HEAD"))
    private void rspolymorph$applySelection(CallbackInfo ci) {
        if (rspolymorph$selected != null) {
            ICraftingRecipe selected = RecipeAccess.findCrafting(world, rspolymorph$selected, matrix);
            if (selected != null) {
                currentRecipe = selected;
            }
        }
    }

    /** At HEAD: reading the matrix fires onCraftingMatrixChanged, which needs the selection. */
    @Inject(method = "read", at = @At("HEAD"))
    private void rspolymorph$readSelection(CompoundNBT tag, CallbackInfo ci) {
        rspolymorph$selected = tag.contains(RsPolymorph.TAG_RECIPE, Constants.NBT.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(RsPolymorph.TAG_RECIPE))
                : null;
    }

    @Inject(method = "write", at = @At("HEAD"))
    private void rspolymorph$writeSelection(CompoundNBT tag, CallbackInfoReturnable<CompoundNBT> cir) {
        if (rspolymorph$selected != null) {
            tag.putString(RsPolymorph.TAG_RECIPE, rspolymorph$selected.toString());
        }
    }
}
