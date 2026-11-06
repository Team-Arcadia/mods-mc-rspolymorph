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
import net.minecraft.gametest.framework.GameTestServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Starts the dedicated server selftest where every loader's game test launch ends up. NeoForge goes
 * through the factory, which refuses an empty test list; Forge builds the server with its constructor,
 * so the first step of the server is hooked too. No loader event and no registered test needed.
 *
 * Author: vyrriox
 */
@Mixin(GameTestServer.class)
public abstract class MixinGameTestServerSelftest {

    @Inject(method = "create", at = @At("HEAD"))
    private static void rspolymorph$selftestFromFactory(CallbackInfoReturnable<GameTestServer> cir) {
        ServerSelftest.onGameTestServerCreate();
    }

    @Inject(method = "initServer", at = @At("HEAD"))
    private void rspolymorph$selftestFromInit(CallbackInfoReturnable<Boolean> cir) {
        ServerSelftest.onGameTestServerCreate();
    }
}
