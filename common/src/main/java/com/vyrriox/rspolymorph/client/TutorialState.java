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
 * Tracks whether the player has already seen the one-time recipe-selection tutorial overlay.
 *
 * Purely client-side and cosmetic: persisted as an empty marker file under the game's config dir so
 * the overlay shows exactly once per installation. Every disk operation is wrapped fail-soft — a
 * read/write error degrades to "show once per session", never to a crash. Contains no
 * version-remapped identifiers, so it is shared as-is by both the 1.21.1 and 26.x builds.
 *
 * The in-memory cache assumes one game directory per JVM, which always holds for a Minecraft client
 * ({@code Minecraft} is a singleton and its {@code gameDirectory} is fixed for the process).
 *
 * Author: vyrriox
 */
public final class TutorialState {

    private static final String FLAG_FILE = "rspolymorph_tutorial.flag";

    /** Lazily cached so {@link #hasSeen()} hits the disk at most once per session. */
    private static Boolean seenCache = null;
    /** Guards a single disk write per session even if the overlay renders many frames. */
    private static boolean persistedThisSession = false;

    private TutorialState() {
    }

    /** @return true if the tutorial marker exists (overlay already shown on a previous run). */
    public static boolean hasSeen() {
        if (seenCache != null) {
            return seenCache;
        }
        boolean seen = false;
        try {
            Path p = flagPath();
            seen = p != null && Files.exists(p);
        } catch (Throwable ignored) {
            // Treat any I/O/security failure as "not seen yet" — worst case the overlay reappears.
        }
        seenCache = seen;
        return seen;
    }

    /**
     * Marks the tutorial as seen (in memory immediately + on disk once, asynchronously). Idempotent.
     * The marker write runs on the common ForkJoinPool (loader/version-agnostic) so the render thread
     * is never blocked by a filesystem syscall (slow drive, network home, AV scanner).
     */
    public static void markSeen() {
        seenCache = Boolean.TRUE;
        if (persistedThisSession) {
            return;
        }
        persistedThisSession = true;
        CompletableFuture.runAsync(() -> {
            try {
                Path p = flagPath();
                if (p != null) {
                    Files.createDirectories(p.getParent());
                    Files.write(p, "1".getBytes(StandardCharsets.UTF_8));
                }
            } catch (Throwable ignored) {
                // Persisting failed — the overlay simply shows again next session. Not fatal.
            }
        });
    }

    private static Path flagPath() {
        try {
            File dir = Minecraft.getInstance().gameDirectory;
            if (dir == null) {
                return null;
            }
            return dir.toPath().resolve("config").resolve(FLAG_FILE);
        } catch (Throwable t) {
            return null;
        }
    }
}
