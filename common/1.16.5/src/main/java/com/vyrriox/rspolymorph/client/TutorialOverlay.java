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

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Arrays;
import java.util.List;

/**
 * One-time card shown the first time a crafting or pattern grid opens, dismissed by a click or by
 * Space or Enter. {@link TutorialState} decides whether it appears.
 *
 * Author: vyrriox
 */
final class TutorialOverlay {

    private static final int PAD = 8;
    private static final int LINE_GAP = 2;
    private static final int BG = 0xF0100010;
    private static final int BORDER_TOP = 0x505000FF;
    private static final int BORDER_BOT = 0x5028007F;
    private static final int SCRIM = 0x80000000;
    private static final int TITLE_COLOR = 0xFFFFE08A;
    private static final int TEXT_COLOR = 0xFFE6E6E6;
    private static final int HINT_COLOR = 0xFFA8A8FF;
    private static final int SEP = 0x60FFFFFF;

    private static final ITextComponent TITLE = new TranslationTextComponent("rspolymorph.tutorial.title");
    private static final ITextComponent HINT = new TranslationTextComponent("rspolymorph.tutorial.dismiss");
    private static final List<ITextComponent> BODY = Arrays.asList(
            new TranslationTextComponent("rspolymorph.tutorial.line1"),
            new TranslationTextComponent("rspolymorph.tutorial.line2"),
            new TranslationTextComponent("rspolymorph.tutorial.line3")
    );

    private TutorialOverlay() {
    }

    /** The whole text as one sentence, for the narrator. */
    static ITextComponent narration() {
        return TITLE.copy().append(". ").append(BODY.get(0)).append(" ").append(BODY.get(1)).append(" ").append(HINT);
    }

    static void render(Canvas canvas) {
        int textW = Math.max(Canvas.textWidth(TITLE), Canvas.textWidth(HINT));
        for (int i = 0; i < BODY.size(); i++) {
            textW = Math.max(textW, Canvas.textWidth(BODY.get(i)));
        }
        int lineH = Canvas.lineHeight() + LINE_GAP;
        int sw = Canvas.screenWidth();
        int sh = Canvas.screenHeight();
        int w = Math.min(textW + PAD * 2, Math.max(PAD * 4, sw - PAD * 4));
        int h = PAD * 2 + lineH + 4 + BODY.size() * lineH + 4 + lineH;
        int x = (sw - w) / 2;
        int y = (sh - h) / 2;

        canvas.fill(0, 0, sw, sh, SCRIM);
        canvas.fill(x, y, x + w, y + h, BG);
        canvas.gradient(x, y + 1, x + 1, y + h - 1, BORDER_TOP, BORDER_BOT);
        canvas.gradient(x + w - 1, y + 1, x + w, y + h - 1, BORDER_TOP, BORDER_BOT);
        canvas.fill(x, y, x + w, y + 1, BORDER_TOP);
        canvas.fill(x, y + h - 1, x + w, y + h, BORDER_BOT);

        int ty = y + PAD;
        canvas.text(TITLE, x + (w - Canvas.textWidth(TITLE)) / 2, ty, TITLE_COLOR, false);
        ty += lineH;
        canvas.fill(x + PAD, ty + 1, x + w - PAD, ty + 2, SEP);
        ty += 4;
        for (int i = 0; i < BODY.size(); i++) {
            canvas.text(BODY.get(i), x + PAD, ty, TEXT_COLOR, false);
            ty += lineH;
        }
        ty += 4;
        canvas.text(HINT, x + (w - Canvas.textWidth(HINT)) / 2, ty, HINT_COLOR, false);
    }
}
