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

import com.refinedmods.refinedstorage.common.autocrafting.PatternResolver;
import com.refinedmods.refinedstorage.common.autocrafting.PatternState;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Forces PatternResolver to use the Polymorph-selected recipe when resolving a crafting pattern
 * that was created with a specific recipe choice stored in a custom data component.
 *
 * Flow:
 *  1. HEAD injection on the public getCraftingPattern(ItemStack, ...) reads the stored recipe ID
 *     into a ThreadLocal.
 *  2. @Redirect on the getRecipeFor call inside the private overload prefers that recipe.
 *  3. RETURN injection cleans up the ThreadLocal.
 *
 * Author: vyrriox
 */
@Mixin(value = PatternResolver.class, remap = false)
public class MixinPatternResolver {

    /** Per-thread override: set while resolving a pattern that has a Polymorph selection. */
    private static final ThreadLocal<ResourceLocation> ACTIVE_RECIPE_OVERRIDE = new ThreadLocal<>();

    @Inject(
        method = "getCraftingPattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;)Ljava/util/Optional;",
        at = @At("HEAD"),
        remap = false
    )
    private void RSPOLYMORPH_captureOverride(
            ItemStack stack,
            Level level,
            PatternState patternState,
            CallbackInfoReturnable<Optional<?>> cir) {
        DataComponentType<ResourceLocation> comp = RsPolymorph.selectedRecipeComponent();
        ACTIVE_RECIPE_OVERRIDE.set(stack.get(comp));
    }

    @Inject(
        method = "getCraftingPattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;)Ljava/util/Optional;",
        at = @At("RETURN"),
        remap = false
    )
    private void RSPOLYMORPH_clearOverride(
            ItemStack stack,
            Level level,
            PatternState patternState,
            CallbackInfoReturnable<Optional<?>> cir) {
        ACTIVE_RECIPE_OVERRIDE.remove();
    }

    /**
     * Redirects the getRecipeFor call inside the private getCraftingPattern overload.
     * When a Polymorph override is active, prefers the specifically-selected recipe
     * over the first match returned by getRecipeFor.
     */
    @Redirect(
        method = "getCraftingPattern(Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;Lcom/refinedmods/refinedstorage/common/autocrafting/CraftingPatternState;)Ljava/util/Optional;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
        ),
        remap = false
    )
    private Optional<RecipeHolder<CraftingRecipe>> RSPOLYMORPH_redirectGetRecipeFor(
            RecipeManager manager,
            RecipeType<CraftingRecipe> type,
            RecipeInput input,
            Level level) {
        // PatternResolver always passes CraftingInput here — cast is safe
        CraftingInput craftingInput = (CraftingInput) input;
        ResourceLocation override = ACTIVE_RECIPE_OVERRIDE.get();
        if (override != null) {
            Optional<RecipeHolder<CraftingRecipe>> found =
                    manager.getRecipesFor(type, craftingInput, level).stream()
                            .filter(h -> h.id().equals(override))
                            .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return manager.getRecipeFor(type, craftingInput, level);
    }
}
