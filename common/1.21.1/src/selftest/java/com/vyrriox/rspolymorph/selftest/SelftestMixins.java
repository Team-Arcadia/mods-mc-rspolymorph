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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mixin proofs shared by the client and the dedicated server selftests. Holds no client type: the
 * server run loads this class.
 *
 * Author: vyrriox
 */
final class SelftestMixins {

    private SelftestMixins() {
    }

    /** What {@code MixinEnvironment#audit} reported about this mod. */
    record AuditResult(Throwable thrown, List<String> logged) {
        /** Null when clean for this mod, whatever other mods did. */
        String problem() {
            if (thrown != null) {
                StringBuilder chain = new StringBuilder();
                for (Throwable c = thrown; c != null; c = c.getCause() == c ? null : c.getCause()) {
                    chain.append(c).append('\n');
                }
                if (chain.toString().contains("rspolymorph")) return "the audit failed on this mod: " + thrown;
            }
            return logged.isEmpty() ? null : logged.size() + " errors, first: " + logged.get(0);
        }
    }

    /** Runs the audit, which forces every mixin of the environment to apply. Keeps what is about this mod. */
    static AuditResult audit() {
        List<String> logged = Collections.synchronizedList(new ArrayList<>());
        Appender appender = new AbstractAppender("rspolymorph-selftest-audit", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (!event.getLevel().isMoreSpecificThan(org.apache.logging.log4j.Level.WARN)) return;
                String text = event.getMessage().getFormattedMessage();
                Throwable thrown = event.getThrown();
                if (thrown != null) text += " " + thrown;
                if (event.getLoggerName().toLowerCase().contains("mixin") && text.contains("rspolymorph")) {
                    logged.add(text);
                }
            }
        };
        appender.start();
        org.apache.logging.log4j.core.Logger root = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
        root.addAppender(appender);
        Throwable thrown = null;
        try {
            MixinEnvironment.getCurrentEnvironment().audit();
        } catch (Throwable t) {
            thrown = t;
        }
        root.removeAppender(appender);
        appender.stop();
        return new AuditResult(thrown, logged);
    }

    /** The full names of the mixins declared in the given blocks of a config of this mod. */
    static List<String> mixinsOf(String config, String... blocks) throws IOException {
        try (InputStream in = SelftestMixins.class.getClassLoader().getResourceAsStream(config)) {
            if (in == null) throw new IOException("mixin config not found: " + config);
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String pkg = json.get("package").getAsString();
            List<String> names = new ArrayList<>();
            for (String block : blocks) {
                JsonArray array = json.getAsJsonArray(block);
                if (array == null) continue;
                for (JsonElement e : array) names.add(pkg + "." + e.getAsString());
            }
            return names;
        }
    }

    /** The classes a mixin targets, read from its class file: a mixin class cannot be loaded. */
    @SuppressWarnings("unchecked")
    static List<String> targetsOf(String mixin) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = SelftestMixins.class.getClassLoader().getResourceAsStream(mixin.replace('.', '/') + ".class")) {
            if (in == null) throw new IOException("class bytes not found: " + mixin);
            new ClassReader(in.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
        }
        List<String> targets = new ArrayList<>();
        if (node.invisibleAnnotations != null) {
            for (AnnotationNode annotation : node.invisibleAnnotations) {
                if (!annotation.desc.endsWith("/Mixin;") || annotation.values == null) continue;
                for (int i = 0; i < annotation.values.size(); i += 2) {
                    Object value = annotation.values.get(i + 1);
                    if ("value".equals(annotation.values.get(i))) {
                        for (Type t : (List<Type>) value) targets.add(t.getClassName());
                    } else if ("targets".equals(annotation.values.get(i))) {
                        for (String s : (List<String>) value) targets.add(s.replace('/', '.'));
                    }
                }
            }
        }
        return targets;
    }

    /** Force-loads the mixin's targets. Null when it really is in each of them, otherwise what is missing. */
    static String problemOf(String mixin) throws Exception {
        List<String> targets = targetsOf(mixin);
        if (targets.isEmpty()) return "no @Mixin target found in its class file";
        for (String target : targets) {
            Class<?> cls = Class.forName(target, true, SelftestMixins.class.getClassLoader());
            if (!hasMixin(cls, mixin)) return "not merged into " + target;
        }
        return null;
    }

    private static boolean hasMixin(Class<?> cls, String mixin) {
        for (Class<?> i : cls.getInterfaces()) {
            if (i.getName().equals(mixin)) return true;
        }
        for (Method m : cls.getDeclaredMethods()) {
            MixinMerged merged = m.getAnnotation(MixinMerged.class);
            if (merged != null && merged.mixin().equals(mixin)) return true;
        }
        for (Field f : cls.getDeclaredFields()) {
            MixinMerged merged = f.getAnnotation(MixinMerged.class);
            if (merged != null && merged.mixin().equals(mixin)) return true;
        }
        return false;
    }
}
