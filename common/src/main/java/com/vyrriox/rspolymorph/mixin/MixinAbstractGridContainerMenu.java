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

import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deterministic cleanup hook for BlockEntity-free crafting grids (e.g. the Quartz Arsenal
 * wireless crafting grid).
 *
 * A wireless grid's RecipeMatrix is created when the grid is opened and registered in
 * CONTAINER_TO_MATRIX by MixinRecipeMatrix. Because that map strong-references the matrix and
 * nothing else removes the entry, the per-matrix selection store (a WeakHashMap) could never
 * collect it — the matrix would stay reachable forever, leaking one matrix + container per open.
 * On menu close we remove both registrations so the wireless matrix becomes GC-able.
 *
 * Targets the RS2 base class shared by crafting, pattern AND wireless menus. The instanceof
 * BlockEntity guard makes the wired CraftingGrid a no-op (its mapping is owned by the BlockEntity
 * lifecycle, not the menu), and the PatternGrid is skipped because its menu does not hold a
 * CraftingGrid (the accessor instanceof check fails).
 *
 * Common-side and server-safe: references only common RS2 / Minecraft types, never client types.
 *
 * Author: vyrriox
 */
@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public abstract class MixinAbstractGridContainerMenu {

    @Inject(method = "removed(Lnet/minecraft/world/entity/player/Player;)V", at = @At("RETURN"), remap = false)
    private void RSPOLYMORPH_onRemoved(Player player, CallbackInfo ci) {
        // Server-side only. In singleplayer removed() fires on BOTH logical sides against the same
        // shared static maps, but client and server each construct their own RecipeMatrix instances.
        // The server-side instance is the one whose registration must be reclaimed; gating here
        // prevents the client-side call from removing the wrong (client) entry and leaving the
        // server entry pinned forever.
        if (!(player instanceof ServerPlayer)) return;

        // Only crafting-grid menus hold a CraftingGrid; the pattern grid menu does not.
        if (!(((Object) this) instanceof AccessorAbstractCraftingGridContainerMenu acc)) return;

        CraftingGrid grid = acc.rspolymorph$getCraftingGrid();
        if (grid == null) return;

        // BE-backed grids own their mapping via the BlockEntity lifecycle — never drop it here.
        if (grid instanceof BlockEntity) return;

        RecipeMatrixContainer rmc = grid.getCraftingMatrix();
        if (rmc == null) return;

        RsPolymorph.unregisterMatrix(rmc);
    }
}
