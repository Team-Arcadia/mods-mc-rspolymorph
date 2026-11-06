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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
     * @return the recipe if it exists and matches the matrix, else null
     */
    @Nullable
    public static IRecipe findCrafting(World world, ResourceLocation id, InventoryCrafting matrix) {
        IRecipe recipe = CraftingManager.REGISTRY.getObject(id);
        return recipe != null && recipe.matches(matrix, world) ? recipe : null;
    }

    /** @return the recipe registered under this id, whatever it matches */
    @Nullable
    public static IRecipe byId(ResourceLocation id) {
        return CraftingManager.REGISTRY.getObject(id);
    }

    /** Scans every crafting recipe: call it only when the content of the matrix changed. */
    public static List<IRecipe> candidates(World world, InventoryCrafting matrix) {
        List<IRecipe> found = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.REGISTRY) {
            if (recipe.matches(matrix, world)) {
                found.add(recipe);
            }
        }
        return found;
    }

    @Nullable
    public static ResourceLocation idOf(IRecipe recipe) {
        return CraftingManager.REGISTRY.getNameForObject(recipe);
    }

    public static ItemStack assemble(IRecipe recipe, InventoryCrafting matrix) {
        return recipe.getCraftingResult(matrix);
    }

    public static boolean sameStack(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** Hash of the items of a matrix, their damage and NBT, counts left out: recipes ignore them. */
    public static long contentHash(InventoryCrafting matrix) {
        long hash = 1L;
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            ItemStack stack = matrix.getStackInSlot(i);
            hash = 1000003L * hash + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            hash = 1000003L * hash + (stack.isEmpty() ? 0 : stack.getItemDamage());
            NBTTagCompound tag = stack.getTagCompound();
            hash = 1000003L * hash + (tag == null ? 0 : tag.hashCode());
        }
        return hash;
    }

    /** @return the recipe id a printed pattern is tagged with, or null if it has none */
    @Nullable
    public static ResourceLocation taggedRecipe(ItemStack pattern) {
        NBTTagCompound tag = pattern.getTagCompound();
        if (tag == null || !tag.hasKey(RsPolymorph.TAG_RECIPE, Constants.NBT.TAG_STRING)) {
            return null;
        }
        return new ResourceLocation(tag.getString(RsPolymorph.TAG_RECIPE));
    }

    public static void tagRecipe(ItemStack pattern, ResourceLocation id) {
        NBTTagCompound tag = pattern.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            pattern.setTagCompound(tag);
        }
        tag.setString(RsPolymorph.TAG_RECIPE, id.toString());
    }
}
