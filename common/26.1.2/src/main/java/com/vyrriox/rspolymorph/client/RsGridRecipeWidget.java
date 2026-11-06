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

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.platform.Services;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recipe-selection driver for an RS grid screen. It discovers the candidate recipes for the open
 * grid and drives a {@link RecipeSelectorPopup}. The selection is sent to the server through
 * {@link Services#network()}.
 *
 * One instance exists per open grid screen ({@link #activeInstance}); it is created by
 * {@code MixinAbstractBaseScreen}, drawn and clicked through the screen mixins, and released by
 * {@code MixinContainerScreenRemoved} when the screen closes.
 *
 * Author: vyrriox
 */
public class RsGridRecipeWidget {

    private static RsGridRecipeWidget activeInstance = null;

    private final Slot outputSlot;
    private final AbstractContainerScreen<?> screen;
    /** The menu's slots never change, so the matrix containers are found once. */
    private final List<RecipeMatrixContainer> containers;
    private final RecipeSelectorPopup popup;

    /** True while the one-time first-open tutorial card should be shown over this screen. */
    private boolean showTutorial = false;
    /** Persist + narrate the tutorial exactly once while it is up (the card stays until dismissed). */
    private boolean tutorialPersisted = false;
    private boolean narratorAnnounced = false;

    /** Grid contents hash at the moment the popup was opened (popup closes if inputs change). */
    private int popupOpenedAtHash = 0;

    // hasMultipleRecipes() cache: recomputed only when the input hash changes.
    private boolean cachedHasMultiple = false;
    private int lastHashForMultipleCheck = Integer.MIN_VALUE;

    /** A candidate recipe together with the input it was matched against (used to assemble output). */
    private record RecipeMatch(RecipeHolder<?> holder, CraftingInput input) {}

    public RsGridRecipeWidget(AbstractContainerScreen<?> screen, Slot outputSlot, List<RecipeMatrixContainer> containers) {
        this.screen = screen;
        this.outputSlot = outputSlot;
        this.containers = containers;
        this.popup = new RecipeSelectorPopup(this::selectRecipe);
        this.showTutorial = !TutorialState.hasSeen();
        activeInstance = this;
    }

    public static RsGridRecipeWidget getActiveInstance() {
        return activeInstance;
    }

    /** Clears the active instance when its screen closes (see MixinContainerScreenRemoved). */
    public static void clearIfActive(AbstractContainerScreen<?> screen) {
        if (activeInstance != null && activeInstance.screen == screen) {
            activeInstance = null;
        }
    }

    public boolean isPopupOpen() {
        return popup.isOpen();
    }

    /** True while the one-time tutorial card is up (routes input to its dismiss handler). */
    public boolean isShowingTutorial() {
        return showTutorial;
    }

    /** True if this widget belongs to the given screen (guards every screen mixin). */
    public boolean isOpenForScreen(AbstractContainerScreen<?> screen) {
        return this.screen == screen;
    }

    // -------------------------------------------------------------------------
    // Container discovery + recipe query
    // -------------------------------------------------------------------------

    /**
     * The distinct matrix containers of a menu, empty for a grid that cannot craft. Client-side menus
     * build their matrix slots over a bare container of their own, on the crafting and pattern grids
     * alike. A grid has only 1-2 of them, so a reference-identity linear check is enough.
     */
    public static List<RecipeMatrixContainer> findContainers(AbstractContainerMenu menu) {
        List<RecipeMatrixContainer> found = new ArrayList<>(2);
        for (Slot slot : menu.slots) {
            if (slot.container instanceof RecipeMatrixContainer rmc && !containsRef(found, rmc)) {
                found.add(rmc);
            }
        }
        return found;
    }

    private static boolean containsRef(List<RecipeMatrixContainer> list, RecipeMatrixContainer c) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == c) return true;
        }
        return false;
    }

    private static List<RecipeMatch> queryRecipes(RecipeMatrixContainer container, ClientLevel level) {
        int size = container.getContainerSize();
        int side = size == 9 ? 3 : size == 4 ? 2 : 0;
        if (side == 0) return List.of();

        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) items.add(container.getItem(i));
        CraftingInput input = CraftingInput.of(side, side, items);

        List<RecipeMatch> matches = new ArrayList<>();
        for (RecipeHolder<?> holder : Platform.INSTANCE.getClientRecipeProvider(level)
                .getRecipesFor(RecipeType.CRAFTING, input, level).toList()) {
            matches.add(new RecipeMatch(holder, input));
        }
        return matches;
    }

    // -------------------------------------------------------------------------
    // Input hash
    // -------------------------------------------------------------------------

    /**
     * Hash of what recipes match on: item and components, not the count, so a stack shrinking during
     * a craft neither runs a new recipe query nor closes the popup.
     */
    private int computeInputHash() {
        int hash = 1;
        for (int c = 0; c < containers.size(); c++) {
            RecipeMatrixContainer container = containers.get(c);
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                hash = 31 * hash + (stack.isEmpty() ? 0 : ItemStack.hashItemAndComponents(stack));
            }
        }
        return hash;
    }

    // -------------------------------------------------------------------------
    // hasMultipleRecipes (drives side-button active state)
    // -------------------------------------------------------------------------

    public boolean hasMultipleRecipes() {
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
        for (RecipeMatrixContainer container : containers) {
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

        // Collect distinct candidate recipes (id -> output) across all containers. The output is
        // assembled from the input: a dynamic recipe has no fixed result item to show.
        Map<Identifier, ItemStack> byId = new LinkedHashMap<>();
        for (RecipeMatrixContainer container : containers) {
            if (container.isEmpty()) continue;
            for (RecipeMatch match : queryRecipes(container, level)) {
                byId.computeIfAbsent(RsPolymorph.recipeId(match.holder()),
                        k -> RsPolymorph.assemble(match.holder(), match.input(), level));
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
        // Uniform SP/MP path. The container id lets the server drop a packet that arrives after the
        // player moved to another menu.
        Services.network().sendSelectToServer(screen.getMenu().containerId, recipeId);
    }

    // -------------------------------------------------------------------------
    // Render + click
    // -------------------------------------------------------------------------

    public void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
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

    /** Reads the tutorial text aloud once for screen-reader users. Fail-soft. */
    private static void announceTutorialToNarrator() {
        try {
            Component msg = Component.translatable("rspolymorph.tutorial.title")
                    .copy()
                    .append(". ")
                    .append(Component.translatable("rspolymorph.tutorial.line1"))
                    .append(" ")
                    .append(Component.translatable("rspolymorph.tutorial.line2"))
                    .append(" ")
                    .append(Component.translatable("rspolymorph.tutorial.dismiss"));
            Minecraft.getInstance().getNarrator().saySystemNow(msg);
        } catch (Throwable ignored) {
            // A narrator failure must never break the screen.
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
