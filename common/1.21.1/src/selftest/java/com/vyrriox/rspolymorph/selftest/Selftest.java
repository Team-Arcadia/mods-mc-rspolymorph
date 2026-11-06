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

import com.refinedmods.refinedstorage.common.autocrafting.PatternItem;
import com.refinedmods.refinedstorage.common.grid.screen.AbstractGridScreen;
import com.refinedmods.refinedstorage.common.support.RecipeMatrix;
import com.refinedmods.refinedstorage.common.support.RecipeMatrixContainer;
import com.refinedmods.refinedstorage.common.support.containermenu.DisabledSlot;
import com.refinedmods.refinedstorage.common.support.containermenu.ValidatedSlot;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.client.AccessorScreenOffset;
import com.vyrriox.rspolymorph.client.RecipeSelectionSideButton;
import com.vyrriox.rspolymorph.client.RecipeSelectorPopup;
import com.vyrriox.rspolymorph.client.RsGridRecipeWidget;
import com.vyrriox.rspolymorph.client.TutorialState;
import com.vyrriox.rspolymorph.network.SelectRecipePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * In-game functional test of the mod, compiled only with {@code -Pselftest} and started only with
 * {@code -Drspolymorph.selftest=true}. It never reaches a released jar.
 *
 * The scenario runs on its own thread and reads like a player's session: every game access goes
 * through {@link #client} or {@link #server}, which run on the game's threads and wait for the
 * result. {@code MixinMinecraftSelftest} pumps the client side from the end of Minecraft's tick, so
 * no loader glue is involved. What differs between Minecraft versions is in {@link SelftestCompat}.
 *
 * Every check prints {@code RSP-SELFTEST PASS name} or {@code RSP-SELFTEST FAIL name: why}, and the
 * run ends with {@code RSP-SELFTEST DONE pass=N fail=M}. The same lines go to
 * {@code rspolymorph-selftest.txt} in the run folder.
 *
 * Author: vyrriox
 */
public final class Selftest {

    private static final Logger LOG = LogManager.getLogger("rspolymorph-selftest");
    private static final boolean ENABLED = Boolean.getBoolean("rspolymorph.selftest");
    private static final long HARD_TIMEOUT_SECONDS = Long.getLong("rspolymorph.selftest.timeout", 480L);
    private static final String WORLD = "rsp_selftest";
    private static final String REPORT = "rspolymorph-selftest.txt";
    private static final String BANNER_WITH_PATTERN = "minecraft:white_banner[minecraft:banner_patterns="
            + "[{pattern:\"minecraft:stripe_top\",color:\"red\"}]]";

    private static final ResourceLocation RECIPE_UNIQUE = ResourceLocation.fromNamespaceAndPath("rsp_selftest", "unique_stick");
    private static final ResourceLocation RECIPE_UNKNOWN = ResourceLocation.fromNamespaceAndPath("rsp_selftest", "does_not_exist");
    private static final ResourceLocation RECIPE_STONECUTTING = ResourceLocation.fromNamespaceAndPath("minecraft", "stone_slab_from_stonecutting");
    private static final ResourceLocation RECIPE_BOWL = ResourceLocation.fromNamespaceAndPath("rsp_selftest", "conflict_bowl");

    private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile int ticks;
    private static boolean started;

    private final Minecraft mc = Minecraft.getInstance();
    private final Path gameDir = mc.gameDirectory.toPath();
    private final Path reportFile = gameDir.resolve(REPORT);
    private int passed;
    private int failed;
    private boolean finished;
    private BlockPos origin;
    private int nextGrid;

    private Selftest() {
    }

    /** Called from the end of Minecraft's client tick. */
    public static void onClientTick() {
        if (!ENABLED) return;
        ticks++;
        if (!started) {
            started = true;
            Selftest test = new Selftest();
            Thread scenario = new Thread(test::run, "rspolymorph-selftest");
            scenario.setDaemon(true);
            scenario.start();
            Thread watchdog = new Thread(test::watchdog, "rspolymorph-selftest-watchdog");
            watchdog.setDaemon(true);
            watchdog.start();
        }
        Runnable task;
        while ((task = QUEUE.poll()) != null) {
            task.run();
        }
    }

    // -------------------------------------------------------------------------
    // Scenario
    // -------------------------------------------------------------------------

    private void run() {
        try {
            Files.deleteIfExists(reportFile);
            section("world", this::startWorld);
            section("audit", this::audit);
            section("tutorial", this::tutorial);
            section("crafting", this::craftingGrid);
            section("packet", this::packetValidation);
            section("unique", this::uniqueRecipe);
            section("plain", this::plainGrid);
            section("component", this::componentVariant);
            section("pattern", this::patternGrid);
            // Last: it leaves and rejoins the world, and proves the save of what the others did not touch.
            section("persist", this::persistence);
            line("SKIP wireless");
        } catch (Throwable t) {
            fail("scenario", t);
        }
        finish();
    }

    private void startWorld() throws Exception {
        client(() -> {
            // A first launch asks about accessibility; a background window must keep running.
            SelftestCompat.skipOnboarding(mc);
            mc.options.pauseOnLostFocus = false;
            mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
            SelftestCompat.setWindowSize(mc, 1280, 720);
            return null;
        });
        waitTicks(5);
        client(() -> {
            mc.options.guiScale().set(2);
            SelftestCompat.applyGuiScale(mc);
            return null;
        });
        await("the title screen", 2400, () -> {
            if (mc.getOverlay() != null) return false;
            if (!(mc.screen instanceof TitleScreen)) {
                mc.setScreen(new TitleScreen());
                return false;
            }
            return true;
        });

        Path worldDir = gameDir.resolve("saves").resolve(WORLD);
        deleteTree(worldDir);
        installDatapack(worldDir);
        deleteTutorialFlag();
        client(() -> {
            SelftestCompat.createFlatWorld(mc, WORLD);
            return null;
        }, 300);
        awaitInWorld();
        origin = client(() -> mc.player.blockPosition());

        check("world.recipes_loaded", () -> require(server(() ->
                RsPolymorph.findRecipe(serverPlayer().level(), RECIPE_BOWL) != null), "the test datapack was not loaded"));
        command("setblock " + at(origin.offset(2, 0, 3)) + " refinedstorage:creative_controller");
    }

    // ---- 1. Mixins ----------------------------------------------------------

    private void audit() throws Exception {
        SelftestMixins.AuditResult audit = client(SelftestMixins::audit, 300);
        if (audit.thrown() != null) LOG.warn("Mixin audit stopped early (checked below for this mod)", audit.thrown());
        check("audit.no_failure_in_our_configs", () -> {
            String problem = audit.problem();
            require(problem == null, problem);
        });
        for (String config : List.of("rspolymorph-common.mixins.json", "rspolymorph-selftest.mixins.json")) {
            for (String mixin : SelftestMixins.mixinsOf(config, "mixins", "client")) {
                check("audit.applied." + mixin.substring(mixin.lastIndexOf('.') + 1), () -> {
                    String problem = client(() -> SelftestMixins.problemOf(mixin));
                    require(problem == null, problem);
                });
            }
        }
    }

    // ---- 9. Tutorial --------------------------------------------------------

    private void tutorial() throws Exception {
        BlockPos pos = placeGrid("crafting_grid");
        open(pos);
        check("tutorial.shows_on_first_open", () -> require(client(() -> widget().isShowingTutorial()), "no tutorial card"));
        waitTicks(5);
        screenshot("tutorial_card");
        check("tutorial.marks_state_file", () -> await("the state file", 100, () -> Files.exists(tutorialFlag())));
        check("tutorial.space_dismisses", () -> {
            boolean consumed = client(() -> SelftestCompat.key(screen(), GLFW.GLFW_KEY_SPACE, 57));
            require(consumed, "the key press was not consumed");
            require(client(() -> !widget().isShowingTutorial()), "the card is still up after Space");
            require(client(() -> gridSearchText().isEmpty()), "Space reached the search box");
        });
        closeScreen();
        open(pos);
        check("tutorial.hidden_on_second_open", () -> require(client(() -> !widget().isShowingTutorial()), "the card came back"));
        closeScreen();
        // Forget the session cache: the only thing left saying "seen" is the state file.
        client(() -> {
            setField(TutorialState.class, null, "seenCache", null);
            return null;
        });
        open(pos);
        check("tutorial.state_file_respected", () -> require(client(() -> !widget().isShowingTutorial()),
                "the card came back although the state file exists"));
        closeScreen();

        // A click dismisses it too, and that click must not reach a slot.
        deleteTutorialFlag();
        client(() -> {
            setField(TutorialState.class, null, "seenCache", null);
            setField(TutorialState.class, null, "persistedThisSession", false);
            return null;
        });
        command("give @a minecraft:clay_ball 2");
        waitTicks(3);
        open(pos);
        check("tutorial.click_dismisses", () -> {
            require(client(() -> widget().isShowingTutorial()), "no tutorial card after deleting the state file");
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            waitTicks(3);
            require(client(() -> !widget().isShowingTutorial()), "the card is still up after a click");
            require(client(() -> menu().getCarried().isEmpty()), "the dismissing click also picked up an item");
        });
        await("the state file", 100, () -> Files.exists(tutorialFlag()));
        closeScreen();
        command("clear @a");
    }

    // ---- 2 and 3. Crafting Grid ---------------------------------------------

    private void craftingGrid() throws Exception {
        command("give @a minecraft:clay_ball 16");
        waitTicks(3);
        BlockPos pos = placeGrid("crafting_grid");
        open(pos);
        check("crafting.button_inactive_when_empty", () -> {
            waitTicks(3);
            require(client(() -> !sideButton().active), "active on an empty grid");
        });
        client(() -> {
            putEach(Items.CLAY_BALL, 3, 0, 1);
            return null;
        });
        await("the default result on the server", 40, () -> !serverResult().isEmpty());
        ItemStack first = serverResult();
        check("crafting.default_result_on_client", () -> await("the result on the client", 40, () -> !clientResult().isEmpty()));

        check("crafting.button_warning_when_conflict", () -> {
            await("the side button to light up", 40, () -> sideButton().active);
            require(client(() -> Boolean.TRUE.equals(field(sideButton(), "warningShown"))), "warning flag not set");
            require(client(() -> field(sideButton(), "warning") != null), "RS warning not set");
        });

        check("crafting.click_outside_closes_popup", () -> {
            clickSideButton();
            require(client(() -> widget().isPopupOpen()), "the side button did not open the popup");
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
            require(client(() -> !widget().isPopupOpen()), "the popup stayed open");
        });

        check("crafting.popup_lists_both_outputs", () -> {
            clickSideButton();
            require(client(() -> widget().isPopupOpen()), "the side button did not open the popup");
            List<Item> outputs = client(() -> popupEntries().stream().map(e -> e.output().getItem()).toList());
            require(outputs.size() == 2 && outputs.contains(Items.BOWL) && outputs.contains(Items.LADDER),
                    "entries: " + outputs);
        });
        waitTicks(3);
        screenshot("popup_open");

        ItemStack other = other(first);
        ResourceLocation otherId = client(() -> entryFor(other.getItem()).id());
        check("crafting.pick_other_recipe", () -> {
            clickPopupEntry(other.getItem());
            require(client(() -> !widget().isPopupOpen()), "the popup stayed open after the pick");
            await("the server's result to change", 20, () -> isStack(serverResult(), other));
            await("the client's result to change", 20, () -> isStack(clientResult(), other));
        });
        check("crafting.selection_stored_on_server", () -> require(otherId.equals(server(() -> selection())),
                "stored: " + server(() -> selection())));
        waitTicks(3);
        screenshot("result_selected");
        check("crafting.popup_reopens_with_both", () -> {
            clickSideButton();
            require(client(() -> popupEntries().size() == 2), "wrong entries");
            client(() -> {
                SelftestCompat.click(screen(), 2, 2, 0);
                return null;
            });
            waitTicks(2);
        });

        // 3. Taking the result.
        check("crafting.take_one_gives_selected", () -> {
            int inputBefore = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            client(() -> {
                pickup(resultSlot(menu()).index, 0);
                return null;
            });
            await("the taken item on the cursor", 20, () -> isStack(server(() -> serverPlayer().containerMenu.getCarried().copy()), other));
            client(() -> {
                pickup(freeInventorySlot(menu()).index, 0);
                return null;
            });
            await("the taken item in the inventory", 20, () ->
                    server(() -> count(serverPlayer(), other.getItem())) == other.getCount());
            require(server(() -> count(serverPlayer(), first.getItem())) == 0, "the other recipe's output was given too");
            int inputAfter = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            require(inputAfter == inputBefore - 2, "ingredients " + inputBefore + " -> " + inputAfter);
            await("the result to stay on the selection", 20, () -> isStack(serverResult(), other) && isStack(clientResult(), other));
        });

        check("crafting.shift_craft_full_stack", () -> {
            client(() -> {
                clearMatrix();
                return null;
            });
            waitTicks(5);
            command("clear @a");
            command("give @a minecraft:clay_ball 128");
            await("the ingredients", 40, () -> server(() -> count(serverPlayer(), Items.CLAY_BALL)) == 128);
            client(() -> {
                putStack(Items.CLAY_BALL, 0);
                return null;
            });
            waitTicks(3);
            client(() -> {
                putStack(Items.CLAY_BALL, 1);
                return null;
            });
            await("the result", 20, () -> isStack(serverResult(), other));
            client(() -> {
                SelftestCompat.quickMove(mc, menu().containerId, resultSlot(menu()).index);
                return null;
            });
            await("the crafted items", 40, () -> server(() -> count(serverPlayer(), other.getItem())) > 0);
            waitTicks(10);
            int made = server(() -> count(serverPlayer(), other.getItem()));
            int crafts = made / other.getCount();
            int left = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            require(made >= other.getMaxStackSize(), "only " + made + " crafted");
            require(made % other.getCount() == 0, made + " is not a multiple of " + other.getCount());
            require(server(() -> count(serverPlayer(), first.getItem())) == 0, "the other recipe's output was given too");
            require(left == 128 - 2 * crafts, crafts + " crafts but " + left + " ingredients left of 128");
            require(isStack(serverResult(), other), "the result moved off the selection");
        });

        closeScreen();
        command("clear @a");
    }

    // ---- 8. Packet validation -----------------------------------------------

    private void packetValidation() throws Exception {
        command("give @a minecraft:clay_ball 4");
        waitTicks(3);
        open(placeGrid("crafting_grid"));
        client(() -> {
            putEach(Items.CLAY_BALL, 1, 0, 1);
            return null;
        });
        await("the result", 40, () -> !serverResult().isEmpty());

        // Select the default recipe through the handler, so that a stored selection exists to protect.
        ItemStack before = serverResult();
        ResourceLocation current = conflictId(before.getItem());
        ResourceLocation otherId = conflictId(other(before).getItem());
        server(() -> {
            SelectRecipePacket.applyOnServer(serverPlayer(), serverPlayer().containerMenu.containerId, current);
            return null;
        });
        check("packet.valid_default_stored", () -> require(current.equals(server(() -> selection())), "not stored"));

        int containerId = client(() -> menu().containerId);
        rejected("packet.rejects_wrong_container", containerId + 7, otherId, before, current);
        rejected("packet.rejects_unknown_recipe", containerId, RECIPE_UNKNOWN, before, current);
        rejected("packet.rejects_other_recipe_type", containerId, RECIPE_STONECUTTING, before, current);
        rejected("packet.rejects_non_matching_recipe", containerId, RECIPE_UNIQUE, before, current);
        check("packet.still_accepts_a_valid_recipe", () -> {
            server(() -> {
                SelectRecipePacket.applyOnServer(serverPlayer(), containerId, otherId);
                return null;
            });
            require(otherId.equals(server(() -> selection())), "a valid selection was refused");
            require(isStack(serverResult(), other(before)), "the result did not follow");
        });
        // With no grid open at all the menu is the player's inventory.
        closeScreen();
        waitTicks(3);
        check("packet.rejects_when_no_grid_open", () -> server(() -> {
            SelectRecipePacket.applyOnServer(serverPlayer(), 0, otherId);
            return null;
        }));
        command("clear @a");
    }

    private void rejected(String name, int containerId, ResourceLocation id, ItemStack result, ResourceLocation selection) {
        check(name, () -> {
            server(() -> {
                SelectRecipePacket.applyOnServer(serverPlayer(), containerId, id);
                return null;
            });
            require(selection.equals(server(() -> selection())), "selection changed to " + server(() -> selection()));
            require(isStack(serverResult(), result), "result changed to " + serverResult());
        });
    }

    // ---- 5. Unique recipe, then non-matching input --------------------------

    private void uniqueRecipe() throws Exception {
        command("give @a minecraft:flint 4");
        command("give @a minecraft:clay_ball 4");
        command("give @a minecraft:dirt 4");
        waitTicks(3);
        open(placeGrid("crafting_grid"));
        client(() -> {
            putEach(Items.FLINT, 1, 0, 1);
            return null;
        });
        await("the unique recipe's result", 40, () -> serverResult().getItem() == Items.STICK);
        check("unique.no_warning", () -> {
            await("the client's result", 20, () -> clientResult().getItem() == Items.STICK);
            waitTicks(3);
            require(client(() -> !sideButton().active), "button active");
            require(client(() -> !Boolean.TRUE.equals(field(sideButton(), "warningShown"))), "warning flag set");
            require(client(() -> field(sideButton(), "warning") == null), "RS warning set");
        });
        check("unique.button_click_does_nothing", () -> {
            clickSideButton();
            require(client(() -> !widget().isPopupOpen()), "a popup opened for a unique recipe");
        });

        // A popup left open over a conflict, then an input that matches nothing.
        client(() -> {
            clearMatrix();
            return null;
        });
        await("the empty result", 20, () -> serverResult().isEmpty());
        client(() -> {
            putEach(Items.CLAY_BALL, 1, 0, 1);
            return null;
        });
        await("the conflict", 40, () -> sideButton().active);
        clickSideButton();
        check("unique.popup_opened_for_conflict", () -> require(client(() -> widget().isPopupOpen()), "no popup"));
        client(() -> {
            Slot target = matrixSlots(menu()).get(1);
            pickup(target.index, 0);
            pickup(freeInventorySlot(menu()).index, 0);
            Slot dirt = inventorySlot(menu(), Items.DIRT);
            pickup(dirt.index, 0);
            pickup(target.index, 1);
            pickup(dirt.index, 0);
            return null;
        });
        check("unique.non_matching_clears_result", () -> {
            await("the server's result to clear", 20, () -> serverResult().isEmpty());
            await("the client's result to clear", 20, () -> clientResult().isEmpty());
        });
        check("unique.non_matching_closes_popup", () -> {
            await("the popup to close", 20, () -> !widget().isPopupOpen());
            await("the button to go dark", 20, () -> !sideButton().active
                    && !Boolean.TRUE.equals(field(sideButton(), "warningShown")));
        });
        closeScreen();
        command("clear @a");
    }

    // ---- 7. Plain Grid after a Crafting Grid --------------------------------

    private void plainGrid() throws Exception {
        command("give @a minecraft:clay_ball 4");
        waitTicks(3);
        BlockPos crafting = placeGrid("crafting_grid");
        BlockPos plain = placeGrid("grid");
        open(crafting);
        client(() -> {
            putEach(Items.CLAY_BALL, 1, 0, 1);
            return null;
        });
        await("the conflict", 40, () -> sideButton().active);
        clickSideButton();
        check("plain.popup_up_before_closing", () -> require(client(() -> widget().isPopupOpen()), "no popup"));
        client(() -> SelftestCompat.key(screen(), GLFW.GLFW_KEY_ESCAPE, 1));
        check("plain.escape_closes_grid", () -> await("the screen to close", 40, () -> mc.screen == null));
        check("plain.no_widget_left", () -> require(client(() -> RsGridRecipeWidget.getActiveInstance() == null), "a widget survived its screen"));

        open(plain);
        check("plain.no_side_button", () -> require(client(() -> sideButtons().isEmpty()), "a plain grid got the button"));
        check("plain.no_widget", () -> require(client(() -> RsGridRecipeWidget.getActiveInstance() == null), "stale active widget"));
        check("plain.clicks_reach_slots", () -> {
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            await("the picked up stack", 20, () -> !menu().getCarried().isEmpty());
            await("the server's picked up stack", 20, () -> server(() -> !serverPlayer().containerMenu.getCarried().isEmpty()));
            client(() -> {
                Slot slot = freeInventorySlot(menu());
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            await("the stack to go back", 20, () -> menu().getCarried().isEmpty());
        });
        check("plain.slot_hover_for_tooltips", () -> {
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.movePointer(mc, slotX(slot), slotY(slot));
                return null;
            });
            await("the slot to be hovered", 20, () -> {
                SelftestCompat.holdPointer(mc);
                Slot hovered = (Slot) field(screen(), "hoveredSlot");
                return hovered != null && hovered.getItem().getItem() == Items.CLAY_BALL;
            });
        });
        waitTicks(3);
        screenshot("plain_grid_tooltip");
        closeScreen();
        command("clear @a");
    }

    // ---- 10. Component-only variant -----------------------------------------

    /**
     * Two plain white banners match one test recipe. A banner with a pattern is the same item with one
     * more data component, and on top of the test recipe it now also matches vanilla's banner duplicate
     * recipe: the input changed only by a component, and the button must follow.
     */
    private void componentVariant() throws Exception {
        command("give @a minecraft:white_banner 2");
        command("give @a " + BANNER_WITH_PATTERN + " 1");
        waitTicks(3);
        open(placeGrid("crafting_grid"));
        client(() -> {
            Slot plain = inventorySlot(menu(), Items.WHITE_BANNER, false);
            pickup(plain.index, 0);
            pickup(matrixSlots(menu()).get(0).index, 1);
            pickup(matrixSlots(menu()).get(1).index, 1);
            return null;
        });
        await("the plain banner result", 40, () -> serverResult().getItem() == Items.BOWL);
        check("component.plain_stacks_no_warning", () -> {
            waitTicks(5);
            require(client(() -> !sideButton().active), "button active with plain banners");
        });
        client(() -> {
            // Swap the second banner for the patterned one: same item, same count, one more component.
            Slot patterned = inventorySlot(menu(), Items.WHITE_BANNER, true);
            pickup(patterned.index, 0);
            pickup(matrixSlots(menu()).get(1).index, 0);
            pickup(patterned.index, 0);
            return null;
        });
        check("component.variant_lights_button", () -> {
            await("the button to light up", 40, () -> sideButton().active);
            require(client(() -> Boolean.TRUE.equals(field(sideButton(), "warningShown"))), "warning flag not set");
            clickSideButton();
            require(client(() -> popupEntries().size() == 2), "entries: " + client(() -> popupEntries().size()));
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
        });
        client(() -> {
            Slot plain = inventorySlot(menu(), Items.WHITE_BANNER, false);
            pickup(plain.index, 0);
            pickup(matrixSlots(menu()).get(1).index, 0);
            pickup(plain.index, 0);
            return null;
        });
        check("component.plain_again_clears_button", () -> await("the button to go dark", 40, () -> !sideButton().active));
        closeScreen();
        command("clear @a");
    }

    // ---- 6. Pattern Grid ----------------------------------------------------

    private void patternGrid() throws Exception {
        command("give @a minecraft:clay_ball 4");
        command("give @a minecraft:flint 4");
        command("give @a refinedstorage:pattern 8");
        waitTicks(3);
        open(placeGrid("pattern_grid"));
        client(() -> {
            // The empty patterns go to the pattern input; the crafting matrix takes ghost copies.
            Slot patterns = emptyPatterns(menu());
            pickup(patterns.index, 0);
            pickup(patternInput(menu()).index, 0);
            ghost(Items.CLAY_BALL, 0, 1);
            return null;
        });
        await("the preview", 40, () -> !serverResult().isEmpty());
        ItemStack first = serverResult();
        ItemStack other = other(first);

        check("pattern.button_warning_when_conflict", () -> await("the side button to light up", 40, () -> sideButton().active));
        ResourceLocation otherId = conflictId(other.getItem());
        ResourceLocation firstId = conflictId(first.getItem());
        check("pattern.preview_follows_selection", () -> {
            clickSideButton();
            require(client(() -> widget().isPopupOpen()), "no popup");
            clickPopupEntry(other.getItem());
            await("the server's preview to change", 20, () -> isStack(serverResult(), other));
            await("the client's preview to change", 20, () -> isStack(clientResult(), other));
            require(otherId.equals(server(() -> selection())), "stored: " + server(() -> selection()));
        });

        // Printed with the selected recipe.
        ItemStack tagged = print();
        check("pattern.print_tagged_with_selection", () -> require(otherId.equals(tagOf(tagged)), "tag: " + tagOf(tagged)));
        check("pattern.resolver_gives_selection", () -> {
            // RS's getOutput gives the item with a count of one, for display.
            ItemStack output = resolveOnServer(tagged);
            require(output.getItem() == other.getItem(), "the server resolves " + output);
            ItemStack clientOutput = client(() -> resolve(tagged, mc.level));
            require(clientOutput.getItem() == other.getItem(), "the client resolves " + clientOutput);
        });
        check("pattern.resolver_pattern_output", () -> {
            long amount = server(() -> {
                ItemStack stack = tagged.copy();
                return ((PatternItem) stack.getItem()).getPattern(stack, serverPlayer().level())
                        .map(p -> p.layout().outputs().get(0).amount()).orElse(0L);
            });
            require(amount == other.getCount(), "the pattern makes " + amount + ", the recipe " + other.getCount());
        });
        takePatternOutput();

        // Printed with the other recipe of the same input.
        check("pattern.select_back_to_first", () -> {
            clickSideButton();
            clickPopupEntry(first.getItem());
            await("the preview to change back", 20, () -> isStack(serverResult(), first) && isStack(clientResult(), first));
        });
        ItemStack taggedFirst = print();
        check("pattern.second_print_has_its_own_tag", () -> require(firstId.equals(tagOf(taggedFirst)), "tag: " + tagOf(taggedFirst)));
        check("pattern.resolver_gives_second", () -> {
            ItemStack output = resolveOnServer(taggedFirst);
            require(output.getItem() == first.getItem(), "resolves " + output);
        });
        takePatternOutput();

        // Reloading the first pattern must bring its recipe back while the grid selects the other.
        check("pattern.reload_restores_selection", () -> {
            client(() -> {
                Slot stored = inventorySlotWithTag(menu(), otherId);
                pickup(stored.index, 0);
                pickup(patternOutput(menu()).index, 0);
                return null;
            });
            await("the reloaded pattern", 20, () -> server(() -> !patternOutput(serverPlayer().containerMenu).getItem().isEmpty()));
            await("the selection to come back", 20, () -> otherId.equals(server(() -> selection())));
            await("the preview to follow", 20, () -> isStack(serverResult(), other) && isStack(clientResult(), other));
        });
        takePatternOutput();

        // A different recipe must not inherit the tag of the selected one.
        check("pattern.different_recipe_no_stale_tag", () -> {
            client(() -> {
                ghost(Items.FLINT, 0, 1);
                return null;
            });
            await("the unique preview", 40, () -> serverResult().getItem() == Items.STICK);
            ItemStack unique = print();
            require(RECIPE_UNIQUE.equals(tagOf(unique)), "tag: " + tagOf(unique));
            ItemStack output = resolveOnServer(unique);
            require(output.getItem() == Items.STICK, "resolves " + output);
        });
        closeScreen();
        command("clear @a");
    }

    /** Clicks the real create button and returns the printed pattern, left in the output slot. */
    private ItemStack print() {
        await("the create button", 40, () -> ((AbstractWidget) field(screen(), "createPatternButton")).active);
        client(() -> {
            AbstractWidget button = (AbstractWidget) field(screen(), "createPatternButton");
            SelftestCompat.click(screen(), button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, 0);
            return null;
        });
        await("the printed pattern", 40, () -> server(() -> !patternOutput(serverPlayer().containerMenu).getItem().isEmpty()));
        return server(() -> patternOutput(serverPlayer().containerMenu).getItem().copy());
    }

    private void takePatternOutput() {
        client(() -> {
            pickup(patternOutput(menu()).index, 0);
            pickup(freeInventorySlot(menu()).index, 0);
            return null;
        });
        await("the pattern to leave the output slot", 40, () ->
                server(() -> patternOutput(serverPlayer().containerMenu).getItem().isEmpty()));
    }

    private ItemStack resolveOnServer(ItemStack pattern) {
        return server(() -> resolve(pattern, serverPlayer().level()));
    }

    private static ItemStack resolve(ItemStack pattern, Level level) {
        ItemStack stack = pattern.copy();
        return ((PatternItem) stack.getItem()).getOutput(stack, level).orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation tagOf(ItemStack pattern) {
        return pattern.get(RsPolymorph.selectedRecipeComponent());
    }

    // ---- 4. Persistence -----------------------------------------------------

    private void persistence() throws Exception {
        command("give @a minecraft:clay_ball 8");
        waitTicks(3);
        BlockPos pos = placeGrid("crafting_grid");
        open(pos);
        client(() -> {
            putEach(Items.CLAY_BALL, 1, 0, 1);
            return null;
        });
        await("the result", 40, () -> !serverResult().isEmpty());
        ItemStack first = serverResult();
        ItemStack other = other(first);
        ResourceLocation otherId = conflictId(other.getItem());
        await("the side button to light up", 40, () -> sideButton().active);
        clickSideButton();
        clickPopupEntry(other.getItem());
        await("the selection", 20, () -> isStack(serverResult(), other) && isStack(clientResult(), other));

        closeScreen();
        waitTicks(5);
        open(pos);
        check("persist.after_reopen", () -> {
            await("the result on the server", 20, () -> isStack(serverResult(), other));
            await("the result on the client", 20, () -> isStack(clientResult(), other));
            require(otherId.equals(server(() -> selection())), "stored: " + server(() -> selection()));
            await("the side button", 20, () -> sideButton().active);
        });
        closeScreen();
        waitTicks(5);

        client(() -> {
            SelftestCompat.disconnect(mc);
            return null;
        }, 120);
        waitTicks(10);
        check("persist.world_left", () -> require(client(() -> mc.level == null), "the client still has a level"));
        client(() -> {
            mc.createWorldOpenFlows().openWorld(WORLD, () -> LOG.error("RSP-SELFTEST could not reopen the world"));
            return null;
        }, 300);
        awaitInWorld();
        open(pos);
        check("persist.after_world_reload", () -> {
            await("the result on the server", 40, () -> isStack(serverResult(), other));
            await("the result on the client", 40, () -> isStack(clientResult(), other));
            require(otherId.equals(server(() -> selection())), "stored: " + server(() -> selection()));
        });
        check("persist.conflict_still_detected_after_reload", () -> {
            await("the side button", 40, () -> sideButton().active);
            clickSideButton();
            require(client(() -> popupEntries().size() == 2), "wrong entries");
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
        });
        closeScreen();
    }

    // -------------------------------------------------------------------------
    // World and grids
    // -------------------------------------------------------------------------

    private void awaitInWorld() {
        await("the world to load", 4800, () -> mc.player != null && mc.level != null
                && mc.getSingleplayerServer() != null && mc.screen == null);
        waitTicks(20);
        client(() -> {
            mc.options.pauseOnLostFocus = false;
            return null;
        });
    }

    /** Places the next grid of the row, wired to the creative controller, and walks to it. */
    private BlockPos placeGrid(String block) {
        BlockPos pos = origin.offset(3 + nextGrid++, 0, 3);
        command("setblock " + at(pos) + " refinedstorage:" + block);
        await("the block on the client", 40, () -> !mc.level.getBlockState(pos).isAir());
        return pos;
    }

    private void open(BlockPos pos) {
        closeScreen();
        command("tp @a " + pos.getX() + ".5 " + pos.getY() + " " + (pos.getZ() - 2) + ".5");
        await("the player to arrive", 40, () -> mc.player.blockPosition().closerThan(pos, 3.5));
        waitTicks(10);
        client(() -> {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            return null;
        });
        await("the grid screen", 60, () -> mc.screen instanceof AbstractGridScreen<?>);
        waitTicks(5);
    }

    private void closeScreen() {
        client(() -> {
            if (mc.screen != null && mc.player != null) mc.player.closeContainer();
            return null;
        });
        await("the screen to close", 40, () -> mc.screen == null);
    }

    private void command(String command) {
        server(() -> {
            MinecraftServer server = mc.getSingleplayerServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
            return null;
        });
    }

    private static String at(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void installDatapack(Path worldDir) throws IOException {
        Path pack = worldDir.resolve("datapacks").resolve(WORLD);
        Path recipes = pack.resolve("data").resolve("rsp_selftest").resolve("recipe");
        Files.createDirectories(recipes);
        Files.writeString(pack.resolve("pack.mcmeta"), SelftestCompat.packMcmeta());
        Files.writeString(recipes.resolve("conflict_bowl.json"), shaped("minecraft:clay_ball", "minecraft:bowl", 2));
        Files.writeString(recipes.resolve("conflict_ladder.json"), shaped("minecraft:clay_ball", "minecraft:ladder", 3));
        Files.writeString(recipes.resolve("unique_stick.json"), shaped("minecraft:flint", "minecraft:stick", 4));
        Files.writeString(recipes.resolve("banner_plain.json"), shaped("minecraft:white_banner", "minecraft:bowl", 1));
    }

    private static String shaped(String ingredient, String result, int count) {
        return "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"misc\",\"pattern\":[\"XX\"],\"key\":{\"X\":"
                + SelftestCompat.ingredient(ingredient) + "},\"result\":{\"id\":\"" + result + "\",\"count\":" + count + "}}";
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    private Path tutorialFlag() {
        return gameDir.resolve("config").resolve("rspolymorph_tutorial.flag");
    }

    private void deleteTutorialFlag() throws IOException {
        Files.deleteIfExists(tutorialFlag());
    }

    private void screenshot(String name) {
        client(() -> {
            SelftestCompat.screenshot(mc, name);
            return null;
        });
        waitTicks(3);
    }

    // -------------------------------------------------------------------------
    // Menus and widgets
    // -------------------------------------------------------------------------

    private AbstractContainerScreen<?> screen() {
        if (mc.screen instanceof AbstractContainerScreen<?> s) return s;
        throw new Failure("no container screen is open, the screen is " + mc.screen);
    }

    private AbstractContainerMenu menu() {
        return screen().getMenu();
    }

    private RsGridRecipeWidget widget() {
        RsGridRecipeWidget widget = RsGridRecipeWidget.getActiveInstance();
        if (widget == null) throw new Failure("no active selection widget");
        return widget;
    }

    private List<RecipeSelectionSideButton> sideButtons() {
        List<RecipeSelectionSideButton> found = new ArrayList<>();
        for (Object child : screen().children()) {
            if (child instanceof RecipeSelectionSideButton b) found.add(b);
        }
        return found;
    }

    private RecipeSelectionSideButton sideButton() {
        List<RecipeSelectionSideButton> found = sideButtons();
        if (found.size() != 1) throw new Failure(found.size() + " side buttons on the screen");
        return found.get(0);
    }

    private void clickSideButton() {
        client(() -> {
            RecipeSelectionSideButton b = sideButton();
            SelftestCompat.click(screen(), b.getX() + b.getWidth() / 2.0, b.getY() + b.getHeight() / 2.0, 0);
            return null;
        });
        waitTicks(2);
    }

    private RecipeSelectorPopup popup() {
        return (RecipeSelectorPopup) field(widget(), "popup");
    }

    @SuppressWarnings("unchecked")
    private List<RecipeSelectorPopup.Entry> popupEntries() {
        return new ArrayList<>((List<RecipeSelectorPopup.Entry>) field(popup(), "entries"));
    }

    private RecipeSelectorPopup.Entry entryFor(Item item) {
        for (RecipeSelectorPopup.Entry e : popupEntries()) {
            if (e.output().getItem() == item) return e;
        }
        throw new Failure("no popup entry for " + item + " in " + popupEntries());
    }

    /** Clicks the icon of the entry with this output, at the place the player sees it. */
    private void clickPopupEntry(Item item) {
        client(() -> {
            RecipeSelectorPopup popup = popup();
            List<RecipeSelectorPopup.Entry> entries = popupEntries();
            int i = entries.indexOf(entryFor(item));
            int size = (int) field(RecipeSelectorPopup.class, "SLOT");
            int cols = (int) field(RecipeSelectorPopup.class, "COLS");
            int padding = (int) field(RecipeSelectorPopup.class, "PADDING");
            int header = (int) field(RecipeSelectorPopup.class, "HEADER");
            int x = (int) field(popup, "x") + padding + (i % cols) * size + size / 2;
            int y = (int) field(popup, "y") + padding + header + (i / cols) * size + size / 2;
            SelftestCompat.click(screen(), x, y, 0);
            return null;
        });
        waitTicks(2);
    }

    private double slotX(Slot slot) {
        return ((AccessorScreenOffset) screen()).rspolymorph$leftPos() + slot.x + 8;
    }

    private double slotY(Slot slot) {
        return ((AccessorScreenOffset) screen()).rspolymorph$topPos() + slot.y + 8;
    }

    private String gridSearchText() {
        Object search = field(screen(), "searchField");
        if (search == null) return "";
        try {
            return (String) search.getClass().getMethod("getValue").invoke(search);
        } catch (ReflectiveOperationException e) {
            throw new Failure("cannot read the search box: " + e);
        }
    }

    private static List<Slot> matrixSlots(AbstractContainerMenu menu) {
        List<Slot> found = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof RecipeMatrixContainer c && c.getContainerSize() == 9) found.add(slot);
        }
        found.sort(Comparator.comparingInt(Slot::getContainerSlot));
        if (found.size() != 9) throw new Failure(found.size() + " crafting matrix slots in the menu");
        return found;
    }

    private static Slot resultSlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot instanceof ResultSlot) return slot;
        }
        // A pattern grid's preview comes right after its matrix. Not through Slot#isActive: RS's pattern
        // slots read a client-only property there and throw on the server.
        Slot next = menu.slots.get(matrixSlots(menu).get(8).index + 1);
        if (next instanceof DisabledSlot && next.container instanceof ResultContainer) return next;
        throw new Failure("no result slot in the menu");
    }

    private static Slot patternInput(AbstractContainerMenu menu) {
        return validatedSlots(menu).get(0);
    }

    private static Slot patternOutput(AbstractContainerMenu menu) {
        return validatedSlots(menu).get(1);
    }

    private static List<Slot> validatedSlots(AbstractContainerMenu menu) {
        List<Slot> found = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot instanceof ValidatedSlot) found.add(slot);
        }
        if (found.size() < 2) throw new Failure(found.size() + " pattern slots in the menu");
        return found;
    }

    private static Slot inventorySlot(AbstractContainerMenu menu, Item item) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getItem().getItem() == item) return slot;
        }
        throw new Failure("no " + item + " in the inventory");
    }

    /** The slot with this item with or without a component patch, for two stacks of one item. */
    private static Slot inventorySlot(AbstractContainerMenu menu, Item item, boolean patched) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getItem().getItem() == item
                    && patched != slot.getItem().getComponentsPatch().isEmpty()) {
                return slot;
            }
        }
        throw new Failure("no " + item + " (patched " + patched + ") in the inventory");
    }

    private static Slot inventorySlotWithTag(AbstractContainerMenu menu, ResourceLocation tag) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && tag.equals(slot.getItem().get(RsPolymorph.selectedRecipeComponent()))) {
                return slot;
            }
        }
        throw new Failure("no pattern tagged " + tag + " in the inventory");
    }

    private static Slot freeInventorySlot(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getItem().isEmpty()) return slot;
        }
        throw new Failure("the inventory is full");
    }

    private static Slot emptyPatterns(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && slot.getItem().getItem() instanceof PatternItem) return slot;
        }
        throw new Failure("no pattern in the inventory");
    }

    private void pickup(int slot, int button) {
        SelftestCompat.pickup(mc, menu().containerId, slot, button);
    }

    /** Takes a stack of the item, right-clicks {@code each} into every given matrix slot, puts the rest back. */
    private void putEach(Item item, int each, int... indexes) {
        AbstractContainerMenu menu = menu();
        Slot source = inventorySlot(menu, item);
        if (source.getItem().getCount() < each * indexes.length) {
            throw new Failure("only " + source.getItem().getCount() + " of " + item);
        }
        pickup(source.index, 0);
        for (int i : indexes) {
            for (int n = 0; n < each; n++) {
                pickup(matrixSlots(menu).get(i).index, 1);
            }
        }
        if (!menu.getCarried().isEmpty()) pickup(source.index, 0);
    }

    /** Moves a whole inventory stack of the item into one matrix slot. */
    private void putStack(Item item, int index) {
        pickup(inventorySlot(menu(), item).index, 0);
        pickup(matrixSlots(menu()).get(index).index, 0);
    }

    /** Sets ghost copies in a pattern grid's matrix, replacing what they hold. */
    private void ghost(Item item, int... indexes) {
        AbstractContainerMenu menu = menu();
        Slot source = inventorySlot(menu, item);
        pickup(source.index, 0);
        for (int i : indexes) {
            pickup(matrixSlots(menu).get(i).index, 0);
        }
        pickup(source.index, 0);
    }

    private void clearMatrix() {
        AbstractContainerMenu menu = menu();
        for (Slot slot : matrixSlots(menu)) {
            if (!slot.getItem().isEmpty()) {
                pickup(slot.index, 0);
                pickup(freeInventorySlot(menu).index, 0);
            }
        }
    }

    private static int matrixCount(AbstractContainerMenu menu, Item item) {
        int total = 0;
        for (Slot slot : matrixSlots(menu)) {
            if (slot.getItem().getItem() == item) total += slot.getItem().getCount();
        }
        return total;
    }

    private static int count(Player player, Item item) {
        Inventory inventory = player.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() == item) total += inventory.getItem(i).getCount();
        }
        return total;
    }

    private ServerPlayer serverPlayer() {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private ItemStack serverResult() {
        return server(() -> resultSlot(serverPlayer().containerMenu).getItem().copy());
    }

    private ItemStack clientResult() {
        return client(() -> resultSlot(menu()).getItem().copy());
    }

    /** The selection of the open grid's crafting matrix. Server thread only. */
    private ResourceLocation selection() {
        Slot slot = matrixSlots(serverPlayer().containerMenu).get(0);
        RecipeMatrix<?, ?> matrix = RsPolymorph.matrixOf((RecipeMatrixContainer) slot.container);
        return RsPolymorph.selectionOf(matrix);
    }

    private static boolean isStack(ItemStack actual, ItemStack expected) {
        return actual.getCount() == expected.getCount() && ItemStack.isSameItemSameComponents(actual, expected);
    }

    /** The output of the other conflicting recipe. */
    private static ItemStack other(ItemStack output) {
        return output.getItem() == Items.BOWL ? new ItemStack(Items.LADDER, 3) : new ItemStack(Items.BOWL, 2);
    }

    private static ResourceLocation conflictId(Item output) {
        return ResourceLocation.fromNamespaceAndPath("rsp_selftest", output == Items.BOWL ? "conflict_bowl" : "conflict_ladder");
    }

    // -------------------------------------------------------------------------
    // Reflection on private fields of the code under test
    // -------------------------------------------------------------------------

    private static Object field(Object owner, String name) {
        try {
            Class<?> cls = owner instanceof Class<?> c ? c : owner.getClass();
            Object target = owner instanceof Class<?> ? null : owner;
            return find(cls, name).get(target);
        } catch (ReflectiveOperationException e) {
            throw new Failure("cannot read field " + name + ": " + e);
        }
    }

    private static void setField(Class<?> cls, Object owner, String name, Object value) {
        try {
            find(cls, name).set(owner, value);
        } catch (ReflectiveOperationException e) {
            throw new Failure("cannot write field " + name + ": " + e);
        }
    }

    private static Field find(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // Declared further up.
            }
        }
        throw new NoSuchFieldException(name);
    }

    // -------------------------------------------------------------------------
    // Threads
    // -------------------------------------------------------------------------

    private <T> T client(Callable<T> task) {
        return client(task, 60);
    }

    private <T> T client(Callable<T> task, int timeoutSeconds) {
        if (mc.isSameThread()) {
            // A condition already on the client thread reading more client state.
            try {
                return task.call();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        QUEUE.add(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return get(future, timeoutSeconds);
    }

    private <T> T server(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) throw new Failure("no integrated server");
        server.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return get(future, 60);
    }

    private static <T> T get(CompletableFuture<T> future, int timeoutSeconds) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException r) throw r;
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new Failure("a game thread did not answer: " + e);
        }
    }

    private void waitTicks(int count) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
        int target = ticks + count;
        while (ticks < target) {
            if (System.nanoTime() > deadline) throw new Failure("the client stopped ticking");
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new Failure("interrupted");
            }
        }
    }

    /** Evaluates the condition on the client thread once per tick. It may read the server too. */
    private void await(String what, int maxTicks, Callable<Boolean> condition) {
        Throwable last = null;
        for (int i = 0; i < maxTicks; i++) {
            try {
                if (client(condition)) return;
                last = null;
            } catch (Throwable t) {
                last = t;
            }
            waitTicks(1);
        }
        throw new Failure("timed out waiting for " + what + (last == null ? "" : " (" + oneLine(last) + ")"));
    }

    // -------------------------------------------------------------------------
    // Reporting
    // -------------------------------------------------------------------------

    private interface Body {
        void run() throws Exception;
    }

    private static final class Failure extends RuntimeException {
        Failure(String message) {
            super(message);
        }
    }

    private static void require(boolean condition, String why) {
        if (!condition) throw new Failure(why);
    }

    private void section(String name, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            LOG.error("RSP-SELFTEST section {} stopped", name, t);
            fail(name + ".stopped", t);
        }
        try {
            client(() -> {
                if (mc.screen != null && mc.player != null) mc.player.closeContainer();
                return null;
            });
            if (mc.getSingleplayerServer() != null) command("clear @a");
        } catch (Throwable t) {
            LOG.error("RSP-SELFTEST cleanup after {} failed", name, t);
        }
    }

    private void check(String name, Body body) {
        try {
            body.run();
            passed++;
            line("PASS " + name);
        } catch (Throwable t) {
            if (!(t instanceof Failure)) LOG.error("RSP-SELFTEST check {} threw", name, t);
            fail(name, t);
        }
    }

    private synchronized void fail(String name, Throwable t) {
        failed++;
        line("FAIL " + name + ": " + oneLine(t));
    }

    private synchronized void line(String text) {
        String full = "RSP-SELFTEST " + text;
        LOG.info(full);
        try {
            Files.writeString(reportFile, full + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Cannot write {}", reportFile, e);
        }
    }

    private static String oneLine(Object o) {
        String text = o instanceof Failure f ? f.getMessage() : String.valueOf(o);
        return text.replaceAll("\\s+", " ");
    }

    private static String stackText(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause() == c ? null : c.getCause()) {
            sb.append(c).append('\n');
        }
        return sb.toString();
    }

    private synchronized void finish() {
        if (finished) return;
        finished = true;
        line("DONE pass=" + passed + " fail=" + failed);
        try {
            CompletableFuture<Void> stopped = new CompletableFuture<>();
            QUEUE.add(() -> {
                try {
                    if (mc.level != null) SelftestCompat.disconnect(mc);
                } catch (Throwable t) {
                    LOG.error("RSP-SELFTEST could not leave the world", t);
                }
                stopped.complete(null);
                mc.stop();
            });
            stopped.get(120, TimeUnit.SECONDS);
        } catch (Exception e) {
            LOG.error("RSP-SELFTEST could not stop the game", e);
            Runtime.getRuntime().halt(3);
        }
    }

    private void watchdog() {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(HARD_TIMEOUT_SECONDS));
        } catch (InterruptedException e) {
            return;
        }
        synchronized (this) {
            if (finished) {
                // The game is closing: give it a moment, then make sure it never hangs.
                try {
                    Thread.sleep(60_000L);
                } catch (InterruptedException e) {
                    return;
                }
            } else {
                failed++;
                line("FAIL hard_timeout: still running after " + HARD_TIMEOUT_SECONDS + " s");
                line("DONE pass=" + passed + " fail=" + failed);
            }
        }
        Runtime.getRuntime().halt(2);
    }
}
