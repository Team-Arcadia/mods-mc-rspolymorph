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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A self-contained recipe-selection popup drawn over an RS grid screen, replacing Polymorph's
 * {@code SelectionWidget}. It renders a vertical grid of candidate recipe outputs as item icons;
 * clicking one fires the selection callback with that recipe's id.
 *
 * Pure vanilla GUI — no Polymorph, no loader API. Owned and driven by {@link RsGridRecipeWidget};
 * rendered and click-routed by {@code MixinAbstractGridScreenRender}.
 *
 * Author: vyrriox
 */
public final class RecipeSelectorPopup {

    /** One selectable recipe: its id and the output stack to show as the icon. */
    public record Entry(ResourceLocation id, ItemStack output) {}

    private static final int SLOT = 18;          // icon cell size
    private static final int COLS = 5;           // icons per row
    private static final int PADDING = 4;
    private static final int HEADER = 13;         // title-bar height inside the box

    // Vanilla-tooltip palette so the popup reads as a native MC element.
    private static final int BG = 0xF0100010;          // tooltip background
    private static final int BORDER_TOP = 0x505000FF;  // tooltip border gradient (top)
    private static final int BORDER_BOT = 0x5028007F;  // tooltip border gradient (bottom)
    private static final int TITLE = 0xFFFFE08A;       // soft gold title text
    private static final int SEP = 0x60FFFFFF;         // header separator line
    // Inset slot bevel.
    private static final int SLOT_BG = 0xFF22222F;
    private static final int SLOT_DARK = 0xFF101019;
    private static final int SLOT_LITE = 0xFF44445F;
    private static final int HOVER = 0x80FFFFFF;        // vanilla slot-hover overlay
    private static final int SELECTED = 0xFFFFD24A;     // active-recipe frame (gold)

    private final List<Entry> entries = new ArrayList<>();
    private final Consumer<ResourceLocation> onSelect;

    private boolean open = false;
    private int x, y;          // top-left of the popup, in screen space
    private int hovered = -1;

    public RecipeSelectorPopup(Consumer<ResourceLocation> onSelect) {
        this.onSelect = onSelect;
    }

    public boolean isOpen() {
        return open;
    }

    private int rows() {
        return Math.max(1, (entries.size() + COLS - 1) / COLS);
    }

    private int boxW() {
        return COLS * SLOT + PADDING * 2;
    }

    private int boxH() {
        return PADDING + HEADER + rows() * SLOT + PADDING;
    }

    /** Opens the popup anchored with its bottom-right near (anchorX, anchorY) — e.g. the result slot. */
    public void open(List<Entry> newEntries, int anchorX, int anchorY) {
        entries.clear();
        entries.addAll(newEntries);
        int w = boxW();
        int h = boxH();
        // Place the popup to the left of and above the anchor so it doesn't cover the result.
        int px = anchorX - w - 4;
        int py = anchorY - h + SLOT;
        // Clamp fully on-screen so the box is never clipped by a window edge.
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        px = Math.max(2, Math.min(px, screenW - w - 2));
        py = Math.max(2, Math.min(py, screenH - h - 2));
        this.x = px;
        this.y = py;
        this.open = true;
        this.hovered = -1;
    }

    public void close() {
        open = false;
        entries.clear();
        hovered = -1;
    }

    /** Renders the popup. Call from the screen's render RETURN so it draws on top. */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, ItemStack current) {
        if (!open || entries.isEmpty()) return;

        Font font = Minecraft.getInstance().font;
        int w = boxW();
        int h = boxH();

        // ── Chrome: vanilla-tooltip background + gradient border ──────────────
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.fillGradient(x, y + 1, x + 1, y + h - 1, BORDER_TOP, BORDER_BOT);             // left
        graphics.fillGradient(x + w - 1, y + 1, x + w, y + h - 1, BORDER_TOP, BORDER_BOT);     // right
        graphics.fill(x, y, x + w, y + 1, BORDER_TOP);                                         // top
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER_BOT);                                 // bottom

        // ── Header: centred title + separator ─────────────────────────────────
        Component header = title();
        graphics.drawString(font, header,
                x + (w - font.width(header)) / 2, y + PADDING - 1, TITLE, false);
        int sepY = y + PADDING + HEADER - 4;
        graphics.fill(x + PADDING, sepY, x + w - PADDING, sepY + 1, SEP);

        int gridX = x + PADDING;
        int gridY = y + PADDING + HEADER;
        boolean hasCurrent = current != null && !current.isEmpty();

        hovered = -1;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridX + col * SLOT;
            int cy = gridY + row * SLOT;

            // Inset slot bevel (top/left dark, bottom/right light) so icons sit on a real frame.
            graphics.fill(cx, cy, cx + SLOT, cy + SLOT, SLOT_BG);
            graphics.fill(cx, cy, cx + SLOT, cy + 1, SLOT_DARK);
            graphics.fill(cx, cy, cx + 1, cy + SLOT, SLOT_DARK);
            graphics.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, SLOT_LITE);
            graphics.fill(cx + SLOT - 1, cy, cx + SLOT, cy + SLOT, SLOT_LITE);

            ItemStack out = entries.get(i).output();
            // Gold frame on the recipe whose output matches the grid's current result.
            if (hasCurrent && ItemStack.isSameItemSameComponents(out, current)) {
                graphics.fill(cx, cy, cx + SLOT, cy + 1, SELECTED);
                graphics.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, SELECTED);
                graphics.fill(cx, cy, cx + 1, cy + SLOT, SELECTED);
                graphics.fill(cx + SLOT - 1, cy, cx + SLOT, cy + SLOT, SELECTED);
            }

            boolean over = mouseX >= cx && mouseX < cx + SLOT && mouseY >= cy && mouseY < cy + SLOT;
            if (over) {
                hovered = i;
                graphics.fill(cx + 1, cy + 1, cx + SLOT - 1, cy + SLOT - 1, HOVER);
            }
            graphics.renderItem(out, cx + 1, cy + 1);
            graphics.renderItemDecorations(font, out, cx + 1, cy + 1);
        }

        // Hovered recipe name as a native vanilla tooltip at the cursor. The screen's own slot
        // tooltip is suppressed by MixinScreenTooltipSuppress while the cursor is over the popup,
        // so this is the only tooltip shown — no double-text overlap.
        if (hovered >= 0) {
            graphics.renderTooltip(font, entries.get(hovered).output(), mouseX, mouseY);
        }
    }

    /** True if the cursor is within the popup box — used to suppress the screen's slot tooltip. */
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!open || entries.isEmpty()) return false;
        return mouseX >= x && mouseX < x + boxW() && mouseY >= y && mouseY < y + boxH();
    }

    /**
     * Handles a click. Returns true if the click was consumed by the popup (either selecting an
     * entry or clicking inside the popup background) so the screen does not pass it to slots.
     */
    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!open || entries.isEmpty()) return false;

        if (!isMouseOver(mouseX, mouseY)) {
            // Click outside closes the popup but is not consumed (let the screen handle it).
            close();
            return false;
        }

        int gridX = x + PADDING;
        int gridY = y + PADDING + HEADER;
        for (int i = 0; i < entries.size(); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int cx = gridX + col * SLOT;
            int cy = gridY + row * SLOT;
            if (mouseX >= cx && mouseX < cx + SLOT && mouseY >= cy && mouseY < cy + SLOT) {
                ResourceLocation id = entries.get(i).id();
                close();
                onSelect.accept(id);
                return true;
            }
        }
        // Clicked the popup background (not an icon) — consume so slots don't react.
        return true;
    }

    /** Hint title for accessibility / future use. */
    public static Component title() {
        return Component.translatable("rspolymorph.gui.recipe_selection");
    }

    @SuppressWarnings("unused")
    private static int fontHeight() {
        Font f = Minecraft.getInstance().font;
        return f.lineHeight;
    }
}
