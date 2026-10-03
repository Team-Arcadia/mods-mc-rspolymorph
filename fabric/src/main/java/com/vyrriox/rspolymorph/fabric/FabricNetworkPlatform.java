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
package com.vyrriox.rspolymorph.fabric;

import com.vyrriox.rspolymorph.network.SelectRecipePacket;
import com.vyrriox.rspolymorph.platform.NetworkPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.resources.ResourceLocation;

/**
 * Fabric implementation of {@link NetworkPlatform}, resolved by the common code through
 * {@code ServiceLoader} (see {@code META-INF/services}).
 *
 * {@code ClientPlayNetworking.send} is client-only API, but this class is only ever instantiated
 * on the client: the sole caller is {@code RsGridRecipeWidget.selectRecipe}, which runs client-side.
 *
 * Author: vyrriox
 */
public final class FabricNetworkPlatform implements NetworkPlatform {

    @Override
    public void sendSelectToServer(ResourceLocation recipeId) {
        ClientPlayNetworking.send(new SelectRecipePacket(recipeId));
    }
}
