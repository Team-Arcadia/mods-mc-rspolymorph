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

import com.vyrriox.rspolymorph.IRsRecipeMatrix;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client to server payload carrying a recipe selection for the menu the client has open.
 *
 * This class is loader-agnostic: it defines the payload {@link Type}, its {@link #STREAM_CODEC},
 * and the pure server-side application logic in {@link #applyOnServer}. Each loader subproject
 * registers the payload and its receiver, then calls {@code applyOnServer} on the server thread.
 *
 * In singleplayer the selection still travels through this payload over the local loopback, so
 * both SP and MP take the same server-resolved path.
 *
 * Author: vyrriox
 */
public record SelectRecipePacket(int containerId, ResourceLocation recipeId) implements CustomPacketPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<SelectRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RsPolymorph.MOD_ID, "select_recipe"));

    public static final StreamCodec<FriendlyByteBuf, SelectRecipePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    SelectRecipePacket::containerId,
                    ResourceLocation.STREAM_CODEC,
                    SelectRecipePacket::recipeId,
                    SelectRecipePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Pure server-side application of the selection. MUST be called on the server thread.
     *
     * Everything the client sent is validated: the menu must still be the open one, the recipe must
     * exist, the menu must hold a matrix of that recipe's type, and the recipe must match that
     * matrix's current input. A selection equal to the stored one is ignored, so a repeated packet
     * neither dirties the block entity nor recomputes the grid.
     *
     * Debug logging only: a modified client must not be able to flood the server log.
     */
    public static void applyOnServer(ServerPlayer player, int containerId, ResourceLocation recipeId) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId != containerId) {
            LOGGER.debug("[RS Polymorph] Selection for a menu that is no longer open: {}", recipeId);
            return;
        }

        Level level = player.level();
        RecipeHolder<?> recipe = RsPolymorph.findRecipe(level, recipeId);
        if (recipe == null) {
            LOGGER.debug("[RS Polymorph] Server received unknown recipe ID: {}", recipeId);
            return;
        }

        RecipeMatrix<?, ?> matrix = findMatrix(menu, recipe.value().getType());
        if (matrix == null || !RsPolymorph.matchesCurrentInput(matrix, recipeId, level)) {
            LOGGER.debug("[RS Polymorph] Recipe {} does not apply to the open grid", recipeId);
            return;
        }

        if (recipeId.equals(RsPolymorph.selectionOf(matrix))) return;

        RsPolymorph.select(matrix, recipeId);
        // Recompute now: MixinRecipeMatrix writes the chosen output into the result slot, which the
        // menu's broadcastChanges then syncs to the client.
        matrix.updateResult(level);
    }

    /**
     * Finds the matrix of the given recipe type in the OPEN menu, so a selection can only ever apply
     * to the grid the player is looking at. Crafting grids (wired or wireless) and the pattern grid
     * all build their matrix slots over the {@link RecipeMatrixContainer}; other menus have none.
     */
    private static RecipeMatrix<?, ?> findMatrix(AbstractContainerMenu menu, RecipeType<?> type) {
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof RecipeMatrixContainer container)) continue;
            RecipeMatrix<?, ?> matrix = RsPolymorph.matrixOf(container);
            if (matrix != null && ((IRsRecipeMatrix<?, ?>) matrix).rspolymorph$getRecipeType() == type) {
                return matrix;
            }
        }
        return null;
    }
}
