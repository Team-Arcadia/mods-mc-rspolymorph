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

import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraftforge.fml.server.ServerModLoader;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * 1.15.2's only way to load the mods on a dedicated server takes the server, which only needs to exist:
 * the selftest never starts one, so it passes an instance whose constructor never ran. Its
 * end() needs a running server and loads no more mods, so it stays out.
 *
 * Author: vyrriox
 */
final class ServerLoad {

    private ServerLoad() {
    }

    static void loadMods() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        ServerModLoader.begin((DedicatedServer) ((Unsafe) field.get(null)).allocateInstance(DedicatedServer.class));
    }
}
