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

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

/**
 * Everything {@link Selftest} calls that differs between Minecraft versions, for 1.20.1. Other
 * versions fork this one class, in {@code common/<version>/src/selftest}.
 *
 * Author: vyrriox
 */
final class SelftestCompat {

    private static double pointerX;
    private static double pointerY;

    private SelftestCompat() {
    }

    static void skipOnboarding(Minecraft mc) {
        mc.options.onboardAccessibility = false;
    }

    static void setWindowSize(Minecraft mc, int width, int height) {
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), width, height);
    }

    static void setGuiScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
    }

    static void createFlatWorld(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20261106L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions());
    }

    static void reopenWorld(Minecraft mc, String name) {
        mc.createWorldOpenFlows().loadLevel(mc.screen, name);
    }

    static void disconnect(Minecraft mc) {
        mc.level.disconnect();
        mc.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        mc.setScreen(new TitleScreen());
    }

    static void useOn(Minecraft mc, BlockPos pos) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    static boolean click(Screen screen, double x, double y, int button) {
        boolean consumed = screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
        return consumed;
    }

    static boolean key(Screen screen, int key, int scanCode) {
        return screen.keyPressed(key, scanCode, 0);
    }

    static boolean typed(Screen screen, char typed) {
        return screen.charTyped(typed, 0);
    }

    static void pickup(Minecraft mc, int containerId, int slot, int button) {
        mc.gameMode.handleInventoryMouseClick(containerId, slot, button, ClickType.PICKUP, mc.player);
    }

    static void quickMove(Minecraft mc, int containerId, int slot) {
        mc.gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {
        });
    }

    /** Puts the pointer at a point given in GUI coordinates, held by {@link #holdPointer}. */
    static void movePointer(Minecraft mc, double guiX, double guiY) {
        pointerX = guiX * mc.getWindow().getGuiScale();
        pointerY = guiY * mc.getWindow().getGuiScale();
        holdPointer(mc);
    }

    /** Sets the pointer again: the real cursor, if it moves over the window, would overwrite it. */
    static void holdPointer(Minecraft mc) {
        try {
            for (String name : new String[]{"xpos", "ypos"}) {
                Field f = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setDouble(mc.mouseHandler, name.equals("xpos") ? pointerX : pointerY);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot move the pointer", e);
        }
    }

    /** Whether the slot is backed by this item handler. */
    static boolean isSlotOf(net.minecraft.world.inventory.Slot slot, net.minecraftforge.items.IItemHandler handler) {
        return slot instanceof net.minecraftforge.items.SlotItemHandler s && s.getItemHandler() == handler;
    }

    static Level level(ServerPlayer player) {
        return player.level();
    }

    static void command(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** A shaped recipe of two side by side ingredients. */
    static String shapedPair(String ingredient, String result, int count) {
        return "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"misc\",\"pattern\":[\"XX\"],\"key\":{\"X\":{\"item\":\""
                + ingredient + "\"}},\"result\":{\"item\":\"" + result + "\",\"count\":" + count + "}}";
    }

    /** The folder of the recipes in a data pack's namespace, renamed to "recipe" in 1.21. */
    static String recipeFolder() {
        return "recipes";
    }

    static String packMcmeta() {
        return "{\"pack\":{\"pack_format\":15,\"description\":\"RS Polymorph selftest recipes\"}}";
    }
}
