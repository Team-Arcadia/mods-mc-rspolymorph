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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every use of the vanilla recipe and stack API that changes between Minecraft versions.
 *
 * Before 1.12 recipes sit in a plain list and have no id, so this fork makes one. The id is a hash of
 * the recipe's class name and of its output (item name, damage, count and NBT): it survives a restart
 * and a change in the order of the list, which printed patterns and saved grids need. Two recipes can
 * share an id only with the same class and the same output. Each lookup therefore checks the match
 * and goes through every recipe of the id, and the popup shows such recipes once, since choosing
 * between them would change nothing the player can see.
 *
 * Stacks are nullable on this line: an empty slot holds null, and a stack has no isEmpty.
 *
 * Author: vyrriox
 */
public final class RecipeAccess {

    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    /** Read on the client and the server thread: always replaced whole, never changed. */
    private static volatile Index index = new Index(Collections.<IRecipe>emptyList());

    private RecipeAccess() {
    }

    private static final class Index {
        final int size;
        final Map<ResourceLocation, List<IRecipe>> byId;

        Index(List<IRecipe> recipes) {
            size = recipes.size();
            byId = new HashMap<>(recipes.size() * 2);
            for (IRecipe recipe : recipes) {
                ResourceLocation id = idOf(recipe);
                List<IRecipe> same = byId.get(id);
                if (same == null) {
                    same = new ArrayList<>(1);
                    byId.put(id, same);
                }
                same.add(recipe);
            }
        }
    }

    private static List<IRecipe> recipes() {
        return CraftingManager.getInstance().getRecipeList();
    }

    /** Built on first use and again if mods added recipes since: one size check per lookup. */
    private static List<IRecipe> withId(ResourceLocation id) {
        List<IRecipe> recipes = recipes();
        Index current = index;
        if (current.size != recipes.size()) {
            current = new Index(recipes);
            index = current;
        }
        List<IRecipe> found = current.byId.get(id);
        return found == null ? Collections.<IRecipe>emptyList() : found;
    }

    /**
     * One map lookup and a match check of the recipes of that id, never a scan of the recipes.
     *
     * @return the recipe if it exists and matches the matrix, else null
     */
    @Nullable
    public static IRecipe findCrafting(World world, ResourceLocation id, InventoryCrafting matrix) {
        for (IRecipe recipe : withId(id)) {
            if (recipe.matches(matrix, world)) {
                return recipe;
            }
        }
        return null;
    }

    /** @return the first recipe with this id, whatever it matches */
    @Nullable
    public static IRecipe byId(ResourceLocation id) {
        List<IRecipe> found = withId(id);
        return found.isEmpty() ? null : found.get(0);
    }

    /** @return every recipe with this id, whatever they match */
    public static List<IRecipe> allWithId(ResourceLocation id) {
        return withId(id);
    }

    /** Scans every crafting recipe: call it only when the content of the matrix changed. */
    public static List<IRecipe> candidates(World world, InventoryCrafting matrix) {
        List<IRecipe> found = new ArrayList<>();
        List<ResourceLocation> ids = new ArrayList<>();
        for (IRecipe recipe : recipes()) {
            if (recipe.matches(matrix, world)) {
                ResourceLocation id = idOf(recipe);
                if (!ids.contains(id)) {
                    ids.add(id);
                    found.add(recipe);
                }
            }
        }
        return found;
    }

    /** What the vanilla lookup finds, as a recipe: it only gives the output. Scans, like vanilla. */
    @Nullable
    public static IRecipe firstMatch(World world, InventoryCrafting matrix) {
        for (IRecipe recipe : recipes()) {
            if (recipe.matches(matrix, world)) {
                return recipe;
            }
        }
        return null;
    }

    public static ResourceLocation idOf(IRecipe recipe) {
        long hash = FNV_OFFSET;
        hash = mix(hash, recipe.getClass().getName());
        ItemStack output = recipe.getRecipeOutput();
        if (output != null) {
            hash = mix(hash, String.valueOf(output.getItem().getRegistryName()));
            hash = mix(hash, output.getItemDamage() + "x" + output.stackSize);
            NBTTagCompound tag = output.getTagCompound();
            if (tag != null) {
                hash = mix(hash, tag.toString());
            }
        }
        return new ResourceLocation(RsPolymorph.MOD_ID, "recipe_" + Long.toHexString(hash));
    }

    /** FNV-1a, whose result does not depend on the run or the JVM. */
    private static long mix(long hash, String text) {
        long result = hash;
        for (int i = 0; i < text.length(); i++) {
            result = (result ^ text.charAt(i)) * FNV_PRIME;
        }
        return (result ^ '|') * FNV_PRIME;
    }

    public static ItemStack assemble(IRecipe recipe, InventoryCrafting matrix) {
        return recipe.getCraftingResult(matrix);
    }

    public static boolean sameStack(ItemStack a, ItemStack b) {
        return ItemStack.areItemsEqual(a, b) && ItemStack.areItemStackTagsEqual(a, b);
    }

    /** @return true when no slot of the matrix holds anything */
    public static boolean isEmpty(InventoryCrafting matrix) {
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            if (matrix.getStackInSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    /** Hash of the items of a matrix, their damage and NBT, counts left out: recipes ignore them. */
    public static long contentHash(InventoryCrafting matrix) {
        long hash = 1L;
        for (int i = 0; i < matrix.getSizeInventory(); i++) {
            ItemStack stack = matrix.getStackInSlot(i);
            hash = 1000003L * hash + (stack == null ? 0 : System.identityHashCode(stack.getItem()));
            hash = 1000003L * hash + (stack == null ? 0 : stack.getItemDamage());
            NBTTagCompound tag = stack == null ? null : stack.getTagCompound();
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
