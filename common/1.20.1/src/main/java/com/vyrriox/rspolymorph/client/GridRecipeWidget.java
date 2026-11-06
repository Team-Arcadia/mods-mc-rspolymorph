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

import com.refinedmods.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.refinedmods.refinedstorage.screen.BaseScreen;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.client.RecipeSelectorPopup.Entry;
import com.vyrriox.rspolymorph.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe selection for one open grid screen, owned by that screen. The candidates are scanned once
 * per change of the matrix, checked from the screen tick by a hash of its items and NBT; rendering
 * only reads the result of that scan.
 *
 * Author: vyrriox
 */
public final class GridRecipeWidget {

    private final BaseScreen<?> screen;
    private final GridNetworkNode node;
    private final Canvas canvas = new Canvas();
    private final RecipeSelectorPopup popup = new RecipeSelectorPopup(this::select);

    private RecipeSelectionSideButton button;
    private List<Entry> candidates = List.of();
    private long matrixHash;
    private boolean matrixHashValid;

    private boolean showTutorial;
    private boolean tutorialAnnounced;
    /** The release after a click the widget consumed must not reach the slot under the cursor. */
    private boolean swallowRelease;
    /** Space also arrives as a typed character, right after its key press. */
    private boolean swallowChar;

    public GridRecipeWidget(BaseScreen<?> screen, GridNetworkNode node) {
        this.screen = screen;
        this.node = node;
        this.showTutorial = !TutorialState.hasSeen();
    }

    public Canvas canvas() {
        return canvas;
    }

    /** Called on each init of the screen, which drops its buttons and rebuilds its slots. */
    public void attachButton() {
        popup.close();
        button = new RecipeSelectionSideButton(screen, this);
        screen.addSideButton(button);
        matrixHashValid = false;
        tick();
    }

    public void tick() {
        swallowChar = false;
        long hash = RecipeAccess.contentHash(node.getCraftingMatrix());
        if (!matrixHashValid || hash != matrixHash) {
            matrixHash = hash;
            matrixHashValid = true;
            popup.close();
            candidates = findCandidates();
        }
        button.visible = candidates.size() > 1 && !node.isProcessingPattern();
    }

    private List<Entry> findCandidates() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || node.getCraftingMatrix().isEmpty()) {
            return List.of();
        }
        List<CraftingRecipe> recipes = RecipeAccess.candidates(level, node.getCraftingMatrix());
        if (recipes.size() < 2) {
            return List.of();
        }
        List<Entry> entries = new ArrayList<>(recipes.size());
        for (CraftingRecipe recipe : recipes) {
            ItemStack output = RecipeAccess.assemble(recipe, node.getCraftingMatrix(), level);
            if (!output.isEmpty()) {
                entries.add(new Entry(RecipeAccess.idOf(recipe), output));
            }
        }
        return entries;
    }

    boolean isPopupOpen() {
        return popup.isOpen();
    }

    void togglePopup() {
        if (popup.isOpen()) {
            popup.close();
        } else if (candidates.size() > 1) {
            int anchorX = screen.getGuiLeft();
            int anchorY = screen.getGuiTop();
            for (Slot slot : screen.getMenu().slots) {
                if (slot.container == node.getCraftingResult()) {
                    anchorX += slot.x;
                    anchorY += slot.y;
                    break;
                }
            }
            popup.open(candidates, anchorX, anchorY);
        }
    }

    private void select(ResourceLocation recipeId) {
        Services.network().sendSelectRecipe(screen.getMenu().containerId, recipeId);
    }

    /** @return true while an overlay covers the cursor, so the screen must not show any hover */
    public boolean hidesHover() {
        return showTutorial || popup.isMouseOver(Canvas.mouseX(), Canvas.mouseY());
    }

    /** Drawn after the whole screen; call {@code canvas().begin} first. */
    public void renderOverlays() {
        if (!popup.isOpen() && !showTutorial) {
            return;
        }
        // Above the items of the screen, which have written their depth at Z 150 and more.
        canvas.pushLayer(400.0f);
        popup.render(canvas, Canvas.mouseX(), Canvas.mouseY(), node.getCraftingResult().getItem(0));
        if (showTutorial) {
            if (!tutorialAnnounced) {
                tutorialAnnounced = true;
                // Seen from the first frame: closing the screen without dismissing it counts too.
                TutorialState.markSeen();
                Minecraft.getInstance().getNarrator().say(TutorialOverlay.narration());
            }
            TutorialOverlay.render(canvas);
        }
        canvas.popLayer();
    }

    /** @return true if the click must not reach the screen */
    public boolean mouseClicked(double mouseX, double mouseY) {
        boolean consumed;
        if (showTutorial) {
            showTutorial = false;
            consumed = true;
        } else if (popup.isOpen() && button.isMouseOver(mouseX, mouseY)) {
            // Handled here, or the press would close the popup and the button reopen it.
            popup.close();
            consumed = true;
        } else {
            consumed = popup.mouseClicked(mouseX, mouseY);
        }
        swallowRelease = consumed;
        return consumed;
    }

    public boolean consumeRelease() {
        boolean swallow = swallowRelease;
        swallowRelease = false;
        return swallow;
    }

    /** @return true if the key must not reach the screen */
    public boolean keyPressed(int key) {
        if (showTutorial && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) {
            showTutorial = false;
            swallowChar = key == GLFW.GLFW_KEY_SPACE;
            return true;
        }
        if (popup.isOpen() && key == GLFW.GLFW_KEY_ESCAPE) {
            popup.close();
            return true;
        }
        return false;
    }

    public boolean consumeChar() {
        boolean swallow = swallowChar;
        swallowChar = false;
        return swallow;
    }
}
