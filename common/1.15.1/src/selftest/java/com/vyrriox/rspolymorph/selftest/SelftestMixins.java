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

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This Forge has no Mixin: the same proofs, but on the coremod's transformers. Each hook leaves a marker field
 * on its class, read here after force-loading that class. "Mixin" names stay so that the shared selftest calls
 * the same methods, an entry being {@code class#hook}. Holds no client type: the server run loads this class.
 *
 * Author: vyrriox
 */
final class SelftestMixins {

    private static final Logger LOG = LogManager.getLogger("rspolymorph-selftest");
    private static final String NODE = "com.raoulvdberge.refinedstorage.apiimpl.network.node.GridNetworkNode";
    private static final String FACTORY = "com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPatternFactory";
    private static final String SCREEN = "com.raoulvdberge.refinedstorage.screen.grid.GridScreen";
    private static final String SERVER = "net.minecraft.server.MinecraftServer";

    private static final List<String> COMMON_HOOKS = Arrays.asList(
            NODE + "#init",
            NODE + "#onCraftingMatrixChanged",
            NODE + "#onCreatePattern",
            NODE + "#read",
            NODE + "#write",
            FACTORY + "#create");
    private static final List<String> CLIENT_HOOKS = Arrays.asList(SCREEN + "#widget", SCREEN + "#onPostInit");
    private static final List<String> SELFTEST_HOOKS = Collections.singletonList(SERVER + "#main");

    private SelftestMixins() {
    }

    /** Nothing is ever left out on this line: Polymorph has no build for it. */
    static boolean isSelectionMixin(String hook) {
        return false;
    }

    /** -PauditMixins with -Pselftest, for a plain run. */
    static void logAudit() {
        String problem = audit().problem();
        if (problem == null) {
            LOG.info("RSP-AUDIT clean for this mod");
        } else {
            LOG.error("RSP-AUDIT {}", problem);
        }
    }

    /** What the forced load of every transformer found. */
    static final class AuditResult {

        private final String problem;

        AuditResult(String problem) {
            this.problem = problem;
        }

        Throwable thrown() {
            return null;
        }

        String problem() {
            return problem;
        }
    }

    /** Loads every class a transformer of this side targets. */
    static AuditResult audit() {
        List<String> hooks = new ArrayList<>(COMMON_HOOKS);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            hooks.addAll(CLIENT_HOOKS);
        }
        List<String> problems = new ArrayList<>();
        for (String hook : hooks) {
            String problem = problemOf(hook);
            if (problem != null) problems.add(hook + ": " + problem);
        }
        return new AuditResult(problems.isEmpty() ? null : problems.size() + " missing, first: " + problems.get(0));
    }

    /** The hooks declared in the given blocks ("mixins" or "client") of a config. */
    static List<String> mixinsOf(String config, String... blocks) {
        List<String> names = new ArrayList<>();
        for (String block : blocks) {
            if (config.contains("selftest")) {
                if (block.equals("mixins")) names.addAll(SELFTEST_HOOKS);
            } else {
                names.addAll(block.equals("client") ? CLIENT_HOOKS : COMMON_HOOKS);
            }
        }
        return names;
    }

    static List<String> targetsOf(String hook) {
        return Collections.singletonList(hook.substring(0, hook.indexOf('#')));
    }

    /** Force-loads the class of the hook. Null when its marker is there, otherwise what is missing. */
    static String problemOf(String hook) {
        String target = hook.substring(0, hook.indexOf('#'));
        String marker = "rspolymorph$hook_" + hook.substring(hook.indexOf('#') + 1);
        try {
            Class.forName(target, true, SelftestMixins.class.getClassLoader()).getDeclaredField(marker);
            return null;
        } catch (ReflectiveOperationException | LinkageError e) {
            return marker + " not on " + target + ": " + e;
        }
    }
}
