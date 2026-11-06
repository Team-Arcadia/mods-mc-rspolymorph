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
package com.vyrriox.rspolymorph.forge;

import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.platform.Services;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Forge entry point (also loaded by NeoForge 47.1 on 1.20.1). Only wires the packet channel: the
 * logic and the mixins live in common/1.20.1. Must never reference a client class.
 *
 * Author: vyrriox
 */
@Mod(RsPolymorph.MOD_ID)
public final class RsPolymorphForge {

    public RsPolymorphForge() {
        Services.setNetwork(new ForgeNetwork());
        // Proof runs only: mixin targets load lazily, this applies every mixin now so a failure shows.
        if (Boolean.getBoolean("rspolymorph.auditMixins")) {
            MixinEnvironment.getCurrentEnvironment().audit();
        }
    }
}
