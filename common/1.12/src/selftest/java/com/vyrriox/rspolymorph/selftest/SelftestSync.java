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
package com.vyrriox.rspolymorph.selftest;

import com.mojang.authlib.GameProfile;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.vyrriox.rspolymorph.network.SelectionSync;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The send list of the selection, with fake players next to the real one: one has the same grid open,
 * one has no grid open. A second real client is not part of this test.
 *
 * Author: vyrriox
 */
public final class SelftestSync {

    private SelftestSync() {
    }

    /** @return what is wrong, or null */
    public static String viewers(WorldServer world, EntityPlayerMP real, Object grid, Object otherGrid) {
        FakePlayer sameGrid = new FakePlayer(world, new GameProfile(UUID.randomUUID(), "rsp-viewer"));
        sameGrid.openContainer = real.openContainer;
        FakePlayer noGrid = new FakePlayer(world, new GameProfile(UUID.randomUUID(), "rsp-elsewhere"));
        noGrid.openContainer = noGrid.inventoryContainer;
        List<EntityPlayerMP> players = new ArrayList<>(Arrays.asList(real, sameGrid, noGrid));

        List<EntityPlayerMP> viewers = SelectionSync.viewers((NetworkNodeGrid) grid, players);
        if (!viewers.equals(Arrays.asList(real, sameGrid))) {
            return "viewers of the grid: " + viewers;
        }
        if (!SelectionSync.viewers((NetworkNodeGrid) otherGrid, players).isEmpty()) {
            return "viewers of another grid: " + SelectionSync.viewers((NetworkNodeGrid) otherGrid, players);
        }
        // The fake viewer has no connection: skipped, not a crash.
        int sent = SelectionSync.sendToViewers((NetworkNodeGrid) grid, players);
        return sent == 1 ? null : "messages sent: " + sent;
    }
}
