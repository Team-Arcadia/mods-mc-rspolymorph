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
package com.vyrriox.rspolymorph.network;

import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.api.network.grid.IGrid;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.container.ContainerGrid;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;

/**
 * Server side of the selection packet. Trusts nothing from the client: anything that is not the
 * player's open crafting or pattern grid, or a recipe that does not match it, is dropped quietly.
 * Runs on the server thread.
 *
 * Author: vyrriox
 */
public final class SelectRecipeHandler {

    private SelectRecipeHandler() {
    }

    public static void handle(EntityPlayerMP player, int windowId, ResourceLocation recipeId) {
        Container open = player.openContainer;
        if (open == null || open.windowId != windowId || !(open instanceof ContainerGrid)) {
            return;
        }
        IGrid grid = ((ContainerGrid) open).getGrid();
        if (!(grid instanceof NetworkNodeGrid)) {
            return;
        }
        NetworkNodeGrid node = (NetworkNodeGrid) grid;
        boolean crafting = node.getGridType() == GridType.CRAFTING;
        boolean pattern = node.getGridType() == GridType.PATTERN && !node.isProcessingPattern();
        if ((crafting || pattern)
                && RecipeAccess.findCrafting(node.getWorld(), recipeId, node.getCraftingMatrix()) != null) {
            ((IRsPolymorphGrid) node).rspolymorph$setSelected(recipeId);
            node.onCraftingMatrixChanged();
            // The result slot reaches the client now, not on the next tick.
            open.detectAndSendChanges();
            SelectionSync.send(player, open.windowId, node);
        }
    }
}
