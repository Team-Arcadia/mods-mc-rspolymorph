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
package com.vyrriox.rspolymorph.selftest.mixin;

import com.vyrriox.rspolymorph.selftest.Selftest;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives the selftest from the end of the client tick, so that it needs no loader event.
 *
 * Author: vyrriox
 */
@Mixin(Minecraft.class)
public abstract class MixinMinecraftSelftest {

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void rspolymorph$selftestTick(CallbackInfo ci) {
        Selftest.onClientTick();
    }
}
