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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A one-time, centred tutorial card shown the first time a player opens an RS grid screen. It
 * explains the recipe-selection feature (the side button + popup) and is dismissed by a click or by
 * pressing Space/Enter. Pure vanilla GUI — drawn over the screen by {@link RsGridRecipeWidget}; no
 * Polymorph, no loader API. {@link TutorialState} decides whether it should appear.
 *
 * MC 26.1.2 fork: two-phase extract API ({@link GuiGraphicsExtractor}, {@code text}), a stratum bump
 * to composite above the screen, and flat-fill borders (no fillGradient dependency). Border colours
 * mirror the 1.21.1 fork's blue-purple hue at full alpha so the card looks the same on both lines.
 *
 * Author: vyrriox
 */
public final class TutorialOverlay {

    private static final int PAD = 8;
    private static final int LINE_GAP = 2;
    private static final int BG = 0xF0100010;          // vanilla tooltip background
    private static final int BORDER_TOP = 0xFF5050FF;  // matches 1.21.1 0x505000FF hue, opaque
    private static final int BORDER_BOT = 0xFF28007F;  // matches 1.21.1 0x5028007F hue, opaque
    private static final int SCRIM = 0x80000000;        // dim the screen behind the card
    private static final int TITLE = 0xFFFFE08A;        // soft gold
    private static final int TEXT = 0xFFE6E6E6;
    private static final int HINT = 0xFFA8A8FF;
    private static final int SEP = 0x60FFFFFF;

    // Invariant content — hoisted so render() allocates nothing per frame.
    private static final Component C_TITLE = Component.translatable("rspolymorph.tutorial.title");
    private static final Component C_LINE1 = Component.translatable("rspolymorph.tutorial.line1");
    private static final Component C_LINE2 = Component.translatable("rspolymorph.tutorial.line2");
    private static final Component C_LINE3 = Component.translatable("rspolymorph.tutorial.line3");
    private static final Component C_HINT = Component.translatable("rspolymorph.tutorial.dismiss");
    private static final List<Component> BODY = List.of(C_LINE1, C_LINE2, C_LINE3);

    // Layout cache (text metrics depend only on the static strings + font); computed once.
    private static int cachedTextW = -1;
    private static int cachedLineH = -1;

    private TutorialOverlay() {
    }

    private static void ensureLayout(Font font) {
        if (cachedTextW >= 0) return;
        int w = font.width(C_TITLE);
        for (Component l : BODY) {
            w = Math.max(w, font.width(l));
        }
        cachedTextW = Math.max(w, font.width(C_HINT));
        cachedLineH = font.lineHeight + LINE_GAP;
    }

    /** Draws the tutorial card centred on screen. Call from the screen render RETURN (drawn on top). */
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        ensureLayout(font);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        int lineH = cachedLineH;
        // Clamp so the card never bleeds off a small-scale / narrow GUI.
        int w = Math.min(cachedTextW + PAD * 2, Math.max(PAD * 4, sw - PAD * 4));
        int h = PAD * 2 + lineH + 4 + BODY.size() * lineH + 4 + lineH;
        int x = (sw - w) / 2;
        int y = (sh - h) / 2;

        // Composite above the screen content.
        graphics.nextStratum();

        // Dim the rest of the screen so the card is the clear focus.
        graphics.fill(0, 0, sw, sh, SCRIM);

        // Card chrome: vanilla-tooltip background + beveled border.
        graphics.fill(x, y, x + w, y + h, BG);
        graphics.fill(x, y, x + w, y + 1, BORDER_TOP);
        graphics.fill(x, y, x + 1, y + h, BORDER_TOP);
        graphics.fill(x, y + h - 1, x + w, y + h, BORDER_BOT);
        graphics.fill(x + w - 1, y, x + w, y + h, BORDER_BOT);

        int ty = y + PAD;
        graphics.text(font, C_TITLE, x + (w - font.width(C_TITLE)) / 2, ty, TITLE);
        ty += lineH;
        graphics.fill(x + PAD, ty + 1, x + w - PAD, ty + 2, SEP);
        ty += 4;
        for (Component l : BODY) {
            graphics.text(font, l, x + PAD, ty, TEXT);
            ty += lineH;
        }
        ty += 4;
        graphics.text(font, C_HINT, x + (w - font.width(C_HINT)) / 2, ty, HINT);
    }
}
