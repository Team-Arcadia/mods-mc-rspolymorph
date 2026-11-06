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
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternProvider;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.client.SelftestAccess;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import com.vyrriox.rspolymorph.network.SelectRecipeMessage;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * The Pattern Grid checks of a Refined Storage whose patterns look their recipe up: tag and resolver.
 *
 * Author: vyrriox
 */
final class SelftestPatterns {

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
        Selftest.check("pattern.print_tagged_with_default", firstId.equals(RecipeAccess.taggedRecipe(printed)), Selftest.str(printed));

        t.clickButton();
        List<ResourceLocation> ids = t.client(SelftestAccess::popupIds);
        t.client(() -> {
            SelftestAccess.clickPopupCell(t.mc.currentScreen, ids.indexOf(otherId));
            return null;
        });
        t.waitFor("preview", () -> Selftest.isItem(t.resultOnClient(), otherItem));
        Selftest.pass("pattern.preview_follows_selection");

        printed = t.print();
        Selftest.check("pattern.print_tagged_with_selection", otherId.equals(RecipeAccess.taggedRecipe(printed)), Selftest.str(printed));
        Selftest.check("pattern.resolver_gives_selection", Selftest.isItem(resolve(t, printed), otherItem), Selftest.str(resolve(t, printed)));

        t.client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(t.mc.player.openContainer.windowId, firstId));
            return null;
        });
        t.waitFor("preview back", () -> Selftest.isItem(t.resultOnClient(), firstItem));
        Selftest.pass("pattern.select_back_to_first");
        ItemStack second2 = t.print();
        Selftest.check("pattern.second_print_has_its_own_tag", firstId.equals(RecipeAccess.taggedRecipe(second2)), Selftest.str(second2));
        Selftest.check("pattern.resolver_gives_second", Selftest.isItem(resolve(t, second2), firstItem), Selftest.str(resolve(t, second2)));
        Selftest.check("pattern.first_print_unchanged", Selftest.isItem(resolve(t, printed), otherItem), Selftest.str(resolve(t, printed)));

        // The resolver falls back on Refined Storage's own lookup.
        ItemStack untagged = printed.copy();
        untagged.getTagCompound().removeTag(RsPolymorph.TAG_RECIPE);
        Item plainResolved = resolve(t, untagged).getItem();
        Selftest.check("pattern.untagged_takes_rs_lookup", plainResolved == Items.DIAMOND || plainResolved == Items.EMERALD, plainResolved.toString());
        ItemStack unknown = printed.copy();
        RecipeAccess.tagRecipe(unknown, new ResourceLocation("rspolymorph_selftest", "gone"));
        Selftest.check("pattern.unknown_tag_takes_rs_lookup", resolve(t, unknown).getItem() == plainResolved, Selftest.str(resolve(t, unknown)));
        ItemStack wrong = printed.copy();
        RecipeAccess.tagRecipe(wrong, SelftestMod.RECIPE_STICK);
        Selftest.check("pattern.non_matching_tag_takes_rs_lookup", resolve(t, wrong).getItem() == plainResolved, Selftest.str(resolve(t, wrong)));

        // Other inputs print the matrix's recipe, not a stale selection.
        t.server(s -> {
            Selftest.clearMatrix(Selftest.node(s, t.pattern));
            Selftest.node(s, t.pattern).getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.LOG));
            return null;
        });
        t.waitFor("unique on the client", () -> Selftest.isItem(t.resultOnClient(), Item.getItemFromBlock(Blocks.PLANKS)));
        ItemStack planks = t.print();
        ResourceLocation planksTag = RecipeAccess.taggedRecipe(planks);
        Selftest.check("pattern.different_recipe_no_stale_tag", planksTag != null && !planksTag.equals(firstId) && !planksTag.equals(otherId), String.valueOf(planksTag));

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

    private static ItemStack resolve(Selftest t, ItemStack patternStack) throws Exception {
        return t.server(s -> {
            ICraftingPattern resolved = ((ICraftingPatternProvider) patternStack.getItem()).create(s.worldServerForDimension(0), patternStack, null);
            return resolved.getOutputs().isEmpty() ? null : resolved.getOutputs().get(0);
        });
    }
}
