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

import com.google.common.io.ByteStreams;
import com.vyrriox.rspolymorph.platform.Services;
import net.minecraft.util.registry.Bootstrap;
import net.minecraftforge.fml.server.ServerModLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Proof on the dedicated server distribution. 1.16.5 has no game test launch: this starts from
 * {@code MixinServerMainSelftest} at the first line of the server's main method, before eula.txt is
 * read. It loads the mods the way the server would and never goes further: no world and no server,
 * so the packet validation is proved by the client run, on its integrated server.
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
    private static final Path REPORT = Paths.get("rspolymorph-selftest-server.txt");
    private static final Path EULA = Paths.get("eula.txt");
    private static final String COMMON_CONFIG = "rspolymorph-common.mixins.json";
    private static final ClassLoader LOADER = ServerSelftest.class.getClassLoader();

    /** Client classes of the game: unloadable on this distribution, or this proof proves nothing. */
    private static final List<String> CLIENT_CLASSES = Arrays.asList(
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.gui.screen.Screen",
            "net.minecraft.client.gui.screen.inventory.ContainerScreen");

    /** What the mod's server side may never name. */
    private static final List<String> FORBIDDEN = Arrays.asList(
            "net/minecraft/client/", "com/mojang/blaze3d/", "com/vyrriox/rspolymorph/client/",
            "com/refinedmods/refinedstorage/screen/");

    /**
     * Client APIs of the loaders. Allowed only in the network platform: its client-to-server send is
     * only ever called from client code, so the JVM never links that reference on a server.
     */
    private static final List<String> LOADER_CLIENT_API = Arrays.asList(
            "net/minecraftforge/client/", "net/minecraftforge/fml/client/");

    /** Everything a server loads of this mod starts from these, and the common mixins. */
    private static final List<String> ROOTS = Arrays.asList(
            "com.vyrriox.rspolymorph.RsPolymorph",
            "com.vyrriox.rspolymorph.RecipeAccess",
            "com.vyrriox.rspolymorph.GridOwnership",
            "com.vyrriox.rspolymorph.IRsPolymorphGrid",
            "com.vyrriox.rspolymorph.network.SelectRecipeHandler",
            "com.vyrriox.rspolymorph.platform.Services",
            "com.vyrriox.rspolymorph.platform.NetworkPlatform",
            "com.vyrriox.rspolymorph.mixin.RsPolymorphMixinPlugin",
            "com.vyrriox.rspolymorph.forge.RsPolymorphForge",
            "com.vyrriox.rspolymorph.forge.ForgeNetwork");

    /**
     * Named as client classes by the mappings of the time, yet common code of the game: the server loads
     * them itself, and they carry no client-only marker. Anything else in that package counts as a leak.
     */
    private static final List<String> SHARED_CLIENT_PACKAGE_CLASSES = Arrays.asList(
            "net.minecraft.client.audio.BackgroundMusicSelector",
            "net.minecraft.client.audio.BackgroundMusicTracks",
            "net.minecraft.client.resources.ReloadListener",
            "net.minecraft.client.resources.JsonReloadListener");

    private static final Pattern OWN_CLASS = Pattern.compile("com/vyrriox/rspolymorph/[A-Za-z0-9_/$]+");

    private static boolean started;
    private static int passed;
    private static int failed;

    private ServerSelftest() {
    }

    /** Called at the first line of the server's main method. Returns only when the selftest is off. */
    public static void onServerMain() {
        if (!ENABLED || started) return;
        started = true;
        try {
            Files.deleteIfExists(REPORT);
            // What the main method does past the EULA, up to the mods being loaded.
            Bootstrap.bootStrap();
            ServerModLoader.load();
            run();
        } catch (Throwable t) {
            fail("scenario", t.toString());
        }
        line("DONE pass=" + passed + " fail=" + failed);
        LogManager.shutdown();
        // No world and no server exist, and none is wanted. Nothing to save, so nothing to wait for.
        Runtime.getRuntime().halt(failed == 0 ? 0 : 1);
    }

    private static void run() throws Exception {
        check("server.environment_is_server", () -> {
            MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();
            return side == MixinEnvironment.Side.SERVER ? null : "the mixin side is " + side;
        });
        check("server.mod_loaded_with_network", () -> Services.network() != null ? null : "the mod entry point did not run");
        check("server.audit_no_failure_in_common_config", () -> SelftestMixins.audit().problem());
        for (String mixin : SelftestMixins.mixinsOf(COMMON_CONFIG, "mixins")) {
            // Force-loads the RS classes it targets, and checks it is really in them.
            check("server.applied." + mixin.substring(mixin.lastIndexOf('.') + 1), () -> SelftestMixins.problemOf(mixin));
        }
        // Before any probe below loads something on a jar that does not strip the client classes.
        check("server.no_client_class_was_loaded", ServerSelftest::loadedClientClass);
        check("server.eula_untouched", () -> {
            if (!Files.exists(EULA)) return null;
            String text = new String(Files.readAllBytes(EULA), StandardCharsets.UTF_8);
            return text.contains("eula=true") ? "eula.txt was accepted by someone" : null;
        });
        line("SKIP server.world_and_packet: a world needs a running server, so the EULA; the client run proves them on its integrated server");
        check("server.no_client_reference_in_class_files", ServerSelftest::clientReference);
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
     * Reads the list of every class each class loader defined so far (a private field Java 8 lets us
     * read): the mod is loaded and the RS classes its common mixins target are forced in. Null when none
     * is a client class, otherwise the first ones found. RS's screen package also holds plain interfaces
     * its menus use, so only its screens count. The few classes of the client package that the vanilla
     * server jar ships too are not client classes.
     */
    private static String loadedClientClass() throws ReflectiveOperationException {
        Field classes = ClassLoader.class.getDeclaredField("classes");
        classes.setAccessible(true);
        boolean sawThis = false;
        List<String> found = new ArrayList<>();
        for (ClassLoader loader = LOADER; loader != null; loader = loader.getParent()) {
            Vector<?> defined = (Vector<?>) classes.get(loader);
            for (Object each : defined.toArray()) {
                String name = ((Class<?>) each).getName();
                sawThis |= name.equals(ServerSelftest.class.getName());
                if ((name.startsWith("net.minecraft.client.") && !SHARED_CLIENT_PACKAGE_CLASSES.contains(name))
                        || name.startsWith("com.vyrriox.rspolymorph.client.")
                        || name.equals("com.refinedmods.refinedstorage.screen.BaseScreen")
                        || name.equals("com.refinedmods.refinedstorage.screen.grid.GridScreen")) {
                    found.add(name);
                }
            }
        }
        if (!sawThis) return "the loaded classes do not list this class: it proves nothing";
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
                text = new String(ByteStreams.toByteArray(in), StandardCharsets.ISO_8859_1);
            }
            scanned++;
            for (String forbidden : FORBIDDEN) {
                if (name.startsWith(forbidden)) return "reached " + name;
                if (text.contains(forbidden)) return name + " names " + forbidden;
            }
            for (String api : LOADER_CLIENT_API) {
                if (!text.contains(api)) continue;
                if (!name.endsWith("Network")) return name + " names " + api;
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
            Files.write(REPORT, (full + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Cannot write {}", REPORT, e);
        }
    }
}
