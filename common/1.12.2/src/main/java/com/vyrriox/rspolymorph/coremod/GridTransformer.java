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
package com.vyrriox.rspolymorph.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The only bytecode this mod changes, in two Refined Storage classes:
 * <ul>
 * <li>{@code NetworkNodeGrid}: a field for the selection with its accessors, a call at the head of
 * {@code onCraftingMatrixChanged}, {@code read} and {@code write}, and one on the stack that
 * {@code onCreatePattern} puts in the output slot;</li>
 * <li>{@code CraftingPattern}: one call on the recipe iterator of the constructor.</li>
 * </ul>
 * No Minecraft method name appears here, so nothing depends on the mappings. No branch is added
 * either, so the stack frames stay valid. A hook point that cannot be found leaves the class as it
 * is and logs an error: the grid then behaves as plain Refined Storage rather than crash the game.
 *
 * Author: vyrriox
 */
public final class GridTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("rspolymorph");

    private static final String NODE_CLASS = "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid";
    private static final String PATTERN_CLASS = "com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern";
    private static final String NODE = "com/raoulvdberge/refinedstorage/apiimpl/network/node/NetworkNodeGrid";
    private static final String PATTERN = "com/raoulvdberge/refinedstorage/apiimpl/autocrafting/CraftingPattern";
    private static final String CRAFTING_MANAGER = "net/minecraft/item/crafting/CraftingManager";
    private static final String HOOKS = "com/vyrriox/rspolymorph/GridHooks";
    private static final String GRID_ITF = "com/vyrriox/rspolymorph/IRsPolymorphGrid";
    private static final String L_GRID_ITF = "L" + GRID_ITF + ";";
    private static final String L_STACK = "Lnet/minecraft/item/ItemStack;";
    private static final String L_TAG = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String L_RECIPE = "Lnet/minecraft/item/crafting/IRecipe;";
    private static final String L_ID = "Lnet/minecraft/util/ResourceLocation;";
    private static final String L_ITERATOR = "Ljava/util/Iterator;";
    private static final String FIELD_RECIPE = "currentRecipe";
    private static final String FIELD_SELECTED = "rspolymorph$selected";

    /** Names of the hooks in place, for the log and the selftest. */
    private static final List<String> APPLIED = new CopyOnWriteArrayList<>();

    public static List<String> applied() {
        return new ArrayList<>(APPLIED);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        if (NODE_CLASS.equals(transformedName)) {
            return rewrite(basicClass, GridTransformer::patchNode, "NetworkNodeGrid");
        }
        if (PATTERN_CLASS.equals(transformedName)) {
            return rewrite(basicClass, GridTransformer::patchPattern, "CraftingPattern");
        }
        return basicClass;
    }

    private interface Patch {
        /** @return the hooks it placed, or null when a hook point is missing and nothing must change */
        List<String> apply(ClassNode node);
    }

    private static byte[] rewrite(byte[] original, Patch patch, String label) {
        ClassNode node = new ClassNode();
        new ClassReader(original).accept(node, 0);
        List<String> hooks = patch.apply(node);
        if (hooks == null) {
            LOG.error("Unknown {} layout, recipe selection is off for it", label);
            return original;
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        APPLIED.addAll(hooks);
        LOG.info("Patched {}: {}", label, hooks);
        return writer.toByteArray();
    }

    private static List<String> patchNode(ClassNode node) {
        boolean hasRecipeField = false;
        for (FieldNode field : node.fields) {
            hasRecipeField |= FIELD_RECIPE.equals(field.name) && L_RECIPE.equals(field.desc);
        }
        MethodNode changed = find(node, "onCraftingMatrixChanged", "()V");
        MethodNode read = find(node, "read", "(" + L_TAG + ")V");
        MethodNode write = find(node, "write", "(" + L_TAG + ")" + L_TAG);
        MethodNode create = find(node, "onCreatePattern", "()V");
        AbstractInsnNode setSlot = create == null ? null : findSetOutputSlot(create);
        if (!hasRecipeField || changed == null || read == null || write == null || setSlot == null) {
            return null;
        }

        node.interfaces.add(GRID_ITF);
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, FIELD_SELECTED, L_ID, null, null));
        addGetter(node, "rspolymorph$getSelected", FIELD_SELECTED, L_ID);
        addSetter(node, "rspolymorph$setSelected", FIELD_SELECTED, L_ID);
        addGetter(node, "rspolymorph$getCurrentRecipe", FIELD_RECIPE, L_RECIPE);
        addSetter(node, "rspolymorph$setCurrentRecipe", FIELD_RECIPE, L_RECIPE);

        changed.instructions.insert(headCall("applySelection", false));
        read.instructions.insert(headCall("readSelection", true));
        write.instructions.insert(headCall("writeSelection", true));

        InsnList tag = new InsnList();
        tag.add(new VarInsnNode(Opcodes.ALOAD, 0));
        tag.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "tagPattern",
                "(" + L_STACK + L_GRID_ITF + ")" + L_STACK, false));
        create.instructions.insertBefore(setSlot, tag);

        List<String> hooks = new ArrayList<>();
        hooks.add("onCraftingMatrixChanged");
        hooks.add("read");
        hooks.add("write");
        hooks.add("onCreatePattern");
        return hooks;
    }

    private static List<String> patchPattern(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            AbstractInsnNode iterator = findRegistryIterator(method);
            if (iterator == null) {
                continue;
            }
            InsnList call = new InsnList();
            call.add(new VarInsnNode(Opcodes.ALOAD, 0));
            call.add(new FieldInsnNode(Opcodes.GETFIELD, PATTERN, "stack", L_STACK));
            call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "taggedFirst",
                    "(" + L_ITERATOR + L_STACK + ")" + L_ITERATOR, false));
            method.instructions.insert(iterator, call);
            List<String> hooks = new ArrayList<>();
            hooks.add("CraftingPattern.<init>");
            return hooks;
        }
        return null;
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return method;
            }
        }
        return null;
    }

    /** The last {@code setStackInSlot(int, ItemStack)} of the method: the pattern entering its slot. */
    private static AbstractInsnNode findSetOutputSlot(MethodNode method) {
        AbstractInsnNode found = null;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode
                    && "setStackInSlot".equals(((MethodInsnNode) insn).name)
                    && ("(I" + L_STACK + ")V").equals(((MethodInsnNode) insn).desc)) {
                found = insn;
            }
        }
        return found;
    }

    /** {@code CraftingManager.REGISTRY.iterator()}, found by its owner: the field name is obfuscated. */
    private static AbstractInsnNode findRegistryIterator(MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode
                    && "iterator".equals(((MethodInsnNode) insn).name)
                    && insn.getPrevious() instanceof FieldInsnNode
                    && insn.getPrevious().getOpcode() == Opcodes.GETSTATIC
                    && CRAFTING_MANAGER.equals(((FieldInsnNode) insn.getPrevious()).owner)) {
                return insn;
            }
        }
        return null;
    }

    private static InsnList headCall(String hook, boolean withTag) {
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        if (withTag) {
            call.add(new VarInsnNode(Opcodes.ALOAD, 1));
        }
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook,
                "(" + L_GRID_ITF + (withTag ? L_TAG : "") + ")V", false));
        return call;
    }

    private static void addGetter(ClassNode node, String name, String field, String desc) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, name, "()" + desc, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, NODE, field, desc));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(method);
    }

    private static void addSetter(ClassNode node, String name, String field, String desc) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, name, "(" + desc + ")V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, NODE, field, desc));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);
    }
}
