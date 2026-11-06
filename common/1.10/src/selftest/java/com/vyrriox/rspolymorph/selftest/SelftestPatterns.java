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

import com.raoulvdberge.refinedstorage.RSItems;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import com.vyrriox.rspolymorph.client.SelftestAccess;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import com.vyrriox.rspolymorph.network.SelectRecipeMessage;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import refinedstorage.item.ItemPattern;

import java.util.List;

/**
 * The Pattern Grid checks of a Refined Storage whose patterns store their outputs and have no recipe:
 * nothing to tag and nothing to resolve, but a print keeps what the grid shows.
 *
 * Author: vyrriox
 */
final class SelftestPatterns {

    private static final String NO_RECIPE = "patterns of this Refined Storage store their outputs and have no recipe";

    private SelftestPatterns() {
    }

    static void run(Selftest t) throws Exception {
        t.open(t.pattern);
        t.server(s -> {
            ((ItemHandlerBase) Selftest.node(s, t.pattern).getPatterns()).setStackInSlot(0, new ItemStack(RSItems.PATTERN, 16));
            return null;
        });
        t.fillConflict(t.pattern, 1);
        t.waitFor("two candidates", () -> SelftestAccess.candidates() == 2);
        Selftest.pause(100);
        Selftest.check("pattern.button_warning_when_conflict", t.client(SelftestAccess::buttonVisible), "hidden");
        ItemStack first = t.waitResult();
        Item firstItem = first.getItem();
        Item otherItem = firstItem == Items.DIAMOND ? Items.EMERALD : Items.DIAMOND;
        ResourceLocation firstId = firstItem == Items.DIAMOND ? t.DIAMOND : t.EMERALD;
        ResourceLocation otherId = firstItem == Items.DIAMOND ? t.EMERALD : t.DIAMOND;

        // The default recipe, printed before any selection.
        ItemStack printed = t.print();
        Selftest.check("pattern.print_keeps_default_output", output(printed) == firstItem, Selftest.str(printed));

        t.clickButton();
        List<ResourceLocation> ids = t.client(SelftestAccess::popupIds);
        t.client(() -> {
            SelftestAccess.clickPopupCell(t.mc.currentScreen, ids.indexOf(otherId));
            return null;
        });
        t.waitFor("preview", () -> Selftest.isItem(t.resultOnClient(), otherItem));
        Selftest.pass("pattern.preview_follows_selection");

        printed = t.print();
        Selftest.check("pattern.print_keeps_selected_output", output(printed) == otherItem, Selftest.str(printed));

        t.client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(t.mc.player.openContainer.windowId, firstId));
            return null;
        });
        t.waitFor("preview back", () -> Selftest.isItem(t.resultOnClient(), firstItem));
        Selftest.pass("pattern.select_back_to_first");
        ItemStack second2 = t.print();
        Selftest.check("pattern.second_print_has_its_own_output", output(second2) == firstItem, Selftest.str(second2));
        Selftest.check("pattern.first_print_unchanged", output(printed) == otherItem, Selftest.str(printed));

        Selftest.skip("pattern.print_tagged_with_default", NO_RECIPE);
        Selftest.skip("pattern.print_tagged_with_selection", NO_RECIPE);
        Selftest.skip("pattern.resolver_gives_selection", NO_RECIPE);
        Selftest.skip("pattern.untagged_takes_rs_lookup", NO_RECIPE);
        Selftest.skip("pattern.unknown_tag_takes_rs_lookup", NO_RECIPE);
        Selftest.skip("pattern.non_matching_tag_takes_rs_lookup", NO_RECIPE);

        // Other inputs print the matrix's recipe, not a stale selection.
        t.server(s -> {
            Selftest.clearMatrix(Selftest.node(s, t.pattern));
            Selftest.node(s, t.pattern).getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.LOG));
            return null;
        });
        t.waitFor("unique on the client", () -> Selftest.isItem(t.resultOnClient(), Item.getItemFromBlock(Blocks.PLANKS)));
        ItemStack planks = t.print();
        Selftest.check("pattern.different_recipe_no_stale_output", output(planks) == Item.getItemFromBlock(Blocks.PLANKS), Selftest.str(planks));

        // A print keeps the byproducts of the recipe it shows, not those of vanilla's first match.
        t.fillRemainderConflict(t.pattern);
        t.waitFor("remainder result", () -> Selftest.isItem(t.resultOnClient(), Items.COAL) || Selftest.isItem(t.resultOnClient(), Items.REDSTONE));
        boolean coalFirst = t.client(t::resultOnClient).getItem() == Items.COAL;
        ItemStack ownRemainder = t.print();
        SelftestByproducts.check("pattern.byproducts_of_default_recipe", ownRemainder, coalFirst ? Items.STICK : Items.BONE);
        ResourceLocation otherRemainderId = coalFirst ? SelftestMod.RECIPE_REMAINDER_BONE : SelftestMod.RECIPE_REMAINDER_STICK;
        Item otherOutput = coalFirst ? Items.REDSTONE : Items.COAL;
        t.client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(t.mc.player.openContainer.windowId, otherRemainderId));
            return null;
        });
        t.waitFor("selected remainder recipe", () -> Selftest.isItem(t.resultOnClient(), otherOutput));
        ItemStack otherRemainder = t.print();
        SelftestByproducts.check("pattern.byproducts_follow_selection", otherRemainder, coalFirst ? Items.BONE : Items.STICK);

        // Processing patterns have a block of their own in this Refined Storage.
        Selftest.skip("pattern.processing_has_no_tag", "this Pattern Grid has no processing mode");
        Selftest.skip("pattern.packet_ignored_on_processing", "this Pattern Grid has no processing mode");

        // Selection survives on the pattern grid too, checked after the reload in persist.
        t.fillConflict(t.pattern, 1);
        t.client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(t.mc.player.openContainer.windowId, otherId));
            return null;
        });
        t.waitFor("selection before reload", () -> Selftest.isItem(t.resultOnClient(), otherItem));
        t.closeScreen();
    }

    private static Item output(ItemStack pattern) {
        ItemStack[] outputs = ItemPattern.getOutputs(pattern);
        return outputs == null || outputs.length != 1 || outputs[0] == null ? null : outputs[0].getItem();
    }
}
