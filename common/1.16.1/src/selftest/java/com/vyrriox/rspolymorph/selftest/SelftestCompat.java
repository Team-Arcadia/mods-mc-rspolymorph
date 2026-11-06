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
import net.minecraft.client.MouseHelper;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.world.gen.FlatChunkGenerator;
import net.minecraft.world.gen.FlatGenerationSettings;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.lwjgl.glfw.GLFW;

/**
 * Everything {@link Selftest} calls that differs between Minecraft versions, for 1.16.1. Other
 * versions of the Java 8 line fork this one class.
 *
 * Author: vyrriox
 */
final class SelftestCompat {

    private static double pointerX;
    private static double pointerY;

    private SelftestCompat() {
    }

    /** 1.16 has no accessibility screen on the first launch. */
    static void skipOnboarding(Minecraft mc) {
    }

    static void mute(Minecraft mc) {
        mc.options.setSoundCategoryVolume(SoundCategory.MASTER, 0.0f);
    }

    static void setWindowSize(Minecraft mc, int width, int height) {
        GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), width, height);
    }

    static void setGuiScale(Minecraft mc, int scale) {
        mc.options.guiScale = scale;
        mc.resizeDisplay();
    }

    static void createFlatWorld(Minecraft mc, String name) {
        WorldSettings settings = new WorldSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), DatapackCodec.DEFAULT);
        long seed = 20261106L;
        mc.createLevel(name, settings, IDynamicRegistries.builtin(), new DimensionGeneratorSettings(seed, false, false,
                DimensionGeneratorSettings.withOverworld(DimensionType.defaultDimensions(seed),
                        new FlatChunkGenerator(FlatGenerationSettings.getDefault()))));
    }

    static void reopenWorld(Minecraft mc, String name) {
        mc.loadLevel(name);
    }

    static void disconnect(Minecraft mc) {
        mc.level.disconnect();
        mc.clearLevel(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
        mc.setScreen(new MainMenuScreen());
    }

    static void useOn(Minecraft mc, BlockPos pos) {
        mc.gameMode.useItemOn(mc.player, mc.level, Hand.MAIN_HAND,
                new BlockRayTraceResult(Vector3d.atCenterOf(pos), Direction.UP, pos, false));
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
        ScreenShotHelper.grab(mc.gameDirectory, name + ".png", mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                mc.getMainRenderTarget(), message -> {
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
            // SRG names: Forge translates them to the development names when it runs from there.
            ObfuscationReflectionHelper.findField(MouseHelper.class, "field_198040_e").setDouble(mc.mouseHandler, pointerX);
            ObfuscationReflectionHelper.findField(MouseHelper.class, "field_198041_f").setDouble(mc.mouseHandler, pointerY);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot move the pointer", e);
        }
    }

    static Slot hoveredSlot(ContainerScreen<?> screen) {
        try {
            return (Slot) ObfuscationReflectionHelper.findField(ContainerScreen.class, "field_147006_u").get(screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read the hovered slot", e);
        }
    }

    /** Whether the slot is backed by this item handler. */
    static boolean isSlotOf(Slot slot, IItemHandler handler) {
        return slot instanceof SlotItemHandler && ((SlotItemHandler) slot).getItemHandler() == handler;
    }

    static World level(ServerPlayerEntity player) {
        return player.level;
    }

    static void command(MinecraftServer server, String command) {
        server.getCommands().performCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    /** A shaped recipe of two side by side ingredients. */
    static String shapedPair(String ingredient, String result, int count) {
        return "{\"type\":\"minecraft:crafting_shaped\",\"pattern\":[\"XX\"],\"key\":{\"X\":{\"item\":\""
                + ingredient + "\"}},\"result\":{\"item\":\"" + result + "\",\"count\":" + count + "}}";
    }

    /** The folder of the recipes in a data pack's namespace, renamed to "recipe" in 1.21. */
    static String recipeFolder() {
        return "recipes";
    }

    static String packMcmeta() {
        return "{\"pack\":{\"pack_format\":5,\"description\":\"RS Polymorph selftest recipes\"}}";
    }
}
