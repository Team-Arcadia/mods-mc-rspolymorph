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

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Every use of the vanilla recipe and stack API that changes between Minecraft versions. An older
 * Minecraft forks this class, the callers stay the same.
 *
 * Author: vyrriox
 */
public final class RecipeAccess {

    private RecipeAccess() {
    }

    /**
     * One map lookup and one match check, never a scan of the recipes.
     *
     * @return the recipe if it exists, is a crafting recipe and matches the matrix, else null
     */
    @Nullable
    public static ICraftingRecipe findCrafting(World level, ResourceLocation id, CraftingInventory matrix) {
        Optional<? extends IRecipe<?>> found = level.getRecipeManager().byKey(id);
        if (found.isPresent()
                && found.get().getType() == IRecipeType.CRAFTING
                && found.get() instanceof ICraftingRecipe) {
            ICraftingRecipe recipe = (ICraftingRecipe) found.get();
            if (recipe.matches(matrix, level)) {
                return recipe;
            }
        }
        return null;
    }

    /** Scans every crafting recipe: call it only when the content of the matrix changed. */
    public static List<ICraftingRecipe> candidates(World level, CraftingInventory matrix) {
        return level.getRecipeManager().getRecipesFor(IRecipeType.CRAFTING, matrix, level);
    }

    public static ResourceLocation idOf(ICraftingRecipe recipe) {
        return recipe.getId();
    }

    public static ItemStack assemble(ICraftingRecipe recipe, CraftingInventory matrix, World level) {
        return recipe.assemble(matrix);
    }

    public static boolean sameStack(ItemStack a, ItemStack b) {
        return ItemStack.isSame(a, b) && ItemStack.tagMatches(a, b);
    }

    /** Hash of the items of a container and their NBT, counts left out: recipes ignore them. */
    public static long contentHash(IInventory container) {
        long hash = 1L;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            hash = 1000003L * hash + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            CompoundNBT tag = stack.getTag();
            hash = 1000003L * hash + (tag == null ? 0 : tag.hashCode());
        }
        return hash;
    }

    /** @return the recipe id a printed pattern is tagged with, or null if it has no valid one */
    @Nullable
    public static ResourceLocation taggedRecipe(ItemStack pattern) {
        CompoundNBT tag = pattern.getTag();
        if (tag == null || !tag.contains(RsPolymorph.TAG_RECIPE)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(RsPolymorph.TAG_RECIPE));
    }

    public static void tagRecipe(ItemStack pattern, ResourceLocation id) {
        pattern.getOrCreateTag().putString(RsPolymorph.TAG_RECIPE, id.toString());
    }
}
