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
package com.vyrriox.rspolymorph.neoforge;

import com.vyrriox.rspolymorph.network.SelectRecipePacket;
import com.vyrriox.rspolymorph.platform.NetworkPlatform;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * NeoForge implementation of {@link NetworkPlatform}, resolved by the common code through
 * {@code ServiceLoader} (see {@code META-INF/services}).
 *
 * In NeoForge 26.x the client→server send moved from {@code PacketDistributor.sendToServer} to
 * the client-only {@code ClientPacketDistributor.sendToServer}. This class is only ever called
 * on the client (from {@code RsGridRecipeWidget.selectRecipe}).
 *
 * Author: vyrriox
 */
public final class NeoForgeNetworkPlatform implements NetworkPlatform {

    @Override
    public void sendSelectToServer(Identifier recipeId) {
        ClientPacketDistributor.sendToServer(new SelectRecipePacket(recipeId));
    }
}
