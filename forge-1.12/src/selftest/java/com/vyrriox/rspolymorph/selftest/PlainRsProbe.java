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

import com.raoulvdberge.refinedstorage.RSBlocks;
import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.block.BlockGrid;
import com.raoulvdberge.refinedstorage.inventory.ItemHandlerBase;
import com.raoulvdberge.refinedstorage.tile.TileNode;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.coremod.GridTransformer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Tells what Refined Storage 1.5.28 does on its own when a world with a pattern in the output slot of
 * a Pattern Grid reloads. Touches the grid only with Refined Storage's own methods, and opens no
 * screen. {@code -Pselftest -Pprobe} runs it with this mod's transformers, adding {@code -Pplain_rs}
 * without them. Prints {@code RSP-SELFTEST PROBE} lines.
 *
 * Author: vyrriox
 */
public final class PlainRsProbe {

    private static final String WORLD = "rsp-probe";

    private final Minecraft mc = Minecraft.getMinecraft();
    private BlockPos at;
    private volatile Runnable onTick;

    public static void start() {
        PlainRsProbe probe = new PlainRsProbe();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(probe);
        Thread thread = new Thread(probe::run, "rsp-probe");
        thread.setDaemon(true);
        thread.start();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Runnable step = onTick;
        if (step != null && event.phase == TickEvent.Phase.END) {
            onTick = null;
            step.run();
        }
    }

    private void run() {
        try {
            while (!(mc.currentScreen instanceof GuiMainMenu)) {
                Thread.sleep(200L);
            }
            Path saved = new File(mc.gameDir, "saves/" + WORLD).toPath();
            if (Files.exists(saved)) {
                try (Stream<Path> files = Files.walk(saved)) {
                    files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
            }
            mc.addScheduledTask(() -> {
                mc.gameSettings.pauseOnLostFocus = false;
            }).get();
            join(true);
            server(s -> {
                WorldServer world = s.getWorld(0);
                at = world.getSpawnPoint().up(8);
                world.setBlockState(at, RSBlocks.GRID.getDefaultState().withProperty(BlockGrid.TYPE, GridType.PATTERN));
            });
            Thread.sleep(500L);
            server(s -> {
                print("transformers=" + GridTransformer.applied() + " node_patched=" + (node(s) instanceof IRsPolymorphGrid));
                NetworkNodeGrid n = node(s);
                ((ItemHandlerBase) n.getPatterns()).setStackInSlot(0, new ItemStack(RSItems.PATTERN, 8));
                n.setProcessingPattern(true);
                n.getProcessingMatrix().setStackInSlot(0, new ItemStack(Blocks.DIRT));
                n.getProcessingMatrix().setStackInSlot(9, new ItemStack(Blocks.STONE));
                n.onCreatePattern();
                print("printed_output=" + n.getPatterns().getStackInSlot(1));
                n.setProcessingPattern(false);
                n.getProcessingMatrix().setStackInSlot(0, ItemStack.EMPTY);
                n.getProcessingMatrix().setStackInSlot(9, ItemStack.EMPTY);
                fill(n);
                print("before_reload_with_pattern " + matrix(n));
            });
            reload();
            server(s -> {
                NetworkNodeGrid n = node(s);
                print("after_reload_with_pattern " + matrix(n));
                ((ItemHandlerBase) n.getPatterns()).setStackInSlot(1, ItemStack.EMPTY);
                fill(n);
                print("before_reload_output_emptied " + matrix(n));
            });
            reload();
            server(s -> print("after_reload_output_emptied " + matrix(node(s))));
            print("DONE pass=0 fail=0");
        } catch (Throwable t) {
            t.printStackTrace();
            print("DONE pass=0 fail=1");
        }
        try {
            leave();
            mc.addScheduledTask(mc::shutdown).get(60, TimeUnit.SECONDS);
        } catch (Throwable t) {
            FMLCommonHandler.instance().exitJava(0, true);
        }
    }

    private static void fill(NetworkNodeGrid n) {
        n.getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.GRAVEL));
        n.getCraftingMatrix().setInventorySlotContents(1, new ItemStack(Items.FLINT));
    }

    private static String matrix(NetworkNodeGrid n) {
        return "output=" + n.getPatterns().getStackInSlot(1) + " processing=" + n.isProcessingPattern()
                + " slot0=" + n.getCraftingMatrix().getStackInSlot(0) + " slot1=" + n.getCraftingMatrix().getStackInSlot(1);
    }

    private NetworkNodeGrid node(MinecraftServer s) {
        return (NetworkNodeGrid) ((TileNode<?>) s.getWorld(0).getTileEntity(at)).getNode();
    }

    private void reload() throws Exception {
        leave();
        while (mc.world != null || mc.getIntegratedServer() != null) {
            Thread.sleep(100L);
        }
        join(false);
        Thread.sleep(500L);
    }

    private void join(boolean create) throws Exception {
        mc.addScheduledTask(() -> {
            WorldSettings settings = create ? new WorldSettings(12345L, GameType.CREATIVE, false, false, WorldType.FLAT) : null;
            mc.launchIntegratedServer(WORLD, WORLD, settings);
        }).get(60, TimeUnit.SECONDS);
        long end = System.currentTimeMillis() + 120000L;
        while (mc.world == null || mc.player == null || mc.getIntegratedServer() == null
                || mc.getIntegratedServer().getPlayerList().getCurrentPlayerCount() == 0 || mc.currentScreen != null) {
            if (System.currentTimeMillis() > end) {
                throw new IllegalStateException("no world");
            }
            Thread.sleep(100L);
        }
    }

    private void leave() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        onTick = () -> {
            try {
                if (mc.world != null) {
                    mc.loadWorld(null);
                }
                mc.displayGuiScreen(new GuiMainMenu());
            } finally {
                done.countDown();
            }
        };
        if (!done.await(90, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the world did not close");
        }
    }

    private interface Step {
        void run(MinecraftServer server) throws Exception;
    }

    private void server(Step step) throws Exception {
        MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
        s.callFromMainThread(() -> {
            step.run(s);
            return null;
        }).get(60, TimeUnit.SECONDS);
    }

    private static void print(String text) {
        new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true).println("RSP-SELFTEST PROBE " + text);
    }
}
