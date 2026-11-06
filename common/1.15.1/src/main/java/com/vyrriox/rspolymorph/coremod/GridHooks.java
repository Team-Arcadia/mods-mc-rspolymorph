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
package com.vyrriox.rspolymorph.coremod;

import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.raoulvdberge.refinedstorage.inventory.item.BaseItemHandler;
import com.raoulvdberge.refinedstorage.item.PatternItem;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * What the coremod's hooks call, one static method per hook. The selection lives on the node: Refined
 * Storage keeps {@code currentRecipe} as long as it matches the matrix, so setting it before
 * {@code onCraftingMatrixChanged} is enough: the result slot, normal crafts and shift crafts all follow it. A
 * selection that no longer matches is kept but ignored, and applies again when its ingredients come back.
 * Must never reference a client class.
 *
 * Author: vyrriox
 */
public final class GridHooks {

    private GridHooks() {
    }

    public static boolean select(GridNetworkNode node, ResourceLocation recipeId) {
        if (RecipeAccess.findCrafting(node.getWorld(), recipeId, node.getCraftingMatrix()) == null) {
            return false;
        }
        ((IRsPolymorphGrid) node).rspolymorph$setSelected(recipeId);
        node.onCraftingMatrixChanged();
        return true;
    }

    /**
     * Refined Storage loads a pattern put in the output slot back into the matrix, from a listener of
     * the patterns handler. Listeners run in order, so this one sees the loaded matrix.
     */
    public static void attachListener(GridNetworkNode node) {
        ((BaseItemHandler) node.getPatterns()).addListener((handler, slot, reading) -> {
            ItemStack pattern = handler.getStackInSlot(slot);
            if (reading || slot != 1 || pattern.isEmpty() || node.isProcessingPattern()) {
                return;
            }
            ResourceLocation tagged = RecipeAccess.taggedRecipe(pattern);
            if (tagged == null) {
                return;
            }
            // The grid already shows it after a print, which also lands here: nothing to pin then.
            IRsPolymorphGrid grid = (IRsPolymorphGrid) node;
            ICraftingRecipe recipe = RecipeAccess.findCrafting(node.getWorld(), tagged, node.getCraftingMatrix());
            if (recipe != null && recipe != grid.rspolymorph$getCurrentRecipe()) {
                grid.rspolymorph$select(tagged);
            }
        });
    }

    /** @return the recipe the grid must show: the selection when it matches, else what it had */
    @Nullable
    public static ICraftingRecipe applySelection(GridNetworkNode node, @Nullable ICraftingRecipe current) {
        ResourceLocation selected = ((IRsPolymorphGrid) node).rspolymorph$getSelected();
        if (selected != null) {
            ICraftingRecipe recipe = RecipeAccess.findCrafting(node.getWorld(), selected, node.getCraftingMatrix());
            if (recipe != null) {
                return recipe;
            }
        }
        return current;
    }

    public static void readSelection(GridNetworkNode node, CompoundNBT tag) {
        ((IRsPolymorphGrid) node).rspolymorph$setSelected(tag.contains(RsPolymorph.TAG_RECIPE, Constants.NBT.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(RsPolymorph.TAG_RECIPE))
                : null);
    }

    public static void writeSelection(GridNetworkNode node, CompoundNBT tag) {
        ResourceLocation selected = ((IRsPolymorphGrid) node).rspolymorph$getSelected();
        if (selected != null) {
            tag.putString(RsPolymorph.TAG_RECIPE, selected.toString());
        }
    }

    /** Tags a printed pattern with the recipe the grid shows, as it enters the output slot. */
    public static ItemStack tagPattern(ItemStack pattern, GridNetworkNode node, @Nullable ICraftingRecipe current) {
        if (current != null && !node.isProcessingPattern()) {
            RecipeAccess.tagRecipe(pattern, RecipeAccess.idOf(current));
        }
        return pattern;
    }

    /**
     * Resolves a tagged pattern to the recipe it was printed with. An untagged pattern, or one whose
     * recipe is gone or no longer matches, takes Refined Storage's own lookup.
     */
    @SuppressWarnings("rawtypes")
    public static Optional resolveTagged(Optional found, World level, ItemStack pattern) {
        ResourceLocation tagged = RecipeAccess.taggedRecipe(pattern);
        if (tagged != null) {
            // The local container Refined Storage filled is out of reach: same inputs, same type.
            CraftingInventory inputs = new CraftingPattern.DummyCraftingInventory();
            for (int i = 0; i < inputs.getContainerSize(); i++) {
                inputs.setItem(i, PatternItem.getInputSlot(pattern, i));
            }
            ICraftingRecipe recipe = RecipeAccess.findCrafting(level, tagged, inputs);
            if (recipe != null) {
                return Optional.of(recipe);
            }
        }
        return found;
    }
}
