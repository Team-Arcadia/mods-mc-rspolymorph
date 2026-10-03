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
package com.vyrriox.rspolymorph;

import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.vyrriox.rspolymorph.platform.Services;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loader-agnostic core for RS Polymorph — integrates Polymorph recipe selection with the
 * Refined Storage 2 grids. This class holds the cross-thread registry maps and the selection
 * state and is shared by both the NeoForge and Fabric entrypoints.
 *
 * IMPORTANT: This class must NEVER reference loader-specific types (NeoForge {@code @Mod},
 * Fabric {@code ModInitializer}, {@code DeferredRegister}, {@code PacketDistributor}, …) or
 * client-only types. Loader wiring lives in the {@code neoforge}/{@code fabric} subprojects;
 * the data-component instance is injected by each loader via {@link #setSelectedRecipeComponent}.
 *
 * All client code lives in {@code com.vyrriox.rspolymorph.client} and is reached only on the
 * client dist.
 *
 * Author: vyrriox
 */
public final class RsPolymorph {

    public static final String MOD_ID = "rspolymorph";

    private RsPolymorph() {}

    // ── Data component ────────────────────────────────────────────────────────
    /**
     * The {@code selected_recipe} data component, stored on crafting pattern items to persist the
     * user's Polymorph recipe selection. Registered by each loader (NeoForge {@code DeferredRegister}
     * / Fabric {@code Registry.register}) and injected here via {@link #setSelectedRecipeComponent}.
     */
    private static volatile DataComponentType<Identifier> selectedRecipeComponent;

    /** Stable resource id for the data component; both loaders register under this id. */
    public static final Identifier SELECTED_RECIPE_COMPONENT_ID =
            Identifier.fromNamespaceAndPath(MOD_ID, "selected_recipe");

    public static void setSelectedRecipeComponent(DataComponentType<Identifier> component) {
        selectedRecipeComponent = component;
    }

    /** The registered {@code selected_recipe} data component. Never null after loader setup. */
    public static DataComponentType<Identifier> selectedRecipeComponent() {
        DataComponentType<Identifier> c = selectedRecipeComponent;
        if (c == null) {
            throw new IllegalStateException(
                    "selected_recipe data component accessed before loader registration");
        }
        return c;
    }

    // ── Registry maps ─────────────────────────────────────────────────────────
    /** RecipeMatrixContainer → the BlockEntity that owns it. Populated on BE construction. */
    private static final Map<RecipeMatrixContainer, BlockEntity> CONTAINER_TO_BE =
            new ConcurrentHashMap<>();

    /** RecipeMatrixContainer → the RecipeMatrix wrapping it. Populated on RecipeMatrix construction. */
    private static final Map<RecipeMatrixContainer, RecipeMatrix<?, ?>> CONTAINER_TO_MATRIX =
            new ConcurrentHashMap<>();

    /**
     * RecipeMatrix → the recipe selection chosen for it, for grids that have NO BlockEntity
     * (e.g. the Quartz Arsenal wireless crafting grid). The BlockEntity-keyed Polymorph
     * capability cannot persist a selection for these, so we keep it here keyed by the matrix.
     *
     * Reclamation is deterministic via {@link #unregisterMatrix} on menu close — that is the
     * load-bearing mechanism, because {@code CONTAINER_TO_MATRIX} strong-pins each matrix and
     * would otherwise keep this entry reachable forever. The WeakHashMap with weak keys is kept
     * only as a best-effort safety net (e.g. if a close hook is ever missed). Wrapped in a
     * synchronized map because it is touched from the server thread (packet handler), the matrix
     * mixin's updateResult, and the menu-close hook.
     */
    private static final Map<RecipeMatrix<?, ?>, Identifier> MATRIX_SELECTION =
            Collections.synchronizedMap(new WeakHashMap<>());

    // ── Selection state ───────────────────────────────────────────────────────
    /**
     * Recipe ID chosen by the client via the Polymorph popup.
     * In singleplayer the JVM is shared, so server-side code reads this directly.
     * Volatile for cross-thread visibility.
     */
    private static volatile Identifier selectedRecipeId = null;

    // ── Public API ────────────────────────────────────────────────────────────

    public static void setSelectedRecipeId(Identifier id) {
        selectedRecipeId = id;
    }

    public static Identifier getSelectedRecipeId() {
        return selectedRecipeId;
    }

    public static Map<RecipeMatrixContainer, BlockEntity> getMatrixMap() {
        return CONTAINER_TO_BE;
    }

    public static Map<RecipeMatrixContainer, RecipeMatrix<?, ?>> getContainerToMatrixMap() {
        return CONTAINER_TO_MATRIX;
    }

    public static void registerContainerBlockEntity(RecipeMatrixContainer container, BlockEntity be) {
        CONTAINER_TO_BE.put(container, be);
        reapStaleEntries();
    }

    public static BlockEntity getBlockEntityForContainer(RecipeMatrixContainer container) {
        return CONTAINER_TO_BE.get(container);
    }

    public static void registerMatrixToContainer(RecipeMatrixContainer container, RecipeMatrix<?, ?> matrix) {
        CONTAINER_TO_MATRIX.put(container, matrix);
    }

    /**
     * Reclaims registrations whose owning grid BlockEntity has been removed (block broken / chunk
     * unloaded). Without this, the strong {@code CONTAINER_TO_BE} / {@code CONTAINER_TO_MATRIX} maps
     * would pin every wired-grid BlockEntity — and through it an entire {@code ServerLevel} — for
     * the server lifetime. Called opportunistically at the registration sites, so growth is matched
     * by cleanup and the maps stay bounded to live grids. Uses the stable vanilla
     * {@code BlockEntity#isRemoved()}, so it behaves identically across MC versions.
     */
    public static void reapStaleEntries() {
        CONTAINER_TO_BE.entrySet().removeIf(e -> {
            if (!e.getValue().isRemoved()) return false;
            RecipeMatrix<?, ?> m = CONTAINER_TO_MATRIX.remove(e.getKey());
            if (m != null) MATRIX_SELECTION.remove(m);
            return true;
        });
    }

    // ── Per-matrix selection (BlockEntity-free grids, e.g. wireless crafting grid) ──

    /**
     * Persists a recipe selection for a grid that has no BlockEntity. {@code null} clears it.
     *
     * Guarded so the per-matrix store can NEVER shadow a BlockEntity-backed matrix: if the
     * matrix's container is registered to a BlockEntity, the selection must go through the
     * persistent grid store ({@code Services.GRID_STORE}) instead, and we ignore the store request.
     * This keeps path-2 in {@code MixinRecipeMatrix} exclusively the non-BE channel.
     */
    public static void setMatrixSelection(RecipeMatrix<?, ?> matrix, Identifier recipeId) {
        if (matrix == null) return;
        if (recipeId == null) {
            MATRIX_SELECTION.remove(matrix);
            return;
        }
        // BE-backed matrix → use the persistent grid store path, not this in-memory store.
        if (CONTAINER_TO_BE.get(matrix.getMatrix()) != null) return;
        MATRIX_SELECTION.put(matrix, recipeId);
    }

    /** Returns the per-matrix recipe selection, or {@code null} if none was stored. */
    public static Identifier getMatrixSelection(RecipeMatrix<?, ?> matrix) {
        if (matrix == null) return null;
        return MATRIX_SELECTION.get(matrix);
    }

    /**
     * Deterministically reclaims the registrations for a BlockEntity-free grid's crafting matrix
     * when its menu closes. Removes the strong {@code CONTAINER_TO_MATRIX} entry (the actual leak
     * root — the WeakHashMap selection store alone cannot collect because this map strong-pins the
     * matrix) and the per-matrix selection in one go.
     *
     * CONTRACT: only call for grids that have NO BlockEntity. For BE-backed grids the mapping is
     * owned by the BlockEntity lifecycle and must not be dropped on menu close.
     */
    public static void unregisterMatrix(RecipeMatrixContainer container) {
        if (container == null) return;
        RecipeMatrix<?, ?> m = CONTAINER_TO_MATRIX.remove(container);
        if (m != null) MATRIX_SELECTION.remove(m);
    }

    /**
     * Returns the Polymorph-persisted recipe for the given matrix, if one was selected
     * and still matches the current grid inputs. Returns null otherwise.
     */
    @SuppressWarnings("unchecked")
    public static RecipeHolder<?> getRecipe(RecipeMatrix<?, ?> matrix, Level level) {
        if (matrix.getMatrix().isEmpty()) return null;

        BlockEntity be = CONTAINER_TO_BE.get(matrix.getMatrix());
        if (be == null) return null;
        if (!(matrix instanceof IRsRecipeMatrix<?, ?> rsMatrix)) return null;

        RecipeType<?> type = rsMatrix.rspolymorph$getRecipeType();
        Identifier selectedId = Services.GRID_STORE.get(be, type);
        if (selectedId == null) return null;

        // Server-only resolution; guard the cast (updateResult can run client-side too).
        if (!(level instanceof ServerLevel serverLevel)) return null;
        MinecraftServer server = serverLevel.getServer();
        ResourceKey<Recipe<?>> selectedKey = ResourceKey.create(Registries.RECIPE, selectedId);
        RecipeHolder<?> selected = server.getRecipeManager().byKey(selectedKey).orElse(null);
        if (selected == null) return null;

        // Validate the stored selection is still of the right type and matches the current inputs.
        if (selected.value().getType() != type) return null;
        Recipe<RecipeInput> recipe = (Recipe<RecipeInput>) selected.value();
        RecipeInput input = (RecipeInput) rsMatrix.rspolymorph$getInputProvider().apply(matrix.getMatrix());
        return (input != null && recipe.matches(input, level)) ? selected : null;
    }
}