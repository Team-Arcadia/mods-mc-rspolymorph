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

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * What the bytecode added by {@code GridTransformer} calls. Server logic only, the same on both
 * sides, with no client class in reach.
 *
 * Refined Storage has no {@code currentRecipe} before 1.12: the grid asks vanilla for an output on
 * each change of the matrix. That one call goes through {@link #result} instead, which decides on a
 * recipe and keeps it in a field generated on the grid. A selection that no longer matches is kept
 * but ignored, and applies again when its ingredients come back.
 *
 * Author: vyrriox
 */
public final class GridHooks {

    private GridHooks() {
    }

    /**
     * In place of {@code CraftingManager.findMatchingRecipe} in {@code onCraftingMatrixChanged}. Costs
     * what the vanilla call costs: one scan per change of the matrix, none with a valid selection.
     */
    public static ItemStack result(CraftingManager manager, InventoryCrafting matrix, World world, IRsPolymorphGrid grid) {
        ResourceLocation selected = grid.rspolymorph$getSelected();
        IRecipe recipe = selected == null ? null : RecipeAccess.findCrafting(world, selected, matrix);
        if (recipe == null) {
            recipe = RecipeAccess.firstMatch(world, matrix);
        }
        grid.rspolymorph$setCurrentRecipe(recipe);
        return recipe == null ? ItemStack.EMPTY : recipe.getCraftingResult(matrix);
    }

    /** At the head of the grid's {@code read}: reading the matrix fires the method above. */
    public static void readSelection(IRsPolymorphGrid grid, NBTTagCompound tag) {
        grid.rspolymorph$setSelected(tag.hasKey(RsPolymorph.TAG_RECIPE, Constants.NBT.TAG_STRING)
                ? new ResourceLocation(tag.getString(RsPolymorph.TAG_RECIPE))
                : null);
    }

    /** At the head of the grid's {@code write}. */
    public static void writeSelection(IRsPolymorphGrid grid, NBTTagCompound tag) {
        ResourceLocation selected = grid.rspolymorph$getSelected();
        if (selected != null) {
            tag.setString(RsPolymorph.TAG_RECIPE, selected.toString());
        }
    }

    /**
     * On the stack that the grid's {@code onCreatePattern} puts in the output slot. The tag is the
     * recipe the grid shows, whoever chose it: the stored selection may be for other inputs. This
     * grid only prints crafting patterns, processing has a block of its own here.
     */
    public static ItemStack tagPattern(ItemStack pattern, IRsPolymorphGrid grid) {
        IRecipe current = grid.rspolymorph$getCurrentRecipe();
        if (current != null) {
            RecipeAccess.tagRecipe(pattern, RecipeAccess.idOf(current));
        }
        return pattern;
    }

    /**
     * On the recipe list of the {@code CraftingPattern} constructor, whose stream takes the first
     * recipe that matches. The tagged recipes come first; if they are gone or no longer match, the
     * stream simply goes on with Refined Storage's own order.
     */
    public static List<IRecipe> taggedFirst(List<IRecipe> recipes, ItemStack pattern) {
        ResourceLocation tagged = pattern == null ? null : RecipeAccess.taggedRecipe(pattern);
        List<IRecipe> first = tagged == null ? null : RecipeAccess.allWithId(tagged);
        if (first == null || first.isEmpty()) {
            return recipes;
        }
        List<IRecipe> ordered = new ArrayList<>(first.size() + recipes.size());
        ordered.addAll(first);
        ordered.addAll(recipes);
        return ordered;
    }
}
