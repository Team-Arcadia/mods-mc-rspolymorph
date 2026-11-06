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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;

/**
 * Server to client: the selection of the grid in this window, an empty string for none.
 *
 * Author: vyrriox
 */
public final class SyncSelectionMessage implements IMessage {

    /** Set by the client when it starts, so that this class names no client class. */
    private static volatile BiConsumer<Integer, ResourceLocation> receiver;

    private int windowId;
    private String recipeId = "";

    /** For FML, which fills the fields from the wire. */
    public SyncSelectionMessage() {
    }

    SyncSelectionMessage(int windowId, @Nullable ResourceLocation recipeId) {
        this.windowId = windowId;
        this.recipeId = recipeId == null ? "" : recipeId.toString();
    }

    public static void setReceiver(BiConsumer<Integer, ResourceLocation> value) {
        receiver = value;
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

    public static final class Handler implements IMessageHandler<SyncSelectionMessage, IMessage> {
        @Override
        public IMessage onMessage(SyncSelectionMessage message, MessageContext ctx) {
            BiConsumer<Integer, ResourceLocation> target = receiver;
            if (target != null) {
                target.accept(message.windowId, message.recipeId.isEmpty() ? null : new ResourceLocation(message.recipeId));
            }
            return null;
        }
    }
}
