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

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The selftest's view of the package-private client classes. Client thread only. Clicks go the way
 * {@code ClientEvents.onMouse} sends them: first to the widget, then to the screen if not consumed.
 *
 * Author: vyrriox
 */
@net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
public final class SelftestAccess {

    private SelftestAccess() {
    }

    private static GridRecipeWidget widget() {
        return ClientEvents.instance == null ? null : ClientEvents.instance.widget();
    }

    public static boolean hasWidget() {
        return widget() != null;
    }

    public static boolean buttonVisible() {
        GridRecipeWidget w = widget();
        return w != null && w.button != null && w.button.visible;
    }

    public static boolean popupOpen() {
        GridRecipeWidget w = widget();
        return w != null && w.isPopupOpen();
    }

    public static boolean tutorialShowing() {
        GridRecipeWidget w = widget();
        return w != null && w.showTutorial;
    }

    public static int scans() {
        return widget().scans;
    }

    public static int candidates() {
        GridRecipeWidget w = widget();
        return w == null ? -1 : w.candidates().size();
    }

    public static List<ResourceLocation> popupIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (RecipeSelectorPopup.Entry entry : widget().popup.entries()) {
            ids.add(entry.id);
        }
        return ids;
    }

    public static List<ItemStack> popupOutputs() {
        List<ItemStack> outputs = new ArrayList<>();
        for (RecipeSelectorPopup.Entry entry : widget().popup.entries()) {
            outputs.add(entry.output);
        }
        return outputs;
    }

    public static void clickButton(GuiScreen screen) throws Exception {
        GridRecipeWidget w = widget();
        click(screen, w.button.x + 2, w.button.y + 2);
    }

    public static void clickPopupCell(GuiScreen screen, int index) throws Exception {
        GridRecipeWidget w = widget();
        click(screen, w.popup.cellX(index) + 2, w.popup.cellY(index) + 2);
    }

    public static void click(GuiScreen screen, int x, int y) throws Exception {
        GridRecipeWidget w = widget();
        boolean consumed = w != null && w.mouseClicked(x, y);
        if (!consumed) {
            Method press = mouseMethod("mouseClicked", "func_73864_a");
            press.setAccessible(true);
            press.invoke(screen, x, y, 0);
        }
        boolean swallowed = w != null && w.consumeRelease();
        if (!swallowed) {
            Method release = mouseMethod("mouseReleased", "func_146286_b");
            release.setAccessible(true);
            release.invoke(screen, x, y, 0);
        }
    }

    /** @return true if the widget took the key */
    public static boolean key(int key) {
        GridRecipeWidget w = widget();
        return w != null && w.keyPressed(key);
    }

    public static boolean hasOurSideButton(GuiScreen screen) throws Exception {
        Field field;
        try {
            field = GuiScreen.class.getDeclaredField("buttonList");
        } catch (NoSuchFieldException reobfuscated) {
            field = GuiScreen.class.getDeclaredField("field_146292_n");
        }
        field.setAccessible(true);
        for (Object button : (List<?>) field.get(screen)) {
            if (button instanceof RecipeSelectionSideButton && ((GuiButton) button).visible) {
                return true;
            }
        }
        return false;
    }

    private static Method mouseMethod(String mcp, String srg) throws NoSuchMethodException {
        try {
            return GuiScreen.class.getDeclaredMethod(mcp, int.class, int.class, int.class);
        } catch (NoSuchMethodException reobfuscated) {
            return GuiScreen.class.getDeclaredMethod(srg, int.class, int.class, int.class);
        }
    }

    public static void resetTutorialState() {
        TutorialState.seenCache = null;
        TutorialState.persistedThisSession = false;
    }
}
