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

import com.vyrriox.rspolymorph.RecipeAccess;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The popup listing the matching recipes, each shown by its output.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
final class RecipeSelectorPopup {

    /** One selectable recipe: its id and the output stack shown as the icon. */
    @SideOnly(Side.CLIENT)
    static final class Entry {
        final ResourceLocation id;
        final ItemStack output;

        Entry(ResourceLocation id, ItemStack output) {
            this.id = id;
            this.output = output;
        }
    }

    private static final int SLOT = 18;
    private static final int COLS = 5;
    private static final int PADDING = 4;
    private static final int HEADER = 13;

    private static final int BG = 0xF0100010;
    private static final int BORDER_TOP = 0x505000FF;
    private static final int BORDER_BOT = 0x5028007F;
    private static final int TITLE_COLOR = 0xFFFFE08A;
    private static final int SEP = 0x60FFFFFF;
    private static final int SLOT_BG = 0xFF22222F;
    private static final int SLOT_DARK = 0xFF101019;
    private static final int SLOT_LITE = 0xFF44445F;
    private static final int HOVER = 0x80FFFFFF;
    private static final int SELECTED = 0xFFFFD24A;

    private final Consumer<ResourceLocation> onSelect;

    private List<Entry> entries = Collections.emptyList();
    private String title = "";
    private boolean open;
    private int x;
    private int y;

    RecipeSelectorPopup(Consumer<ResourceLocation> onSelect) {
        this.onSelect = onSelect;
    }

    boolean isOpen() {
        return open;
    }

    List<Entry> entries() {
        return entries;
    }

    private int boxW() {
        return COLS * SLOT + PADDING * 2;
    }

    private int boxH() {
        int rows = Math.max(1, (entries.size() + COLS - 1) / COLS);
        return PADDING + HEADER + rows * SLOT + PADDING;
    }

    /** Opens to the left of and above the anchor (the result slot), clamped on screen. */
    void open(List<Entry> newEntries, int anchorX, int anchorY, int screenW, int screenH) {
        entries = newEntries;
        title = Canvas.translate("rspolymorph.gui.recipe_selection");
        x = Math.max(2, Math.min(anchorX - boxW() - 4, screenW - boxW() - 2));
        y = Math.max(2, Math.min(anchorY - boxH() + SLOT, screenH - boxH() - 2));
        open = true;
    }

    void close() {
        open = false;
        entries = Collections.emptyList();
    }

    void render(Canvas canvas, GuiScreen screen, int mouseX, int mouseY, ItemStack current) {
        if (!open) {
            return;
        }
        int w = boxW();
        int h = boxH();

        canvas.fill(x, y, x + w, y + h, BG);
        canvas.gradient(x, y + 1, x + 1, y + h - 1, BORDER_TOP, BORDER_BOT);
        canvas.gradient(x + w - 1, y + 1, x + w, y + h - 1, BORDER_TOP, BORDER_BOT);
        canvas.fill(x, y, x + w, y + 1, BORDER_TOP);
        canvas.fill(x, y + h - 1, x + w, y + h, BORDER_BOT);

        canvas.text(title, x + (w - Canvas.textWidth(title)) / 2, y + PADDING - 1, TITLE_COLOR, false);
        int sepY = y + PADDING + HEADER - 4;
        canvas.fill(x + PADDING, sepY, x + w - PADDING, sepY + 1, SEP);

        int hovered = -1;
        for (int i = 0; i < entries.size(); i++) {
            int cx = cellX(i);
            int cy = cellY(i);

            canvas.fill(cx, cy, cx + SLOT, cy + SLOT, SLOT_BG);
            canvas.fill(cx, cy, cx + SLOT, cy + 1, SLOT_DARK);
            canvas.fill(cx, cy, cx + 1, cy + SLOT, SLOT_DARK);
            canvas.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, SLOT_LITE);
            canvas.fill(cx + SLOT - 1, cy, cx + SLOT, cy + SLOT, SLOT_LITE);

            ItemStack output = entries.get(i).output;
            if (RecipeAccess.sameStack(output, current)) {
                canvas.fill(cx, cy, cx + SLOT, cy + 1, SELECTED);
                canvas.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, SELECTED);
                canvas.fill(cx, cy, cx + 1, cy + SLOT, SELECTED);
                canvas.fill(cx + SLOT - 1, cy, cx + SLOT, cy + SLOT, SELECTED);
            }
            if (inCell(i, mouseX, mouseY)) {
                hovered = i;
                canvas.fill(cx + 1, cy + 1, cx + SLOT - 1, cy + SLOT - 1, HOVER);
            }
            canvas.item(output, cx + 1, cy + 1);
        }

        if (hovered >= 0) {
            canvas.itemTooltip(screen, entries.get(hovered).output, mouseX, mouseY);
        }
    }

    boolean isMouseOver(int mouseX, int mouseY) {
        return open && mouseX >= x && mouseX < x + boxW() && mouseY >= y && mouseY < y + boxH();
    }

    /** @return true if the click was inside the popup; a click outside closes it and passes through */
    boolean mouseClicked(int mouseX, int mouseY) {
        if (!open) {
            return false;
        }
        if (!isMouseOver(mouseX, mouseY)) {
            close();
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            if (inCell(i, mouseX, mouseY)) {
                ResourceLocation id = entries.get(i).id;
                close();
                onSelect.accept(id);
                break;
            }
        }
        return true;
    }

    int cellX(int index) {
        return x + PADDING + index % COLS * SLOT;
    }

    int cellY(int index) {
        return y + PADDING + HEADER + index / COLS * SLOT;
    }

    private boolean inCell(int index, int mouseX, int mouseY) {
        int cx = cellX(index);
        int cy = cellY(index);
        return mouseX >= cx && mouseX < cx + SLOT && mouseY >= cy && mouseY < cy + SLOT;
    }
}
