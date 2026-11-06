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
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.container.ContainerGrid;
import com.raoulvdberge.refinedstorage.gui.grid.GuiGrid;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

/**
 * Links the widget to the open grid screen through Forge's screen events, with no change to Refined
 * Storage's GUI classes. The client proxy registers the one instance, and there is at most one open
 * screen, so its widget field is not shared between grids.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
public final class ClientEvents {

    /** Off any screen, and not the -1 of a screen's "no slot" checks. */
    private static final int CURSOR_AWAY = -10000;

    /** Set while the popup draws its own item tooltip. Nothing to skip here: the screen's are not drawn. */
    static boolean drawingOwnTooltip;

    /** The one handler, for the selftest. */
    static ClientEvents instance;

    private GridRecipeWidget widget;
    private int lastMouseX;
    private int lastMouseY;

    public static void register() {
        instance = new ClientEvents();
        MinecraftForge.EVENT_BUS.register(instance);
    }

    GridRecipeWidget widget() {
        return widget;
    }

    @SubscribeEvent
    public void onInit(GuiScreenEvent.InitGuiEvent.Post event) {
        GuiScreen gui = event.getGui();
        if (!(gui instanceof GuiGrid)) {
            widget = null;
            return;
        }
        GuiGrid screen = (GuiGrid) gui;
        IGrid grid = ((ContainerGrid) screen.inventorySlots).getGrid();
        if (grid.getGridType() != GridType.CRAFTING && grid.getGridType() != GridType.PATTERN
                || !(grid instanceof NetworkNodeGrid)) {
            widget = null;
            return;
        }
        if (widget == null || widget.screen != screen) {
            widget = new GridRecipeWidget(screen, (NetworkNodeGrid) grid);
        }
        widget.attachButton();
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && widget != null) {
            if (Minecraft.getMinecraft().currentScreen != widget.screen) {
                widget = null;
            } else {
                widget.tick();
            }
        }
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (widget != null && event.getGui() == widget.screen) {
            lastMouseX = event.getMouseX();
            lastMouseY = event.getMouseY();
            // Refined Storage re-inits its screen on some synced settings, with no Forge event.
            if (!widget.button.consumeDrawn()) {
                widget.attachButton();
            }
            widget.renderOverlays(lastMouseX, lastMouseY);
        }
    }

    /**
     * This Forge has no tooltip event. A screen only shows the tooltip of what lies under the cursor it
     * is given, so while an overlay covers the cursor the screen is drawn with the cursor far away.
     * The event's Post half still fires after a cancelled Pre, and draws the overlay.
     */
    @SubscribeEvent
    public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (widget != null && event.getGui() == widget.screen
                && widget.hidesHover(event.getMouseX(), event.getMouseY())) {
            event.setCanceled(true);
            widget.screen.drawScreen(CURSOR_AWAY, CURSOR_AWAY, event.getRenderPartialTicks());
        }
    }

    @SubscribeEvent
    public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (widget == null || event.getGui() != widget.screen || Mouse.getEventButton() != 0) {
            return;
        }
        if (Mouse.getEventButtonState()) {
            GuiScreen gui = event.getGui();
            Minecraft mc = gui.mc;
            int x = Mouse.getEventX() * gui.width / mc.displayWidth;
            int y = gui.height - Mouse.getEventY() * gui.height / mc.displayHeight - 1;
            event.setCanceled(widget.mouseClicked(x, y));
        } else {
            event.setCanceled(widget.consumeRelease());
        }
    }

    @SubscribeEvent
    public void onKey(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (widget != null && event.getGui() == widget.screen && Keyboard.getEventKeyState()) {
            event.setCanceled(widget.keyPressed(Keyboard.getEventKey()));
        }
    }
}
