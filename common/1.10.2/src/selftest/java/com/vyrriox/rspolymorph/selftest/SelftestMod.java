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
package com.vyrriox.rspolymorph.selftest;

import com.vyrriox.rspolymorph.RecipeAccess;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Two recipes with the same ingredients, a conflict vanilla does not have, and the start of the
 * client selftest. Exists only in a -Pselftest build. Recipes have no registered id on this version:
 * these are the ids the mod makes for them.
 *
 * Author: vyrriox
 */
@Mod(modid = SelftestMod.ID, name = "RS Polymorph selftest", version = "1",
        dependencies = "required-after:rspolymorph")
public final class SelftestMod {

    public static final String ID = "rspolymorph_selftest";
    public static ResourceLocation RECIPE_DIAMOND;
    public static ResourceLocation RECIPE_EMERALD;
    public static ResourceLocation RECIPE_REMAINDER_STICK;
    public static ResourceLocation RECIPE_REMAINDER_BONE;
    /** A real recipe that never matches the conflict. */
    public static ResourceLocation RECIPE_STICK;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        RECIPE_DIAMOND = conflict(new ItemStack(Items.DIAMOND));
        RECIPE_EMERALD = conflict(new ItemStack(Items.EMERALD));
        RECIPE_REMAINDER_STICK = withRemainder(new ItemStack(Items.COAL), Items.STICK);
        RECIPE_REMAINDER_BONE = withRemainder(new ItemStack(Items.REDSTONE), Items.BONE);
    }

    private static ResourceLocation conflict(ItemStack output) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Blocks.GRAVEL));
        inputs.add(new ItemStack(Items.FLINT));
        IRecipe recipe = new ShapelessRecipes(output, inputs);
        GameRegistry.addRecipe(recipe);
        return RecipeAccess.idOf(recipe);
    }

    /** Same ingredients for both, and a remainder that tells which recipe was crafted. */
    private static ResourceLocation withRemainder(ItemStack output, Item remainder) {
        List<ItemStack> inputs = new ArrayList<>();
        inputs.add(new ItemStack(Items.CLAY_BALL));
        inputs.add(new ItemStack(Blocks.SAND));
        IRecipe recipe = new ShapelessRecipes(output, inputs) {
            @Override
            public ItemStack[] getRemainingItems(InventoryCrafting matrix) {
                ItemStack[] left = new ItemStack[matrix.getSizeInventory()];
                left[0] = new ItemStack(remainder);
                return left;
            }
        };
        GameRegistry.addRecipe(recipe);
        return RecipeAccess.idOf(recipe);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) throws Exception {
        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
            ItemStack output = recipe.getRecipeOutput();
            if (RECIPE_STICK == null && output != null && output.getItem() == Items.STICK) {
                RECIPE_STICK = RecipeAccess.idOf(recipe);
            }
        }
        if (FMLCommonHandler.instance().getSide().isClient() && Boolean.getBoolean("rspolymorph.selftest")) {
            // By name: this class also exists on a server, where the driver's client imports would not load.
            Class.forName("com.vyrriox.rspolymorph.selftest.Selftest").getMethod("start").invoke(null);
        }
    }
}
