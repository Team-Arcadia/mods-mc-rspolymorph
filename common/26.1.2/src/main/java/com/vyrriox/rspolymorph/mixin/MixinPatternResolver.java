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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.refinedmods.refinedstorage.common.autocrafting.PatternResolver;
import com.refinedmods.refinedstorage.common.autocrafting.PatternState;
import com.refinedmods.refinedstorage.common.support.RecipeProvider;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * RS 3.2.0 variant of MixinPatternResolver.
 *
 * In RS 3.x the private {@code getCraftingPattern} overload no longer calls
 * {@code RecipeManager.getRecipeFor}; it calls
 * {@code Platform.INSTANCE.getClientRecipeProvider(level).getRecipesFor(...)} which returns a
 * {@code Stream<RecipeHolder<T>>}, then {@code .map(...).flatMap(...).findFirst()}. So instead of a
 * {@code @Redirect} on {@code RecipeManager}, we {@code @WrapOperation} the
 * {@link RecipeProvider#getRecipesFor} call and, when a Polymorph-selected recipe id is active,
 * reorder the stream so the chosen recipe is first.
 *
 * The HEAD/RETURN ThreadLocal capture pair is identical to the 1.21.1 version.
 *
 * Author: vyrriox
 */
@Mixin(value = PatternResolver.class, remap = false)
public class MixinPatternResolver {

    private static final ThreadLocal<Identifier> ACTIVE_RECIPE_OVERRIDE = new ThreadLocal<>();

    @Inject(
        method = "getCraftingPattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;)Ljava/util/Optional;",
        at = @At("HEAD"),
        remap = false
    )
    private void RSPOLYMORPH_captureOverride(
            ItemStack stack, Level level, PatternState patternState,
            CallbackInfoReturnable<Optional<?>> cir) {
        Identifier override = stack.get(RsPolymorph.selectedRecipeComponent());
        ACTIVE_RECIPE_OVERRIDE.set(override);
    }

    @Inject(
        method = "getCraftingPattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;)Ljava/util/Optional;",
        at = @At("RETURN"),
        remap = false
    )
    private void RSPOLYMORPH_clearOverride(
            ItemStack stack, Level level, PatternState patternState,
            CallbackInfoReturnable<Optional<?>> cir) {
        ACTIVE_RECIPE_OVERRIDE.remove();
    }

    /**
     * Wraps {@code RecipeProvider.getRecipesFor} inside the private overload. When an override id is
     * active, moves the matching recipe to the front of the stream so the downstream
     * {@code .map(...).flatMap(...).findFirst()} resolves the chosen recipe.
     */
    @WrapOperation(
        method = "getCraftingPattern(Lnet/minecraft/world/level/Level;Lcom/refinedmods/refinedstorage/common/autocrafting/PatternState;Lcom/refinedmods/refinedstorage/common/autocrafting/CraftingPatternState;)Ljava/util/Optional;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/refinedmods/refinedstorage/common/support/RecipeProvider;getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/stream/Stream;"
        ),
        remap = false
    )
    private <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> RSPOLYMORPH_wrapGetRecipesFor(
            RecipeProvider provider, RecipeType<T> type, I container, Level level,
            Operation<Stream<RecipeHolder<T>>> original) {
        Stream<RecipeHolder<T>> result = original.call(provider, type, container, level);
        Identifier override = ACTIVE_RECIPE_OVERRIDE.get();
        if (override == null) {
            return result;
        }
        // Reorder so the override recipe (if present) is first; preserve the rest as-is.
        // MC 26.x: RecipeHolder.id() is a ResourceKey<Recipe<?>>, so unwrap to Identifier to
        // compare against the stored override id (mirrors MixinRecipeMatrix / RsGridRecipeWidget).
        java.util.List<RecipeHolder<T>> list = result.toList();
        return Stream.concat(
                list.stream().filter(h -> h.id().identifier().equals(override)),
                list.stream().filter(h -> !h.id().identifier().equals(override))
        );
    }
}
