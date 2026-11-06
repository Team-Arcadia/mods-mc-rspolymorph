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
 * Selftest builds only. Takes over the dedicated server at the first line of its main method: this Minecraft has
 * no game test launch, and this never gets as far as the code that reads eula.txt. "main" is not obfuscated.
 *
 * Author: vyrriox
 */
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var FieldNode = Java.type('org.objectweb.asm.tree.FieldNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return {
        'rspolymorph_selftest_server_main': {
            'target': {'type': 'CLASS', 'name': 'net/minecraft/server/MinecraftServer'},
            'transformer': function (classNode) {
                for (var i = 0; i < classNode.methods.size(); i++) {
                    var candidate = classNode.methods.get(i);
                    if (candidate.name === 'main' && candidate.desc === '([Ljava/lang/String;)V') {
                        candidate.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            'com/vyrriox/rspolymorph/selftest/ServerSelftest', 'onServerMain', '()V', false));
                        classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC,
                            'rspolymorph$hook_main', 'Z', null, true));
                        return classNode;
                    }
                }
                throw new Error('rspolymorph: no main method on ' + classNode.name);
            }
        }
    };
}
