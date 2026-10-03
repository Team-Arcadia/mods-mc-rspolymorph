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

import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.support.containermenu.DisabledSlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;

/**
 * Client-only helpers for the standalone recipe-selection UI. No Polymorph: the widget is created
 * directly when a grid screen initializes (from {@code MixinAbstractBaseScreen}) and the result
 * slot is detected from RS's own slot types.
 *
 * Author: vyrriox
 */
public final class ClientSetup {

    private ClientSetup() {}

    /** Retained as a no-op entry point for symmetry with the loader entrypoints. */
    public static void init() {
        // Nothing to register globally anymore — the widget is built per screen in onGridScreenInit.
    }

    /**
     * Builds the {@link RsGridRecipeWidget} for a freshly-initialized grid screen, locating the
     * crafting result slot to anchor the popup. Called from {@code MixinAbstractBaseScreen} after
     * {@code init()} so all slots exist.
     */
    public static void onGridScreenInit(AbstractGridScreen<?> screen) {
        Slot resultSlot = findResultSlot(screen);
        if (resultSlot != null) {
            // Constructing the widget registers it as the active instance.
            new RsGridRecipeWidget(screen, resultSlot);
        }
    }

    /**
     * Finds the crafting result slot on the grid screen. RS's own {@code CraftingGridResultSlot}
     * is package-private, so we match on the vanilla {@code ResultSlot} it extends first, then fall
     * back to any active {@code DisabledSlot} (public RS slot used for the grid result), then any
     * {@code DisabledSlot}. This mirrors the detection the mod used before going standalone.
     */
    private static Slot findResultSlot(AbstractContainerScreen<?> screen) {
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof ResultSlot) {
                return slot;
            }
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot.isActive() && slot instanceof DisabledSlot) {
                return slot;
            }
        }
        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof DisabledSlot) {
                return slot;
            }
        }
        return null;
    }
}
