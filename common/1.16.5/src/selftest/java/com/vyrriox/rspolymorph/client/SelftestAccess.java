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

import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * What the selftest reads of the widget's package-private classes. Lives in their package but only in
 * the selftest source set, so a released jar has no such door.
 *
 * Author: vyrriox
 */
public final class SelftestAccess {

    /** One icon of the open popup and the point of the screen a player clicks to pick it. */
    public static final class PopupEntry {

        private final ResourceLocation id;
        private final ItemStack output;
        private final int clickX;
        private final int clickY;

        PopupEntry(ResourceLocation id, ItemStack output, int clickX, int clickY) {
            this.id = id;
            this.output = output;
            this.clickX = clickX;
            this.clickY = clickY;
        }

        public ResourceLocation id() {
            return id;
        }

        public ItemStack output() {
            return output;
        }

        public int clickX() {
            return clickX;
        }

        public int clickY() {
            return clickY;
        }
    }

    private SelftestAccess() {
    }

    public static boolean isPopupOpen(GridRecipeWidget widget) {
        return widget.isPopupOpen();
    }

    public static boolean isShowingTutorial(GridRecipeWidget widget) {
        return (boolean) read(GridRecipeWidget.class, widget, "showTutorial");
    }

    public static List<PopupEntry> popupEntries(GridRecipeWidget widget) {
        RecipeSelectorPopup popup = (RecipeSelectorPopup) read(GridRecipeWidget.class, widget, "popup");
        int size = (int) read(RecipeSelectorPopup.class, null, "SLOT");
        int cols = (int) read(RecipeSelectorPopup.class, null, "COLS");
        int padding = (int) read(RecipeSelectorPopup.class, null, "PADDING");
        int header = (int) read(RecipeSelectorPopup.class, null, "HEADER");
        int x = (int) read(RecipeSelectorPopup.class, popup, "x");
        int y = (int) read(RecipeSelectorPopup.class, popup, "y");
        List<PopupEntry> found = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<RecipeSelectorPopup.Entry> entries = (List<RecipeSelectorPopup.Entry>) read(RecipeSelectorPopup.class, popup, "entries");
        for (int i = 0; i < entries.size(); i++) {
            RecipeSelectorPopup.Entry entry = entries.get(i);
            found.add(new PopupEntry(entry.id(), entry.output(),
                    x + padding + i % cols * size + size / 2,
                    y + padding + header + i / cols * size + size / 2));
        }
        return found;
    }

    /** Forgets the session cache, so only the state file says the card was seen. */
    public static void forgetTutorialCache() {
        write(TutorialState.class, "seenCache", null);
    }

    /** Also allows the state file to be written again, after a test deleted it. */
    public static void forgetTutorialSession() {
        forgetTutorialCache();
        write(TutorialState.class, "persistedThisSession", false);
    }

    private static Object read(Class<?> cls, Object owner, String name) {
        try {
            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read " + cls.getSimpleName() + "." + name, e);
        }
    }

    private static void write(Class<?> cls, String name, Object value) {
        try {
            Field field = cls.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot write " + cls.getSimpleName() + "." + name, e);
        }
    }
}
