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

import com.google.common.collect.Iterators;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * What the bytecode added by {@code GridTransformer} calls. Server logic only, the same on both
 * sides, with no client class in reach.
 *
 * Refined Storage keeps {@code currentRecipe} as long as it matches the matrix, so setting it before
 * {@code onCraftingMatrixChanged} is enough: the result slot, normal crafts and shift crafts all
 * follow it. Only the remainders ask vanilla again, see {@link #remainders}. A selection that no longer matches is kept but ignored, and applies again when its
 * ingredients come back.
 *
 * Author: vyrriox
 */
public final class GridHooks {

    private GridHooks() {
    }

    /** At the head of {@code NetworkNodeGrid.onCraftingMatrixChanged}. */
    public static void applySelection(IRsPolymorphGrid grid) {
        ResourceLocation selected = grid.rspolymorph$getSelected();
        if (selected != null) {
            NetworkNodeGrid node = (NetworkNodeGrid) grid;
            IRecipe recipe = RecipeAccess.findCrafting(node.getWorld(), selected, node.getCraftingMatrix());
            if (recipe != null) {
                grid.rspolymorph$setCurrentRecipe(recipe);
            }
        }
    }

    /**
     * In place of {@code CraftingManager.getRemainingItems} in {@code onCrafted}: vanilla would give the
     * remainders of its first match, not those of the recipe that was just crafted. The other grids of
     * Refined Storage share this code and have no selection: they keep the vanilla lookup.
     */
    public static NonNullList<ItemStack> remainders(InventoryCrafting matrix, World world, Object grid) {
        IRecipe current = grid instanceof IRsPolymorphGrid ? ((IRsPolymorphGrid) grid).rspolymorph$getCurrentRecipe() : null;
        return current != null && current.matches(matrix, world)
                ? current.getRemainingItems(matrix)
                : CraftingManager.getRemainingItems(matrix, world);
    }

    /** At the head of {@code NetworkNodeGrid.read}: reading the matrix fires the method above. */
    public static void readSelection(IRsPolymorphGrid grid, NBTTagCompound tag) {
        grid.rspolymorph$setSelected(tag.hasKey(RsPolymorph.TAG_RECIPE, Constants.NBT.TAG_STRING)
                ? new ResourceLocation(tag.getString(RsPolymorph.TAG_RECIPE))
                : null);
    }

    /** At the head of {@code NetworkNodeGrid.write}. */
    public static void writeSelection(IRsPolymorphGrid grid, NBTTagCompound tag) {
        ResourceLocation selected = grid.rspolymorph$getSelected();
        if (selected != null) {
            tag.setString(RsPolymorph.TAG_RECIPE, selected.toString());
        }
    }

    /**
     * On the stack that {@code NetworkNodeGrid.onCreatePattern} puts in the output slot. The tag is
     * the recipe the grid shows, whoever chose it: the stored selection may be for other inputs.
     */
    public static ItemStack tagPattern(ItemStack pattern, IRsPolymorphGrid grid) {
        IRecipe current = grid.rspolymorph$getCurrentRecipe();
        if (current != null && !((NetworkNodeGrid) grid).isProcessingPattern()) {
            ResourceLocation id = RecipeAccess.idOf(current);
            if (id != null) {
                RecipeAccess.tagRecipe(pattern, id);
            }
        }
        return pattern;
    }

    /**
     * On the recipe iterator of the {@code CraftingPattern} constructor, whose loop takes the first
     * recipe that matches. A tagged recipe comes first; if it is gone or no longer matches, the loop
     * simply goes on with Refined Storage's own order.
     */
    public static Iterator<IRecipe> taggedFirst(Iterator<IRecipe> recipes, @Nullable ItemStack pattern) {
        ResourceLocation tagged = pattern == null ? null : RecipeAccess.taggedRecipe(pattern);
        IRecipe recipe = tagged == null ? null : RecipeAccess.byId(tagged);
        return recipe == null ? recipes : Iterators.concat(Iterators.singletonIterator(recipe), recipes);
    }
}
