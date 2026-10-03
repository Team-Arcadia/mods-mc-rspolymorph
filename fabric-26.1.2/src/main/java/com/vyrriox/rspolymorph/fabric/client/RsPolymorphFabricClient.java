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
package com.vyrriox.rspolymorph.fabric.client;

import com.vyrriox.rspolymorph.client.ClientSetup;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint. Registers the Polymorph grid widget on the client dist only —
 * the equivalent of the NeoForge {@code FMLEnvironment.dist.isClient()} guard.
 *
 * Author: vyrriox
 */
public final class RsPolymorphFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
    }
}
