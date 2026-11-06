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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Tracks whether the player has already seen the tutorial card, with a marker file in the game's
 * config folder (the same file as on the other Minecraft versions). Purely client-side and cosmetic:
 * a read or write error degrades to "show it once per session", never to a crash.
 *
 * Author: vyrriox
 */
final class TutorialState {

    private static final String FLAG_FILE = "rspolymorph_tutorial.flag";

    /** Hits the disk at most once per session. */
    private static Boolean seenCache = null;
    private static boolean persistedThisSession = false;

    private TutorialState() {
    }

    static boolean hasSeen() {
        if (seenCache == null) {
            boolean seen = false;
            try {
                Path path = flagPath();
                seen = path != null && Files.exists(path);
            } catch (RuntimeException ignored) {
                // Treated as not seen: at worst the card shows again.
            }
            seenCache = seen;
        }
        return seenCache;
    }

    /** Marks the card as seen, in memory now and on disk once, off the render thread. */
    static void markSeen() {
        seenCache = Boolean.TRUE;
        if (persistedThisSession) {
            return;
        }
        persistedThisSession = true;
        CompletableFuture.runAsync(() -> {
            try {
                Path path = flagPath();
                if (path != null) {
                    Files.createDirectories(path.getParent());
                    Files.write(path, "1".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {
                // Not fatal: the card simply shows again next session.
            }
        });
    }

    private static Path flagPath() {
        File dir = Minecraft.getInstance().gameDirectory;
        return dir == null ? null : dir.toPath().resolve("config").resolve(FLAG_FILE);
    }
}
