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
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server: a grid just opened in this window, send its selection. The answer only ever
 * concerns the container the server itself has open for this player.
 *
 * Author: vyrriox
 */
public final class RequestSelectionMessage implements IMessage {

    private int windowId;

    /** For FML, which fills the fields from the wire. */
    public RequestSelectionMessage() {
    }

    public RequestSelectionMessage(int windowId) {
        this.windowId = windowId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
    }

    public static final class Handler implements IMessageHandler<RequestSelectionMessage, IMessage> {
        @Override
        public IMessage onMessage(RequestSelectionMessage message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            int window = message.windowId;
            player.getServerWorld().addScheduledTask(() -> SelectionSync.answer(player, window));
            return null;
        }
    }
}
