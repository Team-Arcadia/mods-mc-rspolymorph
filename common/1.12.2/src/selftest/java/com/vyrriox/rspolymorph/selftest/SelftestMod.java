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

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Two recipes with the same ingredients, a conflict vanilla does not have, and the start of the
 * client selftest. Exists only in a -Pselftest build.
 *
 * Author: vyrriox
 */
@Mod(modid = SelftestMod.ID, name = "RS Polymorph selftest", version = "1",
        dependencies = "required-after:rspolymorph")
public final class SelftestMod {

    public static final String ID = "rspolymorph_selftest";
    public static final ResourceLocation RECIPE_DIAMOND = new ResourceLocation(ID, "conflict_diamond");
    public static final ResourceLocation RECIPE_EMERALD = new ResourceLocation(ID, "conflict_emerald");
    public static final ResourceLocation RECIPE_REMAINDER_STICK = new ResourceLocation(ID, "remainder_stick");
    public static final ResourceLocation RECIPE_REMAINDER_BONE = new ResourceLocation(ID, "remainder_bone");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(conflict(RECIPE_DIAMOND, new ItemStack(Items.DIAMOND)));
        event.getRegistry().register(conflict(RECIPE_EMERALD, new ItemStack(Items.EMERALD)));
        event.getRegistry().register(withRemainder(RECIPE_REMAINDER_STICK, new ItemStack(Items.COAL), Items.STICK));
        event.getRegistry().register(withRemainder(RECIPE_REMAINDER_BONE, new ItemStack(Items.REDSTONE), Items.BONE));
    }

    private static IRecipe conflict(ResourceLocation id, ItemStack output) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(Ingredient.fromStacks(new ItemStack(Blocks.GRAVEL)));
        inputs.add(Ingredient.fromStacks(new ItemStack(Items.FLINT)));
        return new ShapelessRecipes("", output, inputs).setRegistryName(id);
    }

    /** Same ingredients for both, and a remainder that tells which recipe was crafted. */
    private static IRecipe withRemainder(ResourceLocation id, ItemStack output, Item remainder) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.add(Ingredient.fromStacks(new ItemStack(Items.CLAY_BALL)));
        inputs.add(Ingredient.fromStacks(new ItemStack(Blocks.SAND)));
        return new ShapelessRecipes("", output, inputs) {
            @Override
            public NonNullList<ItemStack> getRemainingItems(InventoryCrafting matrix) {
                NonNullList<ItemStack> left = NonNullList.withSize(matrix.getSizeInventory(), ItemStack.EMPTY);
                left.set(0, new ItemStack(remainder));
                return left;
            }
        }.setRegistryName(id);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) throws Exception {
        if (FMLCommonHandler.instance().getSide().isClient() && Boolean.getBoolean("rspolymorph.selftest")) {
            // By name: this class also exists on a server, where the driver's client imports would not load.
            Class.forName("com.vyrriox.rspolymorph.selftest.Selftest").getMethod("start").invoke(null);
        }
    }
}
