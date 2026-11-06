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
import org.spongepowered.asm.mixin.injection.Redirect;


/**
 * Starts the dedicated server selftest. Minecraft 1.18.1 has no factory on this class yet, and its
 * constructor refuses an empty test list before the first server step, so the hook sits on that check.
 *
 * Author: vyrriox
 */
@Mixin(GameTestServer.class)
public abstract class MixinGameTestServerSelftest {

    @Redirect(method = "<init>*", at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z", remap = false))
    private boolean rspolymorph$selftestFromConstructor(java.util.Collection<?> batches) {
        ServerSelftest.onGameTestServerCreate();
        return batches.isEmpty();
    }
}
