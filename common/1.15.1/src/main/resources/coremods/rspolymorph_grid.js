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
 *
 * Forge bundles no Mixin on Minecraft 1.15.1 and 1.14.4, so the hooks into Refined Storage are transformers that
 * Forge runs itself. Each one only calls a static method of GridHooks, where the logic lives. Everything here is
 * found by Refined Storage's own names and by descriptors, so no vanilla method name is needed. A hook is either
 * fully there or not at all: a class that does not look as expected throws before it is changed, and Forge then
 * keeps it as it was and logs the error. Every hook leaves a marker field that the selftest checks.
 *
 * Author: vyrriox
 */
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnList = Java.type('org.objectweb.asm.tree.InsnList');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
var MethodNode = Java.type('org.objectweb.asm.tree.MethodNode');

var RS = 'com/raoulvdberge/refinedstorage/';
var NODE = RS + 'apiimpl/network/node/GridNetworkNode';
var NODE_DESC = 'L' + NODE + ';';
var FACTORY = RS + 'apiimpl/autocrafting/CraftingPatternFactory';
var SCREEN = RS + 'screen/grid/GridScreen';
var HOOKS = 'com/vyrriox/rspolymorph/coremod/GridHooks';
var GRID_INTERFACE = 'com/vyrriox/rspolymorph/IRsPolymorphGrid';
var SCREEN_INTERFACE = 'com/vyrriox/rspolymorph/client/IRsPolymorphScreen';
var WIDGET_DESC = 'Lcom/vyrriox/rspolymorph/client/GridRecipeWidget;';
var LOCATION_DESC = 'Lnet/minecraft/util/ResourceLocation;';
var RECIPE_DESC = 'Lnet/minecraft/item/crafting/ICraftingRecipe;';
var TAG_DESC = 'Lnet/minecraft/nbt/CompoundNBT;';
var STACK_DESC = 'Lnet/minecraft/item/ItemStack;';
var WORLD_DESC = 'Lnet/minecraft/world/World;';
var OPTIONAL_DESC = 'Ljava/util/Optional;';

function method(classNode, name, desc) {
    for (var i = 0; i < classNode.methods.size(); i++) {
        var candidate = classNode.methods.get(i);
        if (candidate.name === name && candidate.desc === desc) {
            return candidate;
        }
    }
    throw new Error('rspolymorph: no method ' + classNode.name + '.' + name + desc);
}

function hasField(classNode, name, desc) {
    for (var i = 0; i < classNode.fields.size(); i++) {
        var candidate = classNode.fields.get(i);
        if (candidate.name === name && candidate.desc === desc) {
            return true;
        }
    }
    return false;
}

function requireField(classNode, name, desc) {
    if (!hasField(classNode, name, desc)) {
        throw new Error('rspolymorph: no field ' + classNode.name + '.' + name + ' ' + desc);
    }
}

function insns(list) {
    var result = new InsnList();
    for (var i = 0; i < list.length; i++) {
        result.add(list[i]);
    }
    return result;
}

function aload(index) {
    return new VarInsnNode(Opcodes.ALOAD, index);
}

function callHook(name, desc) {
    return new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, name, desc, false);
}

/** The only call of that name and owner in the method. */
function onlyCall(target, owner, name) {
    var found = null;
    var all = target.instructions.toArray();
    for (var i = 0; i < all.length; i++) {
        var insn = all[i];
        if (insn.getOpcode() === Opcodes.INVOKEVIRTUAL && insn.owner === owner && insn.name === name) {
            if (found !== null) {
                throw new Error('rspolymorph: several calls of ' + name + ' in ' + target.name);
            }
            found = insn;
        }
    }
    if (found === null) {
        throw new Error('rspolymorph: no call of ' + name + ' in ' + target.name);
    }
    return found;
}

/** The first call returning an Optional that goes straight to a local: the recipe lookup, by its type alone. */
function optionalLookup(target) {
    var all = target.instructions.toArray();
    for (var i = 0; i + 1 < all.length; i++) {
        var insn = all[i];
        if (insn.getType() === 5 /* METHOD_INSN */ && insn.desc.endsWith(')' + OPTIONAL_DESC)
                && all[i + 1].getOpcode() === Opcodes.ASTORE) {
            return insn;
        }
    }
    throw new Error('rspolymorph: no recipe lookup in ' + target.name);
}

function marker(classNode, name) {
    classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
        'rspolymorph$hook_' + name, 'Z', null, true));
}

function getter(classNode, name, fieldName, desc) {
    var node = new MethodNode(Opcodes.ACC_PUBLIC, name, '()' + desc, null, null);
    node.instructions = insns([
        aload(0),
        new FieldInsnNode(Opcodes.GETFIELD, classNode.name, fieldName, desc),
        new InsnNode(Opcodes.ARETURN)
    ]);
    node.maxStack = 1;
    node.maxLocals = 1;
    classNode.methods.add(node);
}

function setter(classNode, name, fieldName, desc) {
    var node = new MethodNode(Opcodes.ACC_PUBLIC, name, '(' + desc + ')V', null, null);
    node.instructions = insns([
        aload(0),
        aload(1),
        new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, fieldName, desc),
        new InsnNode(Opcodes.RETURN)
    ]);
    node.maxStack = 2;
    node.maxLocals = 2;
    classNode.methods.add(node);
}

function transformNode(classNode) {
    // Everything is looked up first, so that a class that changed is left whole.
    requireField(classNode, 'currentRecipe', RECIPE_DESC);
    var constructor = method(classNode, '<init>',
        '(' + WORLD_DESC + 'Lnet/minecraft/util/math/BlockPos;L' + RS + 'api/network/grid/GridType;)V');
    var matrixChanged = method(classNode, 'onCraftingMatrixChanged', '()V');
    var createPattern = method(classNode, 'onCreatePattern', '()V');
    var read = method(classNode, 'read', '(' + TAG_DESC + ')V');
    var write = method(classNode, 'write', '(' + TAG_DESC + ')' + TAG_DESC);
    var setPattern = onlyCall(createPattern, RS + 'inventory/item/BaseItemHandler', 'setStackInSlot');

    classNode.interfaces.add(GRID_INTERFACE);
    classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, 'rspolymorph$selected', LOCATION_DESC, null, null));
    getter(classNode, 'rspolymorph$getSelected', 'rspolymorph$selected', LOCATION_DESC);
    setter(classNode, 'rspolymorph$setSelected', 'rspolymorph$selected', LOCATION_DESC);
    getter(classNode, 'rspolymorph$getCurrentRecipe', 'currentRecipe', RECIPE_DESC);

    // End of the constructor: the patterns listener comes after Refined Storage's own.
    var all = constructor.instructions.toArray();
    for (var i = 0; i < all.length; i++) {
        if (all[i].getOpcode() === Opcodes.RETURN) {
            constructor.instructions.insertBefore(all[i], insns([
                aload(0),
                callHook('attachListener', '(' + NODE_DESC + ')V')
            ]));
        }
    }
    constructor.maxStack += 1;
    marker(classNode, 'init');

    matrixChanged.instructions.insert(insns([
        aload(0),
        aload(0),
        aload(0),
        new FieldInsnNode(Opcodes.GETFIELD, NODE, 'currentRecipe', RECIPE_DESC),
        callHook('applySelection', '(' + NODE_DESC + RECIPE_DESC + ')' + RECIPE_DESC),
        new FieldInsnNode(Opcodes.PUTFIELD, NODE, 'currentRecipe', RECIPE_DESC)
    ]));
    matrixChanged.maxStack += 3;
    marker(classNode, 'onCraftingMatrixChanged');

    // The stack is tagged as it enters the slot: the method also runs when nothing is printed.
    createPattern.instructions.insertBefore(setPattern, insns([
        aload(0),
        aload(0),
        new FieldInsnNode(Opcodes.GETFIELD, NODE, 'currentRecipe', RECIPE_DESC),
        callHook('tagPattern', '(' + STACK_DESC + NODE_DESC + RECIPE_DESC + ')' + STACK_DESC)
    ]));
    createPattern.maxStack += 2;
    marker(classNode, 'onCreatePattern');

    // First thing: reading the matrix runs onCraftingMatrixChanged, which needs the selection.
    read.instructions.insert(insns([
        aload(0),
        aload(1),
        callHook('readSelection', '(' + NODE_DESC + TAG_DESC + ')V')
    ]));
    read.maxStack += 2;
    marker(classNode, 'read');

    write.instructions.insert(insns([
        aload(0),
        aload(1),
        callHook('writeSelection', '(' + NODE_DESC + TAG_DESC + ')V')
    ]));
    write.maxStack += 2;
    marker(classNode, 'write');
    return classNode;
}

function transformFactory(classNode) {
    var create = method(classNode, 'create',
        '(' + WORLD_DESC + 'L' + RS + 'api/autocrafting/ICraftingPatternContainer;' + STACK_DESC + ')L' + RS + 'apiimpl/autocrafting/CraftingPattern;');
    // The result of the lookup is swapped rather than the call, so nothing else on that call can clash with it.
    create.instructions.insert(optionalLookup(create), insns([
        aload(1),
        aload(3),
        callHook('resolveTagged', '(' + OPTIONAL_DESC + WORLD_DESC + STACK_DESC + ')' + OPTIONAL_DESC)
    ]));
    create.maxStack += 2;
    marker(classNode, 'create');
    return classNode;
}

/**
 * The screen owns its widget. Refined Storage also calls init() directly, which clears the buttons but fires no
 * Forge init event, so the button goes back from its own onPostInit. Forge's events do the rest of the screen.
 */
function transformScreen(classNode) {
    var postInit = method(classNode, 'onPostInit', '(II)V');

    classNode.interfaces.add(SCREEN_INTERFACE);
    classNode.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, 'rspolymorph$widget', WIDGET_DESC, null, null));
    getter(classNode, 'rspolymorph$getWidget', 'rspolymorph$widget', WIDGET_DESC);
    setter(classNode, 'rspolymorph$setWidget', 'rspolymorph$widget', WIDGET_DESC);
    marker(classNode, 'widget');

    var all = postInit.instructions.toArray();
    for (var i = 0; i < all.length; i++) {
        if (all[i].getOpcode() === Opcodes.RETURN) {
            postInit.instructions.insertBefore(all[i], insns([
                aload(0),
                new MethodInsnNode(Opcodes.INVOKESTATIC, 'com/vyrriox/rspolymorph/client/GridScreenEvents', 'onPostInit',
                    '(L' + SCREEN + ';)V', false)
            ]));
        }
    }
    postInit.maxStack += 1;
    marker(classNode, 'onPostInit');
    return classNode;
}

function initializeCoreMod() {
    return {
        'rspolymorph_grid_node': {
            'target': {'type': 'CLASS', 'name': NODE},
            'transformer': transformNode
        },
        'rspolymorph_pattern_factory': {
            'target': {'type': 'CLASS', 'name': FACTORY},
            'transformer': transformFactory
        },
        'rspolymorph_grid_screen': {
            'target': {'type': 'CLASS', 'name': SCREEN},
            'transformer': transformScreen
        }
    };
}
