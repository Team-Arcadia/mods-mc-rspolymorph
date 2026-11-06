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
package com.vyrriox.rspolymorph.client;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.container.ContainerGrid;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.network.RequestSelectionMessage;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import com.vyrriox.rspolymorph.network.SyncSelectionMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Puts the server's selection on the client's grid and works its result out again. A selection for a
 * window that is no longer open is dropped.
 *
 * Author: vyrriox
 */
@SideOnly(Side.CLIENT)
public final class SelectionSyncClient {

    /** The grid container the selection was asked for: one identity check per client tick. */
    private Container asked;

    private SelectionSyncClient() {
    }

    /** FML gives the container its window id after the screen opened, so this waits for the id. */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Container open = mc.player == null ? null : mc.player.openContainer;
        if (!(open instanceof ContainerGrid)) {
            asked = null;
        } else if (open != asked && open.windowId != 0 && ((ContainerGrid) open).getGrid() instanceof NetworkNodeGrid) {
            asked = open;
            RsPolymorphNetwork.channel().sendToServer(new RequestSelectionMessage(open.windowId));
        }
    }

    public static void register() {
        SyncSelectionMessage.setReceiver((window, recipe) -> {
            Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> apply(mc, window, recipe));
        });
        MinecraftForge.EVENT_BUS.register(new SelectionSyncClient());
    }

    private static void apply(Minecraft mc, int window, ResourceLocation recipe) {
        Container open = mc.player == null ? null : mc.player.openContainer;
        if (open instanceof ContainerGrid && open.windowId == window
                && ((ContainerGrid) open).getGrid() instanceof NetworkNodeGrid) {
            NetworkNodeGrid node = (NetworkNodeGrid) ((ContainerGrid) open).getGrid();
            ((IRsPolymorphGrid) node).rspolymorph$setSelected(recipe);
            node.onCraftingMatrixChanged();
        }
    }
}
