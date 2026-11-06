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
package com.vyrriox.rspolymorph.platform;

import net.minecraft.resources.ResourceLocation;

/**
 * The packet channel, implemented by the loader project.
 *
 * Author: vyrriox
 */
public interface NetworkPlatform {

    /** Client side: asks the server to select a recipe on the grid open in this container. */
    void sendSelectRecipe(int containerId, ResourceLocation recipeId);
}
