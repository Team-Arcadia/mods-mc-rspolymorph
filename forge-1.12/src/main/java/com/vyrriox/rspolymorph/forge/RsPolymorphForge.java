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
package com.vyrriox.rspolymorph.forge;

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Forge entrypoint: loader wiring only, with every client class behind the proxy. The version comes
 * from mcmod.info.
 *
 * Author: vyrriox
 */
@Mod(
        modid = RsPolymorph.MOD_ID,
        name = "RS Polymorph",
        useMetadata = true,
        acceptedMinecraftVersions = "[1.12,1.12.1]",
        dependencies = "required-after:refinedstorage@[1.5.28,)"
)
public final class RsPolymorphForge {

    @SidedProxy(
            clientSide = "com.vyrriox.rspolymorph.forge.ClientProxy",
            serverSide = "com.vyrriox.rspolymorph.forge.CommonProxy"
    )
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        RsPolymorphNetwork.register();
        proxy.registerClientEvents();
    }
}
