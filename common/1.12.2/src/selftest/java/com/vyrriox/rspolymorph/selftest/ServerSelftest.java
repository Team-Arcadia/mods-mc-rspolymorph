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

import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.coremod.GridTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Proves the dedicated server distribution without running a server: this runs on FML's server launch
 * path, with its class transformers and side checks active, before eula.txt is read. No EULA is
 * accepted and no world or mod is started. Prints the same RSP-SELFTEST lines as the client run; trust
 * the DONE line, not the exit code.
 *
 * Author: vyrriox
 */
public final class ServerSelftest {

    private static final String ROOT = "com/vyrriox/rspolymorph/";
    private static int pass;
    private static int fail;

    private ServerSelftest() {
    }

    public static void run() {
        try {
            checks();
        } catch (Throwable t) {
            t.printStackTrace();
            check("server.no_exception", false, String.valueOf(t));
        }
        print("DONE pass=" + pass + " fail=" + fail);
        System.out.flush();
        // FML traps every other way out of the JVM.
        net.minecraftforge.fml.common.FMLCommonHandler.instance().exitJava(fail == 0 ? 0 : 1, true);
    }

    private static void checks() throws Exception {
        check("dist.side_is_server", FMLLaunchHandler.side() == Side.SERVER, String.valueOf(FMLLaunchHandler.side()));

        List<String> names = modClasses();
        check("dist.classes_listed", names.size() > 10, names.toString());
        List<String> shared = new ArrayList<>();
        List<String> client = new ArrayList<>();
        for (String name : names) {
            boolean clientOnly = name.startsWith(ROOT + "client/") || name.endsWith("/ClientProxy");
            boolean test = name.startsWith(ROOT + "selftest/");
            if (clientOnly) {
                client.add(name);
            } else if (!test) {
                shared.add(name);
            }
        }

        // Class files: no shared class may name a client class at all.
        List<String> offenders = new ArrayList<>();
        for (String name : shared) {
            String pool = new String(read(name), StandardCharsets.ISO_8859_1);
            if (pool.contains("net/minecraft/client/") || pool.contains(ROOT + "client/")) {
                offenders.add(name);
            }
        }
        check("dist.shared_classes_name_no_client_class", offenders.isEmpty(), offenders.toString());

        // Class loading: linked with their members, through the transformers of the server side.
        List<String> failed = new ArrayList<>();
        for (String name : shared) {
            String dotted = name.replace('/', '.');
            try {
                Class<?> type = Class.forName(dotted, true, Launch.classLoader);
                type.getDeclaredMethods();
                type.getDeclaredFields();
            } catch (Throwable t) {
                failed.add(dotted + " " + t);
            }
        }
        check("dist.shared_classes_load", failed.isEmpty(), failed.toString());

        // The @SideOnly classes are refused here: nothing can reach them by accident.
        List<String> loaded = new ArrayList<>();
        for (String name : client) {
            try {
                Class.forName(name.replace('/', '.'), false, Launch.classLoader);
                loaded.add(name);
            } catch (Throwable expected) {
                // Refused by FML's SideTransformer, which is the point.
            }
        }
        check("dist.client_classes_refused", loaded.isEmpty() && !client.isEmpty(), loaded + " of " + client);

        Class<?> node = Class.forName("com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid", true, Launch.classLoader);
        Class<?> itf = Class.forName(IRsPolymorphGrid.class.getName(), false, Launch.classLoader);
        check("dist.node_patched", itf.isAssignableFrom(node), "the grid node does not implement the interface");
        Class.forName("com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern", true, Launch.classLoader);
        Class<?> transformer = Class.forName(GridTransformer.class.getName(), true, Launch.classLoader);
        Object hooks = transformer.getMethod("applied").invoke(null);
        check("dist.all_hooks_applied", ((List<?>) hooks).size() == 5, String.valueOf(hooks));

        // The server's proxy resolves; its class is what @SidedProxy names for this side.
        Class<?> proxy = Class.forName("com.vyrriox.rspolymorph.forge.CommonProxy", true, Launch.classLoader);
        proxy.getMethod("registerClientEvents").invoke(proxy.getDeclaredConstructor().newInstance());
        pass("dist.server_proxy_runs");

        File eula = new File("eula.txt");
        boolean accepted = eula.exists() && new String(Files.readAllBytes(eula.toPath()), StandardCharsets.UTF_8).contains("eula=true");
        check("dist.eula_not_accepted", !accepted, "eula.txt says true");
    }

    /** Every class of the mod, from the folders or the jar that hold them. */
    private static List<String> modClasses() throws Exception {
        List<String> names = new ArrayList<>();
        // The sources of the classes: the two marks sit in the common and in the loader folder.
        for (String mark : new String[]{"RsPolymorph", "forge/CommonProxy"}) {
            java.net.URL url = Launch.classLoader.findResource(ROOT + mark + ".class");
            if (url == null) {
                print("NOTE no source for " + mark);
            } else if ("jar".equals(url.getProtocol())) {
                java.net.JarURLConnection connection = (java.net.JarURLConnection) url.openConnection();
                connection.setUseCaches(false);
                try (java.util.jar.JarFile jar = connection.getJarFile()) {
                    jar.stream().map(java.util.zip.ZipEntry::getName)
                            .filter(n -> n.startsWith(ROOT) && n.endsWith(".class"))
                            .map(n -> n.substring(0, n.length() - ".class".length()))
                            .filter(n -> !names.contains(n))
                            .forEach(names::add);
                }
            } else {
                Path file = Paths.get(url.toURI());
                Path dir = file;
                for (int i = 0; i < (ROOT + mark).split("/").length; i++) {
                    dir = dir.getParent();
                }
                Path base = dir;
                try (Stream<Path> files = Files.walk(base.resolve(ROOT))) {
                    files.filter(f -> f.toString().endsWith(".class"))
                            .map(f -> base.relativize(f).toString().replace('\\', '/').replace(".class", ""))
                            .filter(n -> !names.contains(n))
                            .forEach(names::add);
                }
            }
        }
        return names;
    }

    private static byte[] read(String name) throws Exception {
        try (InputStream in = ServerSelftest.class.getClassLoader().getResourceAsStream(name + ".class")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    private static void check(String name, boolean ok, String why) {
        if (ok) {
            pass(name);
        } else {
            fail++;
            print("FAIL " + name + ": " + why);
        }
    }

    private static void pass(String name) {
        pass++;
        print("PASS " + name);
    }

    private static void print(String text) {
        System.out.println("RSP-SELFTEST " + text);
    }
}
