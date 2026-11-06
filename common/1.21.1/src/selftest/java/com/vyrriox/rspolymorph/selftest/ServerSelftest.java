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

import com.vyrriox.rspolymorph.RsPolymorph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proof on the dedicated server distribution, run by the game test launch (which needs no EULA and
 * leaves eula.txt alone). It starts from {@code MixinGameTestServerSelftest} once the mod is loaded,
 * and never goes further than that: no world, no game test.
 *
 * It holds no reference to a client type, and proves that the mod's server side has none: the client
 * classes must not load here, and the class files reachable from the server entry points must not name
 * one. Prints the same RSP-SELFTEST lines as the client run. Trust the DONE line, not the exit code: a
 * launch that stops early prints nothing.
 *
 * Author: vyrriox
 */
public final class ServerSelftest {

    private static final Logger LOG = LogManager.getLogger("rspolymorph-selftest");
    private static final boolean ENABLED = Boolean.getBoolean("rspolymorph.selftest.server");
    private static final Path REPORT = Path.of("rspolymorph-selftest-server.txt");
    /** The JVM's own list of loaded classes (-Xlog:class+load, set by the run). */
    private static final Path CLASS_LOG = Path.of("rspolymorph-selftest-classload.log");
    /** NeoForge's 26.x development jar does not strip client classes, so only the JVM's list can tell. */
    private static final boolean JOINED_JAR = Boolean.getBoolean("rspolymorph.selftest.joinedJar");
    private static final String COMMON_CONFIG = "rspolymorph-common.mixins.json";
    private static final ClassLoader LOADER = ServerSelftest.class.getClassLoader();

    /** Client classes of the game: unloadable on this distribution, or this proof proves nothing. */
    private static final List<String> CLIENT_CLASSES = List.of(
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.gui.screens.Screen",
            "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");

    /** What the mod's server side may never name. */
    private static final List<String> FORBIDDEN = List.of(
            "net/minecraft/client/", "com/mojang/blaze3d/", "com/vyrriox/rspolymorph/client/",
            "com/refinedmods/refinedstorage/common/grid/screen/");

    /**
     * Client APIs of the loaders. Allowed only in the network platform: its client-to-server send is
     * only ever called from client code, so the JVM never links that reference on a server.
     */
    private static final List<String> LOADER_CLIENT_API = List.of(
            "net/neoforged/neoforge/client/", "net/fabricmc/fabric/api/client/");

    /** Everything a server loads of this mod starts from these, and the common mixins. */
    private static final List<String> ROOTS = List.of(
            "com.vyrriox.rspolymorph.RsPolymorph",
            "com.vyrriox.rspolymorph.IRsRecipeMatrix",
            "com.vyrriox.rspolymorph.IRsMatrixContainer",
            "com.vyrriox.rspolymorph.network.SelectRecipePacket",
            "com.vyrriox.rspolymorph.platform.Services",
            "com.vyrriox.rspolymorph.neoforge.RsPolymorphNeoForge",
            "com.vyrriox.rspolymorph.neoforge.NeoForgeNetworkPlatform",
            "com.vyrriox.rspolymorph.neoforge.NeoForgeGridRecipeStore",
            "com.vyrriox.rspolymorph.fabric.RsPolymorphFabric",
            "com.vyrriox.rspolymorph.fabric.FabricNetworkPlatform",
            "com.vyrriox.rspolymorph.fabric.FabricGridRecipeStore");

    private static final Pattern OWN_CLASS = Pattern.compile("com/vyrriox/rspolymorph/[A-Za-z0-9_/$]+");

    private static boolean started;
    private static int passed;
    private static int failed;

    private ServerSelftest() {
    }

    /** Called when the game test server is about to be created: the mod and its mixins are loaded. */
    public static void onGameTestServerCreate() {
        if (!ENABLED || started) return;
        started = true;
        try {
            Files.deleteIfExists(REPORT);
            run();
        } catch (Throwable t) {
            fail("scenario", t.toString());
        }
        line("DONE pass=" + passed + " fail=" + failed);
        LogManager.shutdown();
        // No world exists and none is wanted. Nothing to save, so nothing to wait for.
        Runtime.getRuntime().halt(failed == 0 ? 0 : 1);
    }

    private static void run() throws Exception {
        check("server.environment_is_server", () -> {
            MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();
            return side == MixinEnvironment.Side.SERVER ? null : "the mixin side is " + side;
        });
        check("server.mod_loaded_with_component", () -> {
            RsPolymorph.selectedRecipeComponent();
            return null;
        });
        check("server.audit_no_failure_in_common_config", () -> SelftestMixins.audit().problem());
        for (String mixin : SelftestMixins.mixinsOf(COMMON_CONFIG, "mixins")) {
            // Force-loads the RS classes it targets, and checks it is really in them.
            check("server.applied." + mixin.substring(mixin.lastIndexOf('.') + 1), () -> SelftestMixins.problemOf(mixin));
        }
        // Before any probe below loads something on a jar that does not strip the client classes.
        check("server.no_client_class_was_loaded", ServerSelftest::loadedClientClass);
        check("server.no_client_reference_in_class_files", ServerSelftest::clientReference);
        if (JOINED_JAR) {
            line("SKIP server.client_classes_unreachable: this development jar keeps the client classes on a server");
            line("SKIP server.client_block_targets_unreachable: same reason");
            return;
        }
        check("server.client_classes_unreachable", () -> {
            for (String name : CLIENT_CLASSES) {
                if (loads(name)) return name + " can be loaded here";
            }
            return null;
        });
        check("server.client_block_targets_unreachable", () -> {
            for (String mixin : SelftestMixins.mixinsOf(COMMON_CONFIG, "client")) {
                for (String target : SelftestMixins.targetsOf(mixin)) {
                    if (loads(target)) return target + " (target of " + mixin + ") can be loaded here";
                }
            }
            return null;
        });
    }

    /** Whether a class loads. Stripped classes throw, each loader in its own way, hence Throwable. */
    private static boolean loads(String name) {
        try {
            Class.forName(name, false, LOADER);
            return true;
        } catch (Throwable expected) {
            return false;
        }
    }

    /**
     * Reads the JVM's list of every class loaded so far: the mod is loaded and the RS classes its common
     * mixins target are forced in. Null when none is a client class, otherwise the first ones found.
     */
    private static String loadedClientClass() throws IOException {
        if (!Files.exists(CLASS_LOG)) return "no class load log at " + CLASS_LOG.toAbsolutePath();
        boolean sawThis = false;
        List<String> found = new ArrayList<>();
        for (String entry : Files.readAllLines(CLASS_LOG, StandardCharsets.ISO_8859_1)) {
            int start = entry.indexOf("] ");
            String name = start < 0 ? entry : entry.substring(entry.lastIndexOf("] ") + 2);
            sawThis |= name.startsWith(ServerSelftest.class.getName() + " ");
            if (name.startsWith("net.minecraft.client.") || name.startsWith("com.vyrriox.rspolymorph.client.")
                    || name.startsWith("com.refinedmods.refinedstorage.common.grid.screen.")) {
                found.add(name.split(" ")[0]);
            }
        }
        if (!sawThis) return "the class load log does not list this class: it proves nothing";
        return found.isEmpty() ? null : found.size() + " client classes loaded, first: "
                + found.subList(0, Math.min(5, found.size()));
    }

    /**
     * Walks the class files of the mod from the server roots and the common mixins, following every
     * class of the mod they name. Null when none names a client type, otherwise the first one found.
     */
    private static String clientReference() throws IOException {
        Deque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        for (String root : ROOTS) queue.add(root.replace('.', '/'));
        for (String mixin : SelftestMixins.mixinsOf(COMMON_CONFIG, "mixins")) queue.add(mixin.replace('.', '/'));
        int scanned = 0;
        List<String> allowed = new ArrayList<>();
        while (!queue.isEmpty()) {
            String name = queue.poll();
            if (!seen.add(name)) continue;
            String text;
            try (InputStream in = LOADER.getResourceAsStream(name + ".class")) {
                if (in == null) continue;   // The entry points of the other loader, or a package name.
                // The constant pool stores class names as plain ASCII.
                text = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            }
            scanned++;
            for (String forbidden : FORBIDDEN) {
                if (name.startsWith(forbidden)) return "reached " + name;
                if (text.contains(forbidden)) return name + " names " + forbidden;
            }
            for (String api : LOADER_CLIENT_API) {
                if (!text.contains(api)) continue;
                if (!name.endsWith("NetworkPlatform")) return name + " names " + api;
                allowed.add(name);
            }
            Matcher matcher = OWN_CLASS.matcher(text);
            while (matcher.find()) queue.add(matcher.group());
        }
        if (scanned < 10) return "only " + scanned + " class files could be read";
        LOG.info("RSP-SELFTEST scanned {} class files; loader client API only in {}", scanned, allowed);
        return null;
    }

    private interface Check {
        /** Null when it holds, otherwise why not. */
        String problem() throws Exception;
    }

    private static void check(String name, Check check) {
        try {
            String problem = check.problem();
            if (problem == null) {
                passed++;
                line("PASS " + name);
            } else {
                fail(name, problem);
            }
        } catch (Throwable t) {
            LOG.error("RSP-SELFTEST check {} threw", name, t);
            fail(name, t.toString());
        }
    }

    private static void fail(String name, String why) {
        failed++;
        line("FAIL " + name + ": " + why.replaceAll("\\s+", " "));
    }

    private static void line(String text) {
        String full = "RSP-SELFTEST " + text;
        LOG.info(full);
        try {
            Files.writeString(REPORT, full + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Cannot write {}", REPORT, e);
        }
    }
}
