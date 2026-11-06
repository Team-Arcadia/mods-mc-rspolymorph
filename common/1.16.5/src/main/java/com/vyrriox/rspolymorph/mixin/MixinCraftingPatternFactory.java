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

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingPattern;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingPatternFactory;
import com.refinedmods.refinedstorage.item.PatternItem;
import com.vyrriox.rspolymorph.RecipeAccess;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

/**
 * Resolves a tagged pattern to the recipe it was printed with. An untagged pattern, or one whose
 * recipe is gone or no longer matches, takes Refined Storage's own lookup.
 *
 * Author: vyrriox
 */
@Mixin(value = CraftingPatternFactory.class, remap = false)
public abstract class MixinCraftingPatternFactory {

    /**
     * Swaps the result of the lookup, stored in the method's only Optional local, rather than the call
     * itself, so nothing else that hooks this call can clash with it.
     */
    @ModifyVariable(method = "create", at = @At("STORE"), ordinal = 0)
    @SuppressWarnings("rawtypes")
    private Optional rspolymorph$resolveTagged(Optional found, World level, ICraftingPatternContainer context, ItemStack pattern) {
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
