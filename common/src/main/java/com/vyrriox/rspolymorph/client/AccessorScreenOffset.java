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
package com.vyrriox.rspolymorph.client;

/**
 * Duck-type interface implemented (via mixin) by {@code AbstractContainerScreen} to expose its
 * protected {@code leftPos}/{@code topPos} so the popup can be anchored in screen space without
 * subclassing. See {@code MixinAbstractContainerScreenOffset}.
 *
 * Author: vyrriox
 */
public interface AccessorScreenOffset {
    int rspolymorph$leftPos();
    int rspolymorph$topPos();
}
