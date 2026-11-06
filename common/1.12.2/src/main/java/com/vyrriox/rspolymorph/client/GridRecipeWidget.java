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

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.gui.grid.GuiGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.client.RecipeSelectorPopup.Entry;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import com.vyrriox.rspolymorph.network.SelectRecipeMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One per open grid screen: finds the matching recipes when the matrix changes, and owns the side
 * button, the popup and the card. The list is rebuilt from the matrix hash read once per client
 * tick, so nothing here scans or allocates per frame.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
final class GridRecipeWidget {

    final GuiGrid screen;
    final NetworkNodeGrid node;
    private final Canvas canvas = new Canvas();
    final RecipeSelectorPopup popup = new RecipeSelectorPopup(this::select);

    RecipeSelectionSideButton button;
    private List<Entry> candidates = Collections.emptyList();
    private long matrixHash;
    private boolean matrixHashValid;
    /** How many times the recipes were scanned, for the selftest. */
    int scans;

    boolean showTutorial;
    /** The release after a click the widget consumed must not reach the slot under the cursor. */
    private boolean swallowRelease;

    GridRecipeWidget(GuiGrid screen, NetworkNodeGrid node) {
        this.screen = screen;
        this.node = node;
        this.showTutorial = !TutorialState.hasSeen();
    }

    /** Called on each init of the screen, which drops its buttons and rebuilds its slots. */
    void attachButton() {
        popup.close();
        button = new RecipeSelectionSideButton(screen, this, canvas);
        screen.addSideButton(button);
        matrixHashValid = false;
        tick();
    }

    void tick() {
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
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || node.getCraftingMatrix().isEmpty()) {
            return Collections.emptyList();
        }
        scans++;
        List<IRecipe> recipes = RecipeAccess.candidates(mc.world, node.getCraftingMatrix());
        if (recipes.size() < 2) {
            return Collections.emptyList();
        }
        List<Entry> entries = new ArrayList<>(recipes.size());
        for (IRecipe recipe : recipes) {
            ItemStack output = RecipeAccess.assemble(recipe, node.getCraftingMatrix());
            ResourceLocation id = RecipeAccess.idOf(recipe);
            if (!output.isEmpty() && id != null) {
                entries.add(new Entry(id, output));
            }
        }
        return entries;
    }

    List<Entry> candidates() {
        return candidates;
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
            for (Slot slot : screen.inventorySlots.inventorySlots) {
                if (slot.inventory == node.getCraftingResult()) {
                    anchorX += slot.xPos;
                    anchorY += slot.yPos;
                    break;
                }
            }
            popup.open(candidates, anchorX, anchorY, screen.width, screen.height);
        }
    }

    private void select(ResourceLocation recipeId) {
        RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(screen.inventorySlots.windowId, recipeId));
    }

    /** @return true while an overlay covers the cursor, so the screen must not show any tooltip */
    boolean hidesHover(int mouseX, int mouseY) {
        return showTutorial || popup.isMouseOver(mouseX, mouseY);
    }

    /** Drawn after the whole screen. */
    void renderOverlays(int mouseX, int mouseY) {
        if (!popup.isOpen() && !showTutorial) {
            return;
        }
        // Above the items of the screen, which have written their depth at Z 150 and more.
        canvas.pushLayer(400.0f);
        popup.render(canvas, screen, mouseX, mouseY, node.getCraftingResult().getStackInSlot(0));
        if (showTutorial) {
            // Seen from the first frame: closing the screen without dismissing it counts too.
            TutorialState.markSeen();
            TutorialOverlay.render(canvas, screen.width, screen.height);
        }
        canvas.popLayer();
    }

    /** @return true if the click must not reach the screen */
    boolean mouseClicked(int mouseX, int mouseY) {
        boolean consumed;
        if (showTutorial) {
            showTutorial = false;
            consumed = true;
        } else if (popup.isOpen() && button.mousePressed(screen.mc, mouseX, mouseY)) {
            // Handled here, or the press would close the popup and the button reopen it.
            popup.close();
            consumed = true;
        } else {
            consumed = popup.mouseClicked(mouseX, mouseY);
        }
        swallowRelease = consumed;
        return consumed;
    }

    boolean consumeRelease() {
        boolean swallow = swallowRelease;
        swallowRelease = false;
        return swallow;
    }

    /** @return true if the key must not reach the screen */
    boolean keyPressed(int key) {
        if (showTutorial && (key == Keyboard.KEY_SPACE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER)) {
            showTutorial = false;
            return true;
        }
        if (popup.isOpen() && key == Keyboard.KEY_ESCAPE) {
            popup.close();
            return true;
        }
        return false;
    }
}
