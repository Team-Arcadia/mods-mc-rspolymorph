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
import com.vyrriox.rspolymorph.network.SelectRecipeHandler;
import com.vyrriox.rspolymorph.platform.NetworkPlatform;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * The packet channel: the one client to server message of the mod. The decoder never throws, so a
 * malformed or spoofed packet is dropped by {@link SelectRecipeHandler} instead of kicking anyone.
 *
 * Author: vyrriox
 */
final class ForgeNetwork implements NetworkPlatform {

    private static final String PROTOCOL = "1";
    /** Longest possible resource location: namespace, separator and path. */
    private static final int MAX_ID_LENGTH = 512;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RsPolymorph.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    ForgeNetwork() {
        CHANNEL.registerMessage(0, SelectRecipeMessage.class,
                SelectRecipeMessage::encode,
                SelectRecipeMessage::decode,
                SelectRecipeMessage::handle);
    }

    @Override
    public void sendSelectRecipe(int containerId, ResourceLocation recipeId) {
        CHANNEL.sendToServer(new SelectRecipeMessage(containerId, recipeId));
    }

    private static final class SelectRecipeMessage {

        private final int containerId;
        private final ResourceLocation recipeId;

        SelectRecipeMessage(int containerId, ResourceLocation recipeId) {
            this.containerId = containerId;
            this.recipeId = recipeId;
        }

        static void encode(SelectRecipeMessage message, PacketBuffer buf) {
            buf.writeVarInt(message.containerId);
            buf.writeUtf(message.recipeId.toString(), MAX_ID_LENGTH);
        }

        static SelectRecipeMessage decode(PacketBuffer buf) {
            try {
                int containerId = buf.readVarInt();
                return new SelectRecipeMessage(containerId, ResourceLocation.tryParse(buf.readUtf(MAX_ID_LENGTH)));
            } catch (RuntimeException malformed) {
                buf.skipBytes(buf.readableBytes());
                return new SelectRecipeMessage(-1, null);
            }
        }

        static void handle(SelectRecipeMessage message, Supplier<NetworkEvent.Context> context) {
            NetworkEvent.Context ctx = context.get();
            ctx.enqueueWork(() -> {
                // This Forge cannot limit a message to one direction: on a client there is no sender.
                ServerPlayerEntity player = ctx.getSender();
                if (player != null) {
                    SelectRecipeHandler.handle(player, message.containerId, message.recipeId);
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}
