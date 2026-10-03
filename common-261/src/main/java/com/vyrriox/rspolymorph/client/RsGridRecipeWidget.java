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
package com.vyrriox.rspolymorph.client;

import com.vyrriox.rspolymorph.IRsRecipeMatrix;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.mixin.AccessorAbstractGridContainerMenu;
import com.vyrriox.rspolymorph.platform.Services;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Standalone recipe-selection driver for an RS2 grid screen — the Polymorph-free replacement for
 * what used to extend Polymorph's {@code PersistentRecipesWidget}. It discovers the candidate
 * recipes for the open grid and drives a {@link RecipeSelectorPopup} (our own UI). Selection is
 * sent to the server through {@link Services#network()} exactly as before.
 *
 * One instance exists per open grid screen ({@link #activeInstance}); it is created by
 * {@code MixinAbstractBaseScreen} and rendered/clicked by {@code MixinAbstractGridScreenRender}.
 *
 * Author: vyrriox
 */
public class RsGridRecipeWidget {

    private static RsGridRecipeWidget activeInstance = null;

    private final Slot outputSlot;
    private final AbstractContainerScreen<?> screen;
    private final BlockEntity activeBlockEntity;
    private final RecipeSelectorPopup popup;

    /** True while the one-time first-open tutorial card should be shown over this screen. */
    private boolean showTutorial = false;
    /** Persist + narrate the tutorial exactly once while it is up (the card stays until dismissed). */
    private boolean tutorialPersisted = false;
    private boolean narratorAnnounced = false;

    /** Grid contents hash at the moment the popup was opened (popup closes if inputs change). */
    private int popupOpenedAtHash = 0;

    // hasMultipleRecipes() cache — recomputed only when the input hash changes.
    private boolean cachedHasMultiple = false;
    private int lastHashForMultipleCheck = Integer.MIN_VALUE;

    // Per-frame cache for container discovery + input hash.
    private List<RecipeMatrixContainer> cachedContainers = null;
    private int cachedInputHash = Integer.MIN_VALUE;
    private long lastCacheFrame = -1;

    /** A candidate recipe together with the input it was matched against (used to assemble output). */
    private record RecipeMatch(RecipeHolder<?> holder, RecipeInput input) {}

    public RsGridRecipeWidget(AbstractContainerScreen<?> screen, Slot outputSlot) {
        this.screen = screen;
        this.outputSlot = outputSlot;
        this.activeBlockEntity = findBlockEntity(screen);
        this.popup = new RecipeSelectorPopup(this::selectRecipe);
        this.showTutorial = !TutorialState.hasSeen();
        activeInstance = this;
    }

    public static RsGridRecipeWidget getActiveInstance() {
        return activeInstance;
    }

    /** Clears the active instance when its screen closes (called from the screen removed hook). */
    public static void clearIfActive(AbstractContainerScreen<?> screen) {
        if (activeInstance != null && activeInstance.screen == screen) {
            activeInstance = null;
        }
    }

    public Slot getOutputSlot() {
        return outputSlot;
    }

    public boolean isPopupOpen() {
        return popup.isOpen();
    }

    /** True while the one-time tutorial card is up (routes input to its dismiss handler). */
    public boolean isShowingTutorial() {
        return showTutorial;
    }

    /** True if the popup is open and the cursor is over it (drives slot-tooltip suppression). */
    public boolean isMouseOverPopup(double mouseX, double mouseY) {
        return popup.isMouseOver(mouseX, mouseY);
    }

    /** True if this widget belongs to the given screen (guards rendering to the right screen). */
    public boolean isOpenForScreen(AbstractContainerScreen<?> screen) {
        return this.screen == screen;
    }

    // -------------------------------------------------------------------------
    // BlockEntity discovery (unchanged logic from the Polymorph version)
    // -------------------------------------------------------------------------

    private static BlockEntity findBlockEntity(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof RecipeMatrixContainer rmc) {
                BlockEntity be = RsPolymorph.getBlockEntityForContainer(rmc);
                if (be != null) return be;
            }
        }
        if (screen.getMenu() instanceof AccessorAbstractGridContainerMenu accessor) {
            Object grid = accessor.rspolymorph$getGrid();
            if (grid instanceof BlockEntity be) return be;
        }
        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        if (player != null) {
            BlockEntity closest = null;
            double closestDist = Double.MAX_VALUE;
            for (BlockEntity be : new HashSet<>(RsPolymorph.getMatrixMap().values())) {
                if (be.getLevel() == player.level()) {
                    double dist = be.getBlockPos().distSqr(player.blockPosition());
                    if (dist < closestDist) {
                        closestDist = dist;
                        closest = be;
                    }
                }
            }
            if (closest != null && closestDist <= 64) return closest;
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Container discovery + recipe query (unchanged logic)
    // -------------------------------------------------------------------------

    private void refreshFrameCache() {
        long frame = Minecraft.getInstance().getFrameTimeNs();
        if (frame == lastCacheFrame) return;
        lastCacheFrame = frame;
        cachedContainers = null;
        cachedInputHash = Integer.MIN_VALUE;
    }

    private List<RecipeMatrixContainer> getContainers() {
        if (cachedContainers != null) return cachedContainers;

        // A grid has only 1-2 distinct matrix containers, so a reference-identity linear check is
        // cheaper and correct (no autoboxing, no HashSet allocation, no hash-collision risk).
        List<RecipeMatrixContainer> fromSlots = new ArrayList<>(2);
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container instanceof RecipeMatrixContainer rmc && !containsRef(fromSlots, rmc)) {
                fromSlots.add(rmc);
            }
        }
        if (!fromSlots.isEmpty()) {
            cachedContainers = fromSlots;
            return cachedContainers;
        }

        if (activeBlockEntity != null) {
            Map<RecipeMatrixContainer, BlockEntity> beMap = RsPolymorph.getMatrixMap();
            List<RecipeMatrixContainer> fromBe = new ArrayList<>();
            for (Map.Entry<RecipeMatrixContainer, BlockEntity> entry : beMap.entrySet()) {
                if (entry.getValue() == activeBlockEntity) {
                    fromBe.add(entry.getKey());
                }
            }
            if (!fromBe.isEmpty()) {
                cachedContainers = fromBe;
                return cachedContainers;
            }
        }

        cachedContainers = Collections.emptyList();
        return cachedContainers;
    }

    /** Reference-identity membership test (containers are compared by identity, not equals). */
    private static boolean containsRef(List<RecipeMatrixContainer> list, RecipeMatrixContainer c) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == c) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<RecipeMatch> queryRecipes(RecipeMatrixContainer container, ClientLevel level) {
        RecipeMatrix<?, ?> matrix = RsPolymorph.getContainerToMatrixMap().get(container);
        if (matrix instanceof IRsRecipeMatrix<?, ?> rsMatrix) {
            RecipeInput input = (RecipeInput) rsMatrix.rspolymorph$getInputProvider().apply(container);
            if (input != null) {
                RecipeType<Recipe<RecipeInput>> type =
                        (RecipeType<Recipe<RecipeInput>>) rsMatrix.rspolymorph$getRecipeType();
                return wrapMatches(Platform.INSTANCE.getClientRecipeProvider(level)
                        .getRecipesFor(type, input, level).toList(), input);
            }
        }

        int size = container.getContainerSize();
        if (size == 9) {
            List<ItemStack> items = new ArrayList<>(9);
            for (int i = 0; i < 9; i++) items.add(container.getItem(i));
            CraftingInput input = CraftingInput.of(3, 3, items);
            return wrapMatches(Platform.INSTANCE.getClientRecipeProvider(level)
                    .getRecipesFor(RecipeType.CRAFTING, input, level).toList(), input);
        } else if (size == 4) {
            List<ItemStack> items = new ArrayList<>(4);
            for (int i = 0; i < 4; i++) items.add(container.getItem(i));
            CraftingInput input = CraftingInput.of(2, 2, items);
            return wrapMatches(Platform.INSTANCE.getClientRecipeProvider(level)
                    .getRecipesFor(RecipeType.CRAFTING, input, level).toList(), input);
        }
        return List.of();
    }

    private static List<RecipeMatch> wrapMatches(List<? extends RecipeHolder<?>> holders, RecipeInput input) {
        List<RecipeMatch> matches = new ArrayList<>(holders.size());
        for (RecipeHolder<?> holder : holders) {
            matches.add(new RecipeMatch(holder, input));
        }
        return matches;
    }

    @SuppressWarnings("unchecked")
    private static ItemStack assembleOutput(RecipeMatch match, ClientLevel level) {
        Recipe<RecipeInput> recipe = (Recipe<RecipeInput>) match.holder().value();
        return recipe.assemble(match.input());
    }

    // -------------------------------------------------------------------------
    // Input hash
    // -------------------------------------------------------------------------

    private int computeInputHash() {
        if (cachedInputHash != Integer.MIN_VALUE) return cachedInputHash;

        int hash = 1;
        for (RecipeMatrixContainer container : getContainers()) {
            hash = 31 * hash + System.identityHashCode(container);
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty()) {
                    hash = 31 * hash;
                } else {
                    hash = 31 * hash + stack.getItem().hashCode();
                    hash = 31 * hash + stack.getCount();
                }
            }
        }
        cachedInputHash = hash;
        return hash;
    }

    // -------------------------------------------------------------------------
    // hasMultipleRecipes (drives side-button active state)
    // -------------------------------------------------------------------------

    public boolean hasMultipleRecipes() {
        // The side button calls this every frame (in the screen's render, BEFORE renderPopup runs),
        // so refresh the per-frame cache here too. Otherwise the container/hash cache is rebuilt on
        // this call AND again in renderPopup, re-discovering containers + recomputing the hash twice
        // per frame. Gating both render paths on the same frame check rebuilds at most once/frame.
        refreshFrameCache();
        int hash = computeInputHash();
        if (hash != lastHashForMultipleCheck) {
            lastHashForMultipleCheck = hash;
            cachedHasMultiple = queryHasMultipleRecipes();
        }
        return cachedHasMultiple;
    }

    private boolean queryHasMultipleRecipes() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        for (RecipeMatrixContainer container : getContainers()) {
            if (container.isEmpty()) continue;
            if (queryRecipes(container, level).size() > 1) return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Popup open / select
    // -------------------------------------------------------------------------

    /** Toggles the recipe-selection popup. Called by the side button. */
    public void triggerSelection() {
        if (popup.isOpen()) {
            popup.close();
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        // Collect distinct candidate recipes (id -> output) across all containers.
        java.util.LinkedHashMap<Identifier, ItemStack> byId = new java.util.LinkedHashMap<>();
        for (RecipeMatrixContainer container : getContainers()) {
            if (container.isEmpty()) continue;
            for (RecipeMatch match : queryRecipes(container, level)) {
                byId.computeIfAbsent(match.holder().id().identifier(),
                        k -> assembleOutput(match, level));
            }
        }
        if (byId.size() < 2) return;

        List<RecipeSelectorPopup.Entry> entries = new ArrayList<>(byId.size());
        for (Map.Entry<Identifier, ItemStack> e : byId.entrySet()) {
            entries.add(new RecipeSelectorPopup.Entry(e.getKey(), e.getValue()));
        }

        popupOpenedAtHash = computeInputHash();
        // Anchor near the result slot, in screen coordinates.
        int anchorX = ((AccessorScreenOffset) screen).rspolymorph$leftPos() + outputSlot.x;
        int anchorY = ((AccessorScreenOffset) screen).rspolymorph$topPos() + outputSlot.y;
        popup.open(entries, anchorX, anchorY);
    }

    public void selectRecipe(Identifier recipeId) {
        // Uniform SP/MP path via the loader-agnostic network service. The server sets and clears
        // the static selectedRecipeId authoritatively inside applyOnServer's try/finally — we must
        // NOT set it from the client here (in singleplayer that shared static could be left stale
        // and mis-tag a later pattern, since the client never clears it).
        Services.network().sendSelectToServer(recipeId);
    }

    // -------------------------------------------------------------------------
    // Render + click (called from MixinAbstractGridScreenRender)
    // -------------------------------------------------------------------------

    public void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        refreshFrameCache();
        // Auto-close the popup if the grid inputs changed since it opened.
        if (popup.isOpen() && computeInputHash() != popupOpenedAtHash) {
            popup.close();
        }
        // Pass the current output item so the popup can highlight the recipe that is active now.
        popup.render(graphics, mouseX, mouseY, outputSlot.getItem());

        // One-time tutorial card, drawn last so it sits on top. The card stays up until dismissed;
        // persistence + narration fire exactly once (guarded flags), not every frame.
        if (showTutorial) {
            if (!tutorialPersisted) {
                tutorialPersisted = true;
                // Persist "seen" the first time it shows, so it never reappears next session even if
                // the screen is closed (e.g. Esc) without an explicit dismiss.
                TutorialState.markSeen();
            }
            if (!narratorAnnounced) {
                narratorAnnounced = true;
                announceTutorialToNarrator();
            }
            TutorialOverlay.render(graphics);
        }
    }

    /**
     * Reads the tutorial text aloud once for screen-reader users. The 26.x narrator API is not
     * verified here, so the whole call chain is reflective and fail-soft: it narrates when the API
     * matches and silently no-ops otherwise — never breaking the screen.
     */
    private static void announceTutorialToNarrator() {
        try {
            net.minecraft.network.chat.Component msg =
                    net.minecraft.network.chat.Component.translatable("rspolymorph.tutorial.title")
                            .copy()
                            .append(". ")
                            .append(net.minecraft.network.chat.Component.translatable("rspolymorph.tutorial.line1"))
                            .append(" ")
                            .append(net.minecraft.network.chat.Component.translatable("rspolymorph.tutorial.line2"))
                            .append(" ")
                            .append(net.minecraft.network.chat.Component.translatable("rspolymorph.tutorial.dismiss"));
            Object mc = Minecraft.getInstance();
            Object narrator = mc.getClass().getMethod("getNarrator").invoke(mc);
            narrator.getClass().getMethod("say", net.minecraft.network.chat.Component.class).invoke(narrator, msg);
        } catch (Throwable ignored) {
            // Narrator API differences must never break the screen.
        }
    }

    /** @return true if the popup consumed the click (screen must not pass it to slots). */
    public boolean handleClick(double mouseX, double mouseY) {
        if (showTutorial) {
            // Any click dismisses the tutorial card; consume it so it doesn't reach popup/slots/button.
            showTutorial = false;
            return true;
        }
        return popup.mouseClicked(mouseX, mouseY);
    }

    /** Keyboard dismissal of the tutorial card (Space/Enter). @return true if it consumed the key. */
    public boolean dismissTutorialByKey() {
        if (showTutorial) {
            showTutorial = false;
            return true;
        }
        return false;
    }
}