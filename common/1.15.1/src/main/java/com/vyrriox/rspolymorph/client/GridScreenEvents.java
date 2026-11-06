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

import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.api.network.grid.IGrid;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.raoulvdberge.refinedstorage.screen.grid.GridScreen;
import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * Connects a {@link GridRecipeWidget} to each crafting or pattern grid screen. Forge's screen events draw and
 * listen; only the button comes from the coremod, as the screen has no event for it. Found by Forge's annotation scan, on the
 * client only: no server code references this class.
 *
 * Author: vyrriox
 */
@Mod.EventBusSubscriber(modid = RsPolymorph.MOD_ID, value = Dist.CLIENT)
public final class GridScreenEvents {

    private GridScreenEvents() {
    }

    @Nullable
    private static GridRecipeWidget widgetOf(Screen screen) {
        return screen instanceof IRsPolymorphScreen ? ((IRsPolymorphScreen) screen).rspolymorph$getWidget() : null;
    }

    /** Called by the coremod at the end of the screen's onPostInit, on every init, which clears its buttons. */
    public static void onPostInit(GridScreen screen) {
        IRsPolymorphScreen owner = (IRsPolymorphScreen) screen;
        GridRecipeWidget widget = owner.rspolymorph$getWidget();
        if (widget == null) {
            IGrid grid = screen.getGrid();
            if (grid instanceof GridNetworkNode
                    && (grid.getGridType() == GridType.CRAFTING || grid.getGridType() == GridType.PATTERN)) {
                widget = new GridRecipeWidget(screen, (GridNetworkNode) grid);
                owner.rspolymorph$setWidget(widget);
            }
        }
        if (widget != null) {
            widget.attachButton();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            GridRecipeWidget widget = widgetOf(Minecraft.getInstance().screen);
            if (widget != null) {
                widget.tick();
            }
        }
    }

    /**
     * Moves the cursor out of the screen's reach under an overlay: no slot hover and no tooltip. The Pre event
     * cannot change the cursor it is given, so it draws the screen itself. The Post event still fires. Last in
     * line, so that everyone else listening has seen the frame.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null && widget.hidesHover()) {
            event.setCanceled(true);
            event.getGui().render(-1, -1, event.getRenderPartialTicks());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null) {
            widget.renderOverlays();
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null && widget.mouseClicked(event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(GuiScreenEvent.MouseReleasedEvent.Pre event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null && widget.consumeRelease()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyPressed(GuiScreenEvent.KeyboardKeyPressedEvent.Pre event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null && widget.keyPressed(event.getKeyCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onCharTyped(GuiScreenEvent.KeyboardCharTypedEvent.Pre event) {
        GridRecipeWidget widget = widgetOf(event.getGui());
        if (widget != null && widget.consumeChar()) {
            event.setCanceled(true);
        }
    }
}
