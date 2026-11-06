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
 * The only bytecode this mod changes, in one Refined Storage class:
 * <ul>
 * <li>the grid: two fields with their accessors, the vanilla recipe lookup of
 * {@code onCraftingMatrixChanged} and the vanilla remainder lookups of {@code onCrafted} and
 * {@code onCreatePattern} replaced, and a call at the head of its save and load methods.</li>
 * </ul>
 * Patterns of this Refined Storage keep what the grid showed when they were printed, so nothing else
 * changes. Vanilla calls are found by owner and descriptor. The only Minecraft names here are those of
 * {@code readFromNBT} and {@code writeToNBT}, which have the same descriptor on the oldest Refined
 * Storage that has no {@code read} of its own. No branch is added, so the stack frames stay valid. A hook
 * point that cannot be found leaves the class as it is and logs an error: the grid then behaves as
 * plain Refined Storage rather than crash the game.
 *
 * Author: vyrriox
 */
public final class GridTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("rspolymorph");

    private static final String NODE_CLASS = "com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid";
    private static final String NODE = "com/raoulvdberge/refinedstorage/apiimpl/network/node/NetworkNodeGrid";
    private static final String CRAFTING_MANAGER = "net/minecraft/item/crafting/CraftingManager";
    private static final String HOOKS = "com/vyrriox/rspolymorph/GridHooks";
    private static final String GRID_ITF = "com/vyrriox/rspolymorph/IRsPolymorphGrid";
    private static final String L_GRID_ITF = "L" + GRID_ITF + ";";
    private static final String L_MANAGER = "L" + CRAFTING_MANAGER + ";";
    private static final String L_MATRIX = "Lnet/minecraft/inventory/InventoryCrafting;";
    private static final String L_WORLD = "Lnet/minecraft/world/World;";
    private static final String L_STACK = "Lnet/minecraft/item/ItemStack;";
    private static final String L_STACKS = "[" + L_STACK;
    private static final String L_TAG = "Lnet/minecraft/nbt/NBTTagCompound;";
    private static final String L_RECIPE = "Lnet/minecraft/item/crafting/IRecipe;";
    private static final String L_ID = "Lnet/minecraft/util/ResourceLocation;";
    private static final String FIELD_RECIPE = "rspolymorph$current";
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
            return rewrite(basicClass, GridTransformer::patchNode, "grid");
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
        MethodNode changed = find(node, "onCraftingMatrixChanged", "()V");
        MethodNode read = first(find(node, "read", "(" + L_TAG + ")V"),
                find(node, "readFromNBT", "(" + L_TAG + ")V"), find(node, "func_145839_a", "(" + L_TAG + ")V"));
        MethodNode write = first(find(node, "write", "(" + L_TAG + ")" + L_TAG),
                find(node, "writeToNBT", "(" + L_TAG + ")V"), find(node, "func_145841_b", "(" + L_TAG + ")V"));
        MethodNode create = find(node, "onCreatePattern", "()V");
        AbstractInsnNode lookup = changed == null ? null : findRecipeLookup(changed);
        MethodNode crafted = findNamed(node, "onCrafted");
        AbstractInsnNode remainders = crafted == null ? null
                : findManagerCall(crafted, "(" + L_MATRIX + L_WORLD + ")" + L_STACKS);
        if (lookup == null || read == null || write == null || create == null || remainders == null) {
            return null;
        }
        // The oldest Refined Storage keeps no byproducts on its patterns.
        AbstractInsnNode byproducts = findManagerCall(create, "(" + L_MATRIX + L_WORLD + ")" + L_STACKS);

        node.interfaces.add(GRID_ITF);
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, FIELD_SELECTED, L_ID, null, null));
        node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, FIELD_RECIPE, L_RECIPE, null, null));
        addGetter(node, "rspolymorph$getSelected", FIELD_SELECTED, L_ID);
        addSetter(node, "rspolymorph$setSelected", FIELD_SELECTED, L_ID);
        addGetter(node, "rspolymorph$getCurrentRecipe", FIELD_RECIPE, L_RECIPE);
        addSetter(node, "rspolymorph$setCurrentRecipe", FIELD_RECIPE, L_RECIPE);

        // The manager, matrix and world are on the stack already: add the grid, and call the hook.
        InsnList result = new InsnList();
        result.add(new VarInsnNode(Opcodes.ALOAD, 0));
        result.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "result",
                "(" + L_MANAGER + L_MATRIX + L_WORLD + L_GRID_ITF + ")" + L_STACK, false));
        changed.instructions.insert(lookup, result);
        changed.instructions.remove(lookup);

        replaceRemainders(crafted, remainders);
        if (byproducts != null) {
            replaceRemainders(create, byproducts);
        }

        read.instructions.insert(headCall("readSelection"));
        write.instructions.insert(headCall("writeSelection"));

        List<String> hooks = new ArrayList<>();
        hooks.add("onCraftingMatrixChanged");
        hooks.add("read");
        hooks.add("write");
        hooks.add("onCrafted");
        if (byproducts != null) {
            hooks.add("onCreatePattern");
        }
        return hooks;
    }

    /** The manager, matrix and world are on the stack already: add the grid, and call the hook. */
    private static void replaceRemainders(MethodNode method, AbstractInsnNode call) {
        InsnList remainder = new InsnList();
        remainder.add(new VarInsnNode(Opcodes.ALOAD, 0));
        remainder.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "remainders",
                "(" + L_MANAGER + L_MATRIX + L_WORLD + L_GRID_ITF + ")" + L_STACKS, false));
        method.instructions.insert(call, remainder);
        method.instructions.remove(call);
    }

    private static MethodNode first(MethodNode... candidates) {
        for (MethodNode method : candidates) {
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    /** {@code onCrafted} takes something else on each of these Refined Storage versions. */
    private static MethodNode findNamed(ClassNode node, String name) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name)) {
                return method;
            }
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

    /** {@code findMatchingRecipe(matrix, world)}: the only manager method that gives a stack from these. */
    private static AbstractInsnNode findRecipeLookup(MethodNode method) {
        return findManagerCall(method, "(" + L_MATRIX + L_WORLD + ")" + L_STACK);
    }

    private static AbstractInsnNode findManagerCall(MethodNode method, String desc) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode
                    && CRAFTING_MANAGER.equals(((MethodInsnNode) insn).owner)
                    && desc.equals(((MethodInsnNode) insn).desc)) {
                return insn;
            }
        }
        return null;
    }

    private static InsnList headCall(String hook) {
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        call.add(new VarInsnNode(Opcodes.ALOAD, 1));
        call.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook,
                "(" + L_GRID_ITF + L_TAG + ")V", false));
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
