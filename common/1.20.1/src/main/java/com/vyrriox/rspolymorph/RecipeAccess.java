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

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

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
    public static CraftingRecipe findCrafting(Level level, ResourceLocation id, CraftingContainer matrix) {
        Optional<? extends Recipe<?>> found = level.getRecipeManager().byKey(id);
        if (found.isPresent()
                && found.get().getType() == RecipeType.CRAFTING
                && found.get() instanceof CraftingRecipe recipe
                && recipe.matches(matrix, level)) {
            return recipe;
        }
        return null;
    }

    /** Scans every crafting recipe: call it only when the content of the matrix changed. */
    public static List<CraftingRecipe> candidates(Level level, CraftingContainer matrix) {
        return level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, matrix, level);
    }

    public static ResourceLocation idOf(CraftingRecipe recipe) {
        return recipe.getId();
    }

    public static ItemStack assemble(CraftingRecipe recipe, CraftingContainer matrix, Level level) {
        return recipe.assemble(matrix, level.registryAccess());
    }

    public static boolean sameStack(ItemStack a, ItemStack b) {
        return ItemStack.isSameItemSameTags(a, b);
    }

    /** Hash of the items of a container and their NBT, counts left out: recipes ignore them. */
    public static long contentHash(Container container) {
        long hash = 1L;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            hash = 1000003L * hash + (stack.isEmpty() ? 0 : System.identityHashCode(stack.getItem()));
            CompoundTag tag = stack.getTag();
            hash = 1000003L * hash + (tag == null ? 0 : tag.hashCode());
        }
        return hash;
    }

    /** @return the recipe id a printed pattern is tagged with, or null if it has no valid one */
    @Nullable
    public static ResourceLocation taggedRecipe(ItemStack pattern) {
        CompoundTag tag = pattern.getTag();
        if (tag == null || !tag.contains(RsPolymorph.TAG_RECIPE)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(RsPolymorph.TAG_RECIPE));
    }

    public static void tagRecipe(ItemStack pattern, ResourceLocation id) {
        pattern.getOrCreateTag().putString(RsPolymorph.TAG_RECIPE, id.toString());
    }
}
