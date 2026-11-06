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

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.container.ContainerGrid;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Before Refined Storage 1.6 the client's own grid works out the crafting result whenever a slot of
 * its matrix arrives, and the server never sends that result: vanilla leaves crafting result slots out
 * of its slot updates. So the client has to know the selection too. It gets it when a grid opens and
 * when the selection changes.
 *
 * Author: vyrriox
 */
public final class SelectionSync {

    @SubscribeEvent
    public void onOpen(PlayerContainerEvent.Open event) {
        Container open = event.getContainer();
        if (event.getEntityPlayer() instanceof EntityPlayerMP && open instanceof ContainerGrid
                && ((ContainerGrid) open).getGrid() instanceof NetworkNodeGrid) {
            send((EntityPlayerMP) event.getEntityPlayer(), open.windowId, (NetworkNodeGrid) ((ContainerGrid) open).getGrid());
        }
    }

    static void send(EntityPlayerMP player, int windowId, NetworkNodeGrid node) {
        ResourceLocation selected = ((IRsPolymorphGrid) node).rspolymorph$getSelected();
        RsPolymorphNetwork.channel().sendTo(new SyncSelectionMessage(windowId, selected), player);
    }
}
