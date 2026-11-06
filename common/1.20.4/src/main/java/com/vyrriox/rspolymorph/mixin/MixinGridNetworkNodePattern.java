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
import com.vyrriox.rspolymorph.RecipeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Tags a printed pattern with the recipe the grid shows, whoever chose it: this mod, Polymorph or
 * Refined Storage itself. Always applied, it touches nothing Polymorph touches. The stack is tagged
 * as it enters the slot rather than at the method's return, which also runs when nothing was printed
 * and would retag a pattern left in the slot. Finding the id of the recipe scans the recipes, once
 * per printed pattern.
 *
 * Author: vyrriox
 */
@Mixin(value = GridNetworkNode.class, remap = false)
public abstract class MixinGridNetworkNodePattern extends NetworkNode {

    @Shadow
    private CraftingRecipe currentRecipe;

    @Shadow
    @Final
    private CraftingContainer matrix;

    private MixinGridNetworkNodePattern(Level level, BlockPos pos) {
        super(level, pos);
    }

    @Shadow
    public abstract boolean isProcessingPattern();

    @ModifyArg(
            method = "onCreatePattern",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/inventory/item/BaseItemHandler;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"
            ),
            index = 1
    )
    private ItemStack rspolymorph$tagPattern(ItemStack pattern) {
        if (currentRecipe != null && !isProcessingPattern()) {
            ResourceLocation id = RecipeAccess.idOf(currentRecipe, matrix, level);
            if (id != null) {
                RecipeAccess.tagRecipe(pattern, id);
            }
        }
        return pattern;
    }
}
