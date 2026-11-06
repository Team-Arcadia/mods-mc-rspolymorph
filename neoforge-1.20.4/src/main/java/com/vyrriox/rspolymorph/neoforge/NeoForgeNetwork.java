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
import com.vyrriox.rspolymorph.network.SelectRecipeHandler;
import com.vyrriox.rspolymorph.platform.NetworkPlatform;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

/**
 * The packet channel: the one client to server message of the mod. The reader never throws, so a
 * malformed or spoofed packet is dropped by {@link SelectRecipeHandler} instead of kicking anyone.
 *
 * Author: vyrriox
 */
final class NeoForgeNetwork implements NetworkPlatform {

    private static final String PROTOCOL = "1";
    /** Longest possible resource location: namespace, separator and path. */
    private static final int MAX_ID_LENGTH = 512;
    private static final ResourceLocation SELECT_RECIPE = new ResourceLocation(RsPolymorph.MOD_ID, "select_recipe");

    void register(RegisterPayloadHandlerEvent event) {
        event.registrar(RsPolymorph.MOD_ID)
                .versioned(PROTOCOL)
                .play(SELECT_RECIPE, SelectRecipePayload::read, handlers -> handlers.server(NeoForgeNetwork::handle));
    }

    /** Payloads arrive on the network thread: the menu and the node are only touched on the server thread. */
    private static void handle(SelectRecipePayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> context.player().ifPresent(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                SelectRecipeHandler.handle(serverPlayer, payload.containerId(), payload.recipeId());
            }
        }));
    }

    @Override
    public void sendSelectRecipe(int containerId, ResourceLocation recipeId) {
        PacketDistributor.SERVER.noArg().send(new SelectRecipePayload(containerId, recipeId));
    }

    private record SelectRecipePayload(int containerId, ResourceLocation recipeId) implements CustomPacketPayload {

        static SelectRecipePayload read(FriendlyByteBuf buf) {
            try {
                int containerId = buf.readVarInt();
                return new SelectRecipePayload(containerId, ResourceLocation.tryParse(buf.readUtf(MAX_ID_LENGTH)));
            } catch (RuntimeException malformed) {
                buf.skipBytes(buf.readableBytes());
                return new SelectRecipePayload(-1, null);
            }
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeVarInt(containerId);
            buf.writeUtf(recipeId.toString(), MAX_ID_LENGTH);
        }

        @Override
        public ResourceLocation id() {
            return SELECT_RECIPE;
        }
    }
}
