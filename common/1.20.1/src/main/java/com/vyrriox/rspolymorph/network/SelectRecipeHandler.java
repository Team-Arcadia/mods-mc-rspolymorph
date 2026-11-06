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

import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.refinedmods.refinedstorage.container.GridContainerMenu;
import com.vyrriox.rspolymorph.GridOwnership;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

/**
 * Server side of the selection packet. Trusts nothing from the client: anything that is not the
 * player's open crafting or pattern grid, or a recipe that does not match it, is dropped quietly.
 * The loader project calls this on the server thread.
 *
 * Author: vyrriox
 */
public final class SelectRecipeHandler {

    private SelectRecipeHandler() {
    }

    public static void handle(ServerPlayer player, int containerId, @Nullable ResourceLocation recipeId) {
        AbstractContainerMenu open = player.containerMenu;
        if (GridOwnership.polymorphOwnsGrids()
                || recipeId == null
                || open.containerId != containerId
                || !(open instanceof GridContainerMenu menu)
                || !(menu.getGrid() instanceof GridNetworkNode node)) {
            return;
        }
        boolean crafting = node.getGridType() == GridType.CRAFTING;
        boolean pattern = node.getGridType() == GridType.PATTERN && !node.isProcessingPattern();
        if ((crafting || pattern) && ((IRsPolymorphGrid) node).rspolymorph$select(recipeId)) {
            // The result slot reaches the client now, not on the next change of the matrix.
            open.broadcastChanges();
        }
    }
}
