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
package com.vyrriox.rspolymorph.platform;

import net.minecraft.resources.ResourceLocation;

/**
 * Loader-agnostic networking abstraction.
 *
 * Only one direction is needed: the client tells the server which Polymorph recipe
 * the player selected. NeoForge implements this with {@code PacketDistributor}; Fabric
 * with {@code ClientPlayNetworking}. The payload type registration and the server-side
 * receiver are wired up by each loader's entrypoint.
 *
 * Author: vyrriox
 */
public interface NetworkPlatform {

    /**
     * Sends the recipe selection from the client to the server over the C2S channel.
     * Must be called on the client only.
     */
    void sendSelectToServer(ResourceLocation recipeId);
}
