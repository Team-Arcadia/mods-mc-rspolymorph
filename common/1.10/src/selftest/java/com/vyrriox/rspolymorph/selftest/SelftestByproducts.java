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

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import refinedstorage.item.ItemPattern;

/**
 * The byproducts a pattern keeps, the part of it that vanilla's first match would get wrong.
 *
 * Author: vyrriox
 */
final class SelftestByproducts {

    private SelftestByproducts() {
    }

    static void check(String name, ItemStack pattern, Item expected) {
        ItemStack[] byproducts = ItemPattern.getByproducts(pattern);
        boolean found = false;
        for (ItemStack byproduct : byproducts) {
            found |= Selftest.isItem(byproduct, expected);
        }
        Selftest.check(name, found && byproducts.length == 1, java.util.Arrays.toString(byproducts));
    }
}
