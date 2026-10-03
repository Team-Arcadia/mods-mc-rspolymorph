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

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.platform.GridSelectionData;
import com.vyrriox.rspolymorph.network.SelectRecipePacket;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * NeoForge entrypoint for RS Polymorph. Registers the {@code selected_recipe} data component
 * (stamped on printed patterns), the per-grid selection {@code AttachmentType}, and the C2S
 * selection payload, then wires the shared {@link RsPolymorph} core. All gameplay logic lives
 * in the {@code common} module; this class only performs NeoForge-specific registration.
 *
 * Author: vyrriox
 */
@Mod(RsPolymorph.MOD_ID)
public final class RsPolymorphNeoForge {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RsPolymorph.MOD_ID);

    private static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> SELECTED_RECIPE_COMPONENT =
            DATA_COMPONENT_TYPES.register("selected_recipe", () ->
                    DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .build());

    // Per-grid recipe selection, persisted on the grid block entity's NBT via the attachment.
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, RsPolymorph.MOD_ID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Map<ResourceLocation, ResourceLocation>>> GRID_SELECTION =
            ATTACHMENT_TYPES.register("grid_selection", () ->
                    AttachmentType.builder(GridSelectionData::empty)
                            .serialize(GridSelectionData.CODEC)
                            .build());

    public RsPolymorphNeoForge(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
        ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerPayloads);
    }

    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                SelectRecipePacket.TYPE,
                SelectRecipePacket.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        SelectRecipePacket.applyOnServer(player, packet.recipeId());
                    }
                })
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("RS Polymorph (NeoForge) initializing...");

        // Inject the registered data component into the loader-agnostic core.
        RsPolymorph.setSelectedRecipeComponent(SELECTED_RECIPE_COMPONENT.get());

        // Bind the attachment-backed grid selection store used by NeoForgeGridRecipeStore.
        NeoForgeGridRecipeStore.bind(GRID_SELECTION);

        // Client widget registration lives in a separate class to keep client-only type
        // references out of this class's constant pool / lambda methods on a dedicated server.
        if (FMLEnvironment.dist.isClient()) {
            com.vyrriox.rspolymorph.client.ClientSetup.init();
        }
    }
}
