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
import com.vyrriox.rspolymorph.platform.Services;
import com.refinedmods.refinedstorage.common.grid.CraftingGrid;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.vyrriox.rspolymorph.mixin.AccessorAbstractCraftingGridContainerMenu;
import com.vyrriox.rspolymorph.mixin.AccessorAbstractGridContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Optional;

/**
 * Client → Server payload carrying a Polymorph recipe selection.
 *
 * This class is loader-agnostic: it defines the payload {@link Type}, its {@link #STREAM_CODEC},
 * and the pure server-side application logic in {@link #applyOnServer(ServerPlayer, ResourceLocation)}.
 * Each loader subproject registers the payload and its receiver, then calls {@code applyOnServer}
 * on the server thread — NeoForge via {@code IPayloadContext#enqueueWork}, Fabric via
 * {@code ServerPlayNetworking} on the server executor.
 *
 * In singleplayer the JVM is shared, but the selection is still routed through this payload over
 * the local loopback so both SP and MP take the same server-resolved path.
 *
 * Author: vyrriox
 */
public record SelectRecipePacket(ResourceLocation recipeId) implements CustomPacketPayload {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Type<SelectRecipePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(RsPolymorph.MOD_ID, "select_recipe"));

    public static final StreamCodec<FriendlyByteBuf, SelectRecipePacket> STREAM_CODEC =
            StreamCodec.composite(
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
     * Looks up the recipe by ID and applies it to the grid backing the player's currently open
     * crafting menu. Loader receivers call this after decoding the payload and hopping to the
     * server executor.
     *
     * Strategy 1: scan menu slots for RecipeMatrixContainer (works for CraftingGrid).
     * Strategy 2: use the menu accessor to get the Grid field (works for PatternGrid
     *             where slots are phantom/filter and don't expose RecipeMatrixContainer).
     * Strategy 3: BlockEntity-free grids (e.g. the wireless crafting grid) — drive the
     *             selection through the RecipeMatrix directly.
     */
    public static void applyOnServer(ServerPlayer player, ResourceLocation recipeId) {
        Level level = player.level();
        Optional<RecipeHolder<?>> recipeOpt = level.getRecipeManager().byKey(recipeId);
        if (recipeOpt.isEmpty()) {
            // Debug, not warn: a modified client must not be able to flood the server log.
            LOGGER.debug("[RS Polymorph] Server received unknown recipe ID: {}", recipeId);
            return;
        }
        RecipeHolder<?> recipe = recipeOpt.get();

        // The static selectedRecipeId is read by MixinPatternGrid.createCraftingPattern() and
        // MixinRecipeMatrix while we recompute the grid this tick. Set it ONCE here and clear it
        // unconditionally in the finally, so NO return path (BE, BE-free, or not-found) can leave a
        // stale value that would mis-tag a later, unrelated craft.
        RsPolymorph.setSelectedRecipeId(recipeId);
        try {
            BlockEntity targetBe = findBlockEntity(player.containerMenu);
            if (targetBe != null) {
                RecipeType<?> type = recipe.value().getType();
                // Persist the choice on the grid block entity (survives reload) via the loader store.
                Services.GRID_STORE.set(targetBe, type, recipeId);
                // Recompute the grid result now so the new output syncs to the client this tick.
                updateMatchingMatrices(targetBe, type, level);
                return;
            }

            // No BlockEntity — this is a BlockEntity-free grid (e.g. the Quartz Arsenal
            // wireless crafting grid). Drive the selection through its RecipeMatrix directly.
            if (applyToBlockEntityFreeGrid(player, recipeId)) {
                return;
            }

            LOGGER.debug("[RS Polymorph] Could not find target grid for recipe selection (player: {})",
                    player.getName().getString());
        } finally {
            RsPolymorph.setSelectedRecipeId(null);
        }
    }

    /**
     * Recomputes the result for every {@link RecipeMatrix} owned by {@code be} whose recipe type
     * matches {@code type}. {@code MixinRecipeMatrix} reads the freshly-persisted selection at the
     * {@code updateResult} RETURN and writes the chosen output into the grid's result slot, which
     * {@code broadcastChanges} then syncs to the client. Replaces the old
     * {@code RsGridRecipeData.selectRecipe} trigger.
     */
    private static void updateMatchingMatrices(BlockEntity be, RecipeType<?> type, Level level) {
        Map<RecipeMatrixContainer, BlockEntity> beMap = RsPolymorph.getMatrixMap();
        Map<RecipeMatrixContainer, RecipeMatrix<?, ?>> matrixMap = RsPolymorph.getContainerToMatrixMap();
        for (Map.Entry<RecipeMatrixContainer, BlockEntity> entry : beMap.entrySet()) {
            if (entry.getValue() != be) continue;
            RecipeMatrix<?, ?> matrix = matrixMap.get(entry.getKey());
            if (matrix instanceof IRsRecipeMatrix<?, ?> rsMatrix
                    && rsMatrix.rspolymorph$getRecipeType() == type) {
                matrix.updateResult(level);
            }
        }
    }

    /**
     * Applies the selection to a crafting grid that has no BlockEntity.
     *
     * Resolves the open menu's {@link com.refinedmods.refinedstorage.common.grid.CraftingGrid}
     * via the menu accessor, looks up the {@link RecipeMatrix} that wraps its crafting matrix
     * container (registered by MixinRecipeMatrix on construction), persists the choice in the
     * per-matrix selection store so it survives later updateResult calls, then drives
     * {@code updateResult}. The static selectedRecipeId provides the fast path for this first
     * apply; the per-matrix store keeps it sticky afterwards.
     *
     * The override itself is applied by MixinRecipeMatrix at updateResult RETURN, which writes
     * the chosen output into the grid's ResultContainer via setResult. The result reaches the
     * client through the menu's crafting result slot (added over craftingGrid.getCraftingResult()
     * in the AbstractCraftingGridContainerMenu constructor) on the next broadcastChanges() tick —
     * the same path the wired grid uses. RecipeMatrix.setResult does NOT run the matrix listener,
     * so we do not rely on it for the client sync.
     *
     * @return true if a matrix was found and updated; false if this menu is not a crafting grid
     *         or its matrix is not registered.
     */
    private static boolean applyToBlockEntityFreeGrid(ServerPlayer player, ResourceLocation recipeId) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof AccessorAbstractCraftingGridContainerMenu craftingMenu)) return false;

        CraftingGrid craftingGrid = craftingMenu.rspolymorph$getCraftingGrid();
        if (craftingGrid == null) return false;

        RecipeMatrixContainer container = craftingGrid.getCraftingMatrix();
        if (container == null) return false;

        RecipeMatrix<?, ?> matrix = RsPolymorph.getContainerToMatrixMap().get(container);
        if (matrix == null) return false;

        Level level = player.level();

        // Persist for subsequent updateResult calls (input changes re-apply via MixinRecipeMatrix).
        RsPolymorph.setMatrixSelection(matrix, recipeId);

        // The static selectedRecipeId is already set by the caller (applyOnServer) and cleared in
        // its finally; MixinRecipeMatrix reads it during updateResult and writes the chosen output
        // into the result slot, which broadcastChanges syncs to the client.
        matrix.updateResult(level);
        return true;
    }

    /**
     * Finds the BlockEntity backing the player's open grid menu.
     *
     * Both strategies are scoped to the OPEN menu, so the selection can only ever be applied
     * to the grid the player is actually looking at. (A previous global reverse-scan fallback
     * was removed: it returned the first registered grid with Polymorph data regardless of which
     * menu was open, which mis-routed wireless-grid selections onto an unrelated wired grid and
     * corrupted it.)
     *
     * Strategy 1: scan menu slots for RecipeMatrixContainer → CONTAINER_TO_BE lookup (CraftingGrid).
     * Strategy 2: accessor on AbstractGridContainerMenu to get the Grid field directly (PatternGrid).
     *
     * Returns null for grids with no BlockEntity (e.g. the wireless crafting grid); the caller
     * then routes through {@link #applyToBlockEntityFreeGrid}.
     */
    private static BlockEntity findBlockEntity(AbstractContainerMenu menu) {
        // Strategy 1: slot scan (works for CraftingGrid)
        for (Slot slot : menu.slots) {
            if (!(slot.container instanceof RecipeMatrixContainer rmc)) continue;
            BlockEntity be = RsPolymorph.getBlockEntityForContainer(rmc);
            if (be != null) return be;
        }

        // Strategy 2: accessor on the menu's Grid field (works for PatternGrid)
        if (menu instanceof AccessorAbstractGridContainerMenu accessor) {
            Object grid = accessor.rspolymorph$getGrid();
            if (grid instanceof BlockEntity be) return be;
        }

        return null;
    }
}
