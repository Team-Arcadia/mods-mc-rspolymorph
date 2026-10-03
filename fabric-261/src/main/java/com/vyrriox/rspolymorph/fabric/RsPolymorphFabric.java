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

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.network.SelectRecipePacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Fabric entrypoint for RS Polymorph. Mirrors {@code RsPolymorphNeoForge}: registers the
 * {@code selected_recipe} data component, the C2S selection payload and its server receiver,
 * and wires the shared {@link RsPolymorph} core. All gameplay logic lives in {@code common}.
 *
 * Author: vyrriox
 */
public final class RsPolymorphFabric implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize() {
        LOGGER.info("RS Polymorph (Fabric) initializing...");

        // Register the selected_recipe data component under the same id as NeoForge.
        DataComponentType<Identifier> component = Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                RsPolymorph.SELECTED_RECIPE_COMPONENT_ID,
                DataComponentType.<Identifier>builder()
                        .persistent(Identifier.CODEC)
                        .build());
        RsPolymorph.setSelectedRecipeComponent(component);

        // Register the C2S payload type and its server-side receiver.
        PayloadTypeRegistry.playC2S().register(SelectRecipePacket.TYPE, SelectRecipePacket.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectRecipePacket.TYPE, (payload, context) ->
                context.server().execute(() ->
                        SelectRecipePacket.applyOnServer(context.player(), payload.recipeId())));

        // Touch the grid store so its persistent attachment is registered at startup (the Fabric
        // attachment is created in FabricGridRecipeStore's class initializer).
        com.vyrriox.rspolymorph.platform.Services.GRID_STORE.getClass();
    }
}
