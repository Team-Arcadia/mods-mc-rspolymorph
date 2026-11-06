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
package com.vyrriox.rspolymorph;

/**
 * Tells who owns recipe selection on Refined Storage grids. Polymorph and Polymorph+ ship their own
 * selector for these grids, which redirects the recipe lookup in the node and draws its own button.
 * When that integration is present this mod leaves selection to it and only keeps the pattern
 * features. Plain Java with no game import, and it looks for a resource instead of loading the class:
 * it runs from the mixin config plugin, before the game may load anything.
 *
 * Author: vyrriox
 */
public final class GridOwnership {

    /** Polymorph+ keeps the later package, the original Polymorph of this Minecraft has the older one. */
    private static final String[] POLYMORPH_MODULES = {
            "com/illusivesoulworks/polymorph/common/integration/refinedstorage/RefinedStorageModule.class",
            "top/theillusivec4/polymorph/common/integration/refinedstorage/RefinedStorageModule.class"
    };

    private static final boolean POLYMORPH_OWNS_GRIDS = detect();

    private GridOwnership() {
    }

    /** @return true when Polymorph or Polymorph+ handles selection on grids, so this mod stays out */
    public static boolean polymorphOwnsGrids() {
        return POLYMORPH_OWNS_GRIDS;
    }

    private static boolean detect() {
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        ClassLoader own = GridOwnership.class.getClassLoader();
        for (String module : POLYMORPH_MODULES) {
            if ((context != null && context.getResource(module) != null)
                    || (own != null && own.getResource(module) != null)) {
                return true;
            }
        }
        return false;
    }
}
