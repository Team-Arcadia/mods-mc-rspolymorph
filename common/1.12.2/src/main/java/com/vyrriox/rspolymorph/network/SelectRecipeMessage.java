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

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server, for singleplayer and multiplayer alike: the window id of the open grid and the
 * chosen recipe. The server trusts neither, see {@link SelectRecipeHandler}.
 *
 * Author: vyrriox
 */
public final class SelectRecipeMessage implements IMessage {

    /** Longer than any real recipe id; a longer string is dropped before it costs anything. */
    private static final int MAX_ID_LENGTH = 512;

    private int windowId;
    private String recipeId = "";

    /** For FML, which fills the fields from the wire. */
    public SelectRecipeMessage() {
    }

    public SelectRecipeMessage(int windowId, ResourceLocation recipeId) {
        this.windowId = windowId;
        this.recipeId = recipeId.toString();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        recipeId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        ByteBufUtils.writeUTF8String(buf, recipeId);
    }

    public static final class Handler implements IMessageHandler<SelectRecipeMessage, IMessage> {
        @Override
        public IMessage onMessage(SelectRecipeMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            int window = message.windowId;
            String id = message.recipeId;
            if (id.isEmpty() || id.length() > MAX_ID_LENGTH) {
                return null;
            }
            player.getServerWorld().addScheduledTask(
                    () -> SelectRecipeHandler.handle(player, window, new ResourceLocation(id)));
            return null;
        }
    }
}
