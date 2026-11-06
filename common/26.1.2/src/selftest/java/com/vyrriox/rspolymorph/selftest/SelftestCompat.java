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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;

/**
 * The calls of {@link Selftest} that differ between Minecraft versions, for 26.x.
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
        GLFW.glfwSetWindowSize(mc.getWindow().handle(), width, height);
    }

    static void applyGuiScale(Minecraft mc) {
        mc.resizeGui();
    }

    static void createFlatWorld(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false), true,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20261106L, false, false),
                access -> access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions(),
                mc.screen);
    }

    static void disconnect(Minecraft mc) {
        mc.disconnectFromWorld(Component.translatable("menu.savingLevel"));
        mc.setScreen(new TitleScreen());
    }

    static boolean click(Screen screen, double x, double y, int button) {
        MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, 0));
        boolean consumed = screen.mouseClicked(event, false);
        screen.mouseReleased(event);
        return consumed;
    }

    static boolean key(Screen screen, int key, int scanCode) {
        return screen.keyPressed(new KeyEvent(key, scanCode, 0));
    }

    static void pickup(Minecraft mc, int containerId, int slot, int button) {
        mc.gameMode.handleContainerInput(containerId, slot, button, ContainerInput.PICKUP, mc.player);
    }

    static void quickMove(Minecraft mc, int containerId, int slot) {
        mc.gameMode.handleContainerInput(containerId, slot, 0, ContainerInput.QUICK_MOVE, mc.player);
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), 1, message -> {
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

    static String ingredient(String item) {
        return "\"" + item + "\"";
    }

    static String packMcmeta() {
        return "{\"pack\":{\"min_format\":101,\"max_format\":101,\"description\":\"RS Polymorph selftest recipes\"}}";
    }
}
