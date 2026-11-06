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
package com.vyrriox.rspolymorph.mixin;

import com.vyrriox.rspolymorph.GridOwnership;
import org.objectweb.asm.tree.ClassNode;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Leaves out the selection mixins when Polymorph owns the grids; the pattern mixins always apply.
 *
 * Author: vyrriox
 */
public final class RsPolymorphMixinPlugin implements IMixinConfigPlugin {

    private static final Set<String> SELECTION_MIXINS = new HashSet<>(Arrays.asList(
            "com.vyrriox.rspolymorph.mixin.MixinGridNetworkNode",
            "com.vyrriox.rspolymorph.mixin.MixinGridScreen"
    ));

    @Override
    public void onLoad(String mixinPackage) {
        LogManager.getLogger("rspolymorph").info(GridOwnership.polymorphOwnsGrids()
                ? "Refined Storage grid selection is owned by Polymorph; RS Polymorph only keeps its pattern features"
                : "Refined Storage grid selection is owned by RS Polymorph");
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !GridOwnership.polymorphOwnsGrids() || !SELECTION_MIXINS.contains(mixinClassName);
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
