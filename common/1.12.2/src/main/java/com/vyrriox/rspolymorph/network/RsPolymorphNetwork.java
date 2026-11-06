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
package com.vyrriox.rspolymorph.network;

import com.vyrriox.rspolymorph.RsPolymorph;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The mod's single network channel. Called once from the mod's pre-init.
 *
 * Author: vyrriox
 */
public final class RsPolymorphNetwork {

    private static SimpleNetworkWrapper channel;

    private RsPolymorphNetwork() {
    }

    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(RsPolymorph.MOD_ID);
        channel.registerMessage(SelectRecipeMessage.Handler.class, SelectRecipeMessage.class, 0, Side.SERVER);
    }

    public static SimpleNetworkWrapper channel() {
        return channel;
    }
}
