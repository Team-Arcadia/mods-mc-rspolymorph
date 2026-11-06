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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.BlockHitResult;

/**
 * What the selftest cannot call the same way on the versions of this fork: Minecraft 1.18.2.
 * Worlds are created from Minecraft itself, and the flat generator needs the structure sets.
 *
 * Author: vyrriox
 */
final class SelftestWorlds {

    private static final long SEED = 20261106L;

    private SelftestWorlds() {
    }

    static void setGuiScale(Minecraft mc, int scale) {
        mc.options.guiScale = scale;
    }

    static void createFlatWorld(Minecraft mc, String name) {
        LevelSettings settings = new LevelSettings(name, GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                new GameRules(), DataPackConfig.DEFAULT);
        RegistryAccess.Frozen registries = RegistryAccess.builtinCopy().freeze();
        Registry<Biome> biomes = registries.registryOrThrow(Registry.BIOME_REGISTRY);
        Registry<StructureSet> structures = registries.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
        WorldGenSettings generation = new WorldGenSettings(SEED, false, false, WorldGenSettings.withOverworld(
                registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(registries, SEED),
                new FlatLevelSource(structures, FlatLevelGeneratorSettings.getDefault(biomes, structures))));
        mc.createLevel(name, settings, registries, generation);
    }

    static void reopenWorld(Minecraft mc, String name) {
        mc.loadLevel(name);
    }

    static void useItemOn(Minecraft mc, InteractionHand hand, BlockHitResult hit) {
        mc.gameMode.useItemOn(mc.player, mc.level, hand, hit);
    }

    static void command(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performCommand(source, command);
    }

    static int packFormat() {
        return 9;
    }
}
