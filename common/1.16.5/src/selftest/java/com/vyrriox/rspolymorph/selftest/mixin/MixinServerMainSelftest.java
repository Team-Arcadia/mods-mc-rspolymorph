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

import com.vyrriox.rspolymorph.selftest.ServerSelftest;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Takes over the dedicated server at the first line of its main method when the server selftest is on.
 * 1.16.5 has no game test launch, and this keeps the same promise: it never gets as far as the code
 * that reads eula.txt, and no server ever starts.
 *
 * Author: vyrriox
 */
@Mixin(Main.class)
public abstract class MixinServerMainSelftest {

    @Inject(method = "main", at = @At("HEAD"), remap = false)
    private static void rspolymorph$selftest(String[] args, CallbackInfo ci) {
        ServerSelftest.onServerMain();
    }
}
