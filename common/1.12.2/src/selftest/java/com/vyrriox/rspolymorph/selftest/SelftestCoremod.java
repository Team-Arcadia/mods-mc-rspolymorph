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

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

/**
 * The dedicated server half of the selftest. With -Drspolymorph.selftest.server=true it takes over at
 * the first line of the server's main method, before eula.txt is even read, and never starts a server
 * (see {@link ServerSelftest}).
 *
 * Author: vyrriox
 */
@IFMLLoadingPlugin.Name("RS Polymorph selftest")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1002)
@IFMLLoadingPlugin.TransformerExclusions("com.vyrriox.rspolymorph.selftest.SelftestCoremod")
public final class SelftestCoremod implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.vyrriox.rspolymorph.selftest.SelftestCoremod$Transformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    public static final class Transformer implements IClassTransformer {
        @Override
        public byte[] transform(String name, String transformedName, byte[] basicClass) {
            if (basicClass == null || !"net.minecraft.server.MinecraftServer".equals(transformedName)
                    || !Boolean.getBoolean("rspolymorph.selftest.server")) {
                return basicClass;
            }
            ClassNode node = new ClassNode();
            new ClassReader(basicClass).accept(node, 0);
            for (MethodNode method : node.methods) {
                if ("main".equals(method.name) && "([Ljava/lang/String;)V".equals(method.desc)) {
                    method.instructions.insert(new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "com/vyrriox/rspolymorph/selftest/ServerSelftest", "run", "()V", false));
                }
            }
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            return writer.toByteArray();
        }
    }
}
