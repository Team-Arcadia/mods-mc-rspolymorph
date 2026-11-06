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
package com.vyrriox.rspolymorph.selftest;

import com.vyrriox.rspolymorph.client.SelftestAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * This Forge has no tooltip event, so a covered screen is drawn with the cursor away: over the popup
 * the Pre event of its drawing is taken, elsewhere it is not. Client thread, with the popup open.
 *
 * Author: vyrriox
 */
public final class SelftestHover {

    private SelftestHover() {
    }

    /** @return what is wrong, or null */
    public static String problem() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (!post(screen, SelftestAccess.popupCellX(0) + 2, SelftestAccess.popupCellY(0) + 2)) {
            return "the screen is drawn with the real cursor under the popup";
        }
        if (post(screen, screen.width - 1, 1)) {
            return "the screen lost its cursor away from the popup";
        }
        return null;
    }

    private static boolean post(GuiScreen screen, int x, int y) {
        return MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.DrawScreenEvent.Pre(screen, x, y, 0.0f));
    }
}
