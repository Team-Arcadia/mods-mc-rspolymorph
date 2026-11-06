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

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.network.grid.GridType;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.StackListEntry;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.CraftingPatternFactory;
import com.refinedmods.refinedstorage.apiimpl.network.node.GridNetworkNode;
import com.refinedmods.refinedstorage.block.NetworkNodeBlock;
import com.refinedmods.refinedstorage.tile.ControllerTile;
import com.refinedmods.refinedstorage.container.GridContainer;
import com.refinedmods.refinedstorage.item.PatternItem;
import com.refinedmods.refinedstorage.screen.grid.GridScreen;
import com.vyrriox.rspolymorph.GridOwnership;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.client.GridRecipeWidget;
import com.vyrriox.rspolymorph.client.RecipeSelectionSideButton;
import com.vyrriox.rspolymorph.client.SelftestAccess;
import com.vyrriox.rspolymorph.client.SelftestAccess.PopupEntry;
import com.vyrriox.rspolymorph.network.SelectRecipeHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-game functional test of the mod on the Refined Storage 1.x line, compiled only with
 * {@code -Pselftest} and started only with {@code -Drspolymorph.selftest=true}. It never reaches a
 * released jar.
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
 * With Polymorph or Polymorph+ next to the mod the scenario changes: this mod owns no selection then,
 * so it proves that it stays out and that printed patterns are still tagged.
 *
 * Author: vyrriox
 */
public final class Selftest {

    private static final Logger LOG = LogManager.getLogger("rspolymorph-selftest");
    private static final boolean ENABLED = Boolean.getBoolean("rspolymorph.selftest");
    private static final boolean AUDIT_ONLY = Boolean.getBoolean("rspolymorph.auditMixins");
    private static final long HARD_TIMEOUT_SECONDS = Long.getLong("rspolymorph.selftest.timeout", 480L);
    private static final String WORLD = "rsp_selftest";
    private static final String REPORT = "rspolymorph-selftest.txt";
    private static final String WIDGET_FIELD = "rspolymorph$widget";
    private static final String SELECTED_FIELD = "rspolymorph$selected";
    private static final String BANNER_WITH_PATTERN =
            "minecraft:white_banner{BlockEntityTag:{Patterns:[{Pattern:\"ts\",Color:14}]}}";

    private static final ResourceLocation RECIPE_UNIQUE = new ResourceLocation("rsp_selftest", "unique_stick");
    private static final ResourceLocation RECIPE_UNKNOWN = new ResourceLocation("rsp_selftest", "does_not_exist");
    private static final ResourceLocation RECIPE_STONECUTTING = new ResourceLocation("minecraft", "stone_slab_from_stone_stonecutting");
    private static final ResourceLocation RECIPE_BOWL = new ResourceLocation("rsp_selftest", "conflict_bowl");

    private static final Queue<Runnable> QUEUE = new ConcurrentLinkedQueue<>();
    private static volatile int ticks;
    private static boolean started;
    private static boolean audited;

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
        if (AUDIT_ONLY && !audited) {
            audited = true;
            SelftestMixins.logAudit();
        }
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
            if (GridOwnership.polymorphOwnsGrids()) {
                line("SKIP own_selection_checks: Polymorph owns the grids, only its scenario runs");
                section("polymorph", this::polymorphOwnsGrids);
            } else {
                section("tutorial", this::tutorial);
                section("crafting", this::craftingGrid);
                section("packet", this::packetValidation);
                section("unique", this::uniqueRecipe);
                section("plain", this::plainGrid);
                section("nbt", this::nbtVariant);
                section("pattern", this::patternGrid);
                // Last: it leaves and rejoins the world, and proves the save of what the others did not touch.
                section("persist", this::persistence);
            }
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
            SelftestCompat.mute(mc);
            SelftestCompat.setWindowSize(mc, 1280, 720);
            return null;
        });
        waitTicks(5);
        client(() -> {
            SelftestCompat.setGuiScale(mc, 2);
            return null;
        });
        await("the title screen", 2400, () -> {
            if (mc.getOverlay() != null) return false;
            if (!(mc.screen instanceof MainMenuScreen)) {
                mc.setScreen(new MainMenuScreen());
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
                mc.getSingleplayerServer().getRecipeManager().byKey(RECIPE_BOWL).isPresent()), "the test datapack was not loaded"));
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
        for (String config : Arrays.asList("rspolymorph-common.mixins.json", "rspolymorph-selftest.mixins.json")) {
            for (String mixin : SelftestMixins.mixinsOf(config, "mixins", "client")) {
                String name = mixin.substring(mixin.lastIndexOf('.') + 1);
                if (GridOwnership.polymorphOwnsGrids() && SelftestMixins.isSelectionMixin(mixin)) {
                    line("SKIP audit.applied." + name + ": left out next to Polymorph");
                    continue;
                }
                check("audit.applied." + name, () -> {
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
        check("tutorial.shows_on_first_open", () -> require(client(() -> SelftestAccess.isShowingTutorial(widget())), "no tutorial card"));
        waitTicks(5);
        screenshot("tutorial_card");
        check("tutorial.marks_state_file", () -> await("the state file", 100, () -> Files.exists(tutorialFlag())));
        check("tutorial.space_dismisses", () -> {
            client(() -> {
                require(SelftestCompat.key(screen(), GLFW.GLFW_KEY_SPACE, 57), "the key press was not consumed");
                // Same tick as the key press, as in a real key event.
                require(SelftestCompat.typed(screen(), ' '), "the typed space was not consumed");
                return null;
            });
            require(client(() -> !SelftestAccess.isShowingTutorial(widget())), "the card is still up after Space");
            require(client(() -> gridSearchText().isEmpty()), "Space reached the search box");
        });
        closeScreen();
        open(pos);
        check("tutorial.hidden_on_second_open", () -> require(client(() -> !SelftestAccess.isShowingTutorial(widget())), "the card came back"));
        closeScreen();
        // Forget the session cache: the only thing left saying "seen" is the state file.
        client(() -> {
            SelftestAccess.forgetTutorialCache();
            return null;
        });
        open(pos);
        check("tutorial.state_file_respected", () -> require(client(() -> !SelftestAccess.isShowingTutorial(widget())),
                "the card came back although the state file exists"));
        closeScreen();

        // A click dismisses it too, and that click must not reach a slot.
        deleteTutorialFlag();
        client(() -> {
            SelftestAccess.forgetTutorialSession();
            return null;
        });
        command("give @a minecraft:clay_ball 2");
        waitTicks(3);
        open(pos);
        check("tutorial.click_dismisses", () -> {
            require(client(() -> SelftestAccess.isShowingTutorial(widget())), "no tutorial card after deleting the state file");
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            waitTicks(3);
            require(client(() -> !SelftestAccess.isShowingTutorial(widget())), "the card is still up after a click");
            require(client(() -> mc.player.inventory.getCarried().isEmpty()), "the dismissing click also picked up an item");
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
        check("crafting.grid_is_connected", () -> await("the grid to join the network", 40, () -> grid().isGridActive()));
        check("crafting.button_hidden_when_empty", () -> {
            waitTicks(3);
            require(client(() -> !sideButton().visible), "visible on an empty grid");
        });
        client(() -> {
            putEach(Items.CLAY_BALL, 3, 0, 1);
            return null;
        });
        await("the default result on the server", 40, () -> !serverResult().isEmpty());
        ItemStack first = serverResult();
        check("crafting.default_result_on_client", () -> await("the result on the client", 40, () -> !clientResult().isEmpty()));

        check("crafting.button_warning_when_conflict", () -> {
            await("the side button to show", 40, () -> sideButton().visible);
            String tooltip = client(() -> (String) field(sideButton(), "tooltip"));
            require(tooltip.contains(net.minecraft.util.text.TextFormatting.RED.toString()), "no warning line in the tooltip: " + tooltip);
        });
        waitTicks(3);
        screenshot("button_warning");

        check("crafting.click_outside_closes_popup", () -> {
            clickSideButton();
            require(client(() -> SelftestAccess.isPopupOpen(widget())), "the side button did not open the popup");
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
            require(client(() -> !SelftestAccess.isPopupOpen(widget())), "the popup stayed open");
        });

        check("crafting.popup_lists_both_outputs", () -> {
            clickSideButton();
            require(client(() -> SelftestAccess.isPopupOpen(widget())), "the side button did not open the popup");
            List<Item> outputs = client(() -> popupEntries().stream().map(e -> e.output().getItem()).collect(Collectors.toList()));
            require(outputs.size() == 2 && outputs.contains(Items.BOWL) && outputs.contains(Items.LADDER),
                    "entries: " + outputs);
        });
        waitTicks(3);
        screenshot("popup_open");

        ItemStack other = other(first);
        ResourceLocation otherId = client(() -> entryFor(other.getItem()).id());
        check("crafting.pick_other_recipe", () -> {
            clickPopupEntry(other.getItem());
            require(client(() -> !SelftestAccess.isPopupOpen(widget())), "the popup stayed open after the pick");
            await("the server's result to change", 20, () -> isStack(serverResult(), other));
            await("the client's result to change", 20, () -> isStack(clientResult(), other));
        });
        check("crafting.selection_stored_on_server", () -> require(otherId.equals(server(this::selection)),
                "stored: " + server(this::selection)));
        waitTicks(3);
        screenshot("result_selected");
        check("crafting.popup_reopens_with_both", () -> {
            clickSideButton();
            require(client(() -> popupEntries().size() == 2), "wrong entries");
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
        });

        // 3. Taking the result.
        check("crafting.take_one_gives_selected", () -> {
            int inputBefore = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            client(() -> {
                pickup(resultSlot(menu()).index, 0);
                return null;
            });
            await("the taken item on the cursor", 20, () -> isStack(server(() -> serverPlayer().inventory.getCarried().copy()), other));
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
            // The clicks below read the client's copy of the inventory: under load it lags the server's.
            await("the ingredients on the client", 100, () -> count(mc.player, Items.CLAY_BALL) == 128);
            client(() -> {
                putStack(Items.CLAY_BALL, 0);
                return null;
            });
            await("the first stack in the matrix", 100, () ->
                    server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL)) == 64 && mc.player.inventory.getCarried().isEmpty());
            client(() -> {
                putStack(Items.CLAY_BALL, 1);
                return null;
            });
            await("the result", 100, () -> isStack(serverResult(), other));
            client(() -> {
                SelftestCompat.quickMove(mc, menu().containerId, resultSlot(menu()).index);
                return null;
            });
            await("the crafted items", 40, () -> server(() -> count(serverPlayer(), other.getItem())) > 0);
            waitTicks(10);
            int made = server(() -> count(serverPlayer(), other.getItem()));
            int crafts = made / other.getCount();
            int left = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            // RS stops before the next craft would pass the item's stack size.
            require(made > other.getMaxStackSize() - other.getCount(), "only " + made + " crafted");
            require(made % other.getCount() == 0, made + " is not a multiple of " + other.getCount());
            require(server(() -> count(serverPlayer(), first.getItem())) == 0, "the other recipe's output was given too");
            require(left == 128 - 2 * crafts, crafts + " crafts but " + left + " ingredients left of 128");
            require(isStack(serverResult(), other), "the result moved off the selection");
        });

        // What a player with a storage sees: RS refills the matrix while it crafts.
        check("crafting.shift_craft_refills_from_network", () -> {
            client(() -> {
                clearMatrix();
                return null;
            });
            waitTicks(5);
            placeBlock("creative_storage_block");
            int inserted = 64;
            await("the storage to accept the ingredients", 200, () -> server(() -> {
                INetwork network = gridOf(serverPlayer().containerMenu).getNetwork();
                if (network == null) return false;
                if (storedClay(network) == 0) network.insertItem(new ItemStack(Items.CLAY_BALL), inserted, Action.PERFORM);
                return storedClay(network) == inserted;
            }));
            command("clear @a");
            command("give @a minecraft:clay_ball 2");
            await("the ingredients", 40, () -> server(() -> count(serverPlayer(), Items.CLAY_BALL)) == 2);
            client(() -> {
                putEach(Items.CLAY_BALL, 1, 0, 1);
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
            require(crafts > 1, "only " + crafts + " craft: the matrix was not refilled");
            require(server(() -> count(serverPlayer(), first.getItem())) == 0, "the other recipe's output was given too");
            int stored = server(() -> storedClay(gridOf(serverPlayer().containerMenu).getNetwork()));
            int matrix = server(() -> matrixCount(serverPlayer().containerMenu, Items.CLAY_BALL));
            require(stored + matrix == inserted + 2 - 2 * crafts, crafts + " crafts, " + stored + " stored, " + matrix + " in the matrix");
            require(isStack(serverResult(), other), "the result moved off the selection");
        });

        closeScreen();
        command("clear @a");
    }

    private static int storedClay(INetwork network) {
        StackListEntry<ItemStack> entry = network.getItemStorageCache().getList().getEntry(new ItemStack(Items.CLAY_BALL), 0);
        return entry == null ? 0 : entry.getStack().getCount();
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
            SelectRecipeHandler.handle(serverPlayer(), serverPlayer().containerMenu.containerId, current);
            return null;
        });
        check("packet.valid_default_stored", () -> require(current.equals(server(this::selection)), "not stored"));

        int containerId = client(() -> menu().containerId);
        rejected("packet.rejects_wrong_container", containerId + 7, otherId, before, current);
        rejected("packet.rejects_unknown_recipe", containerId, RECIPE_UNKNOWN, before, current);
        rejected("packet.rejects_other_recipe_type", containerId, RECIPE_STONECUTTING, before, current);
        rejected("packet.rejects_non_matching_recipe", containerId, RECIPE_UNIQUE, before, current);
        rejected("packet.rejects_missing_recipe_id", containerId, null, before, current);
        check("packet.still_accepts_a_valid_recipe", () -> {
            server(() -> {
                SelectRecipeHandler.handle(serverPlayer(), containerId, otherId);
                return null;
            });
            require(otherId.equals(server(this::selection)), "a valid selection was refused");
            require(isStack(serverResult(), other(before)), "the result did not follow");
        });
        // With no grid open at all the menu is the player's inventory.
        closeScreen();
        waitTicks(3);
        check("packet.rejects_when_no_grid_open", () -> server(() -> {
            SelectRecipeHandler.handle(serverPlayer(), 0, otherId);
            return null;
        }));
        command("clear @a");
    }

    private void rejected(String name, int containerId, ResourceLocation id, ItemStack result, ResourceLocation selection) {
        check(name, () -> {
            server(() -> {
                SelectRecipeHandler.handle(serverPlayer(), containerId, id);
                return null;
            });
            require(selection.equals(server(this::selection)), "selection changed to " + server(this::selection));
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
            require(client(() -> !sideButton().visible), "button visible");
        });
        check("unique.button_click_does_nothing", () -> {
            clickSideButton();
            require(client(() -> !SelftestAccess.isPopupOpen(widget())), "a popup opened for a unique recipe");
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
        await("the conflict", 40, () -> sideButton().visible);
        clickSideButton();
        check("unique.popup_opened_for_conflict", () -> require(client(() -> SelftestAccess.isPopupOpen(widget())), "no popup"));
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
            await("the popup to close", 20, () -> !SelftestAccess.isPopupOpen(widget()));
            await("the button to hide", 20, () -> !sideButton().visible);
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
        await("the conflict", 40, () -> sideButton().visible);
        clickSideButton();
        check("plain.popup_up_before_closing", () -> require(client(() -> SelftestAccess.isPopupOpen(widget())), "no popup"));
        client(() -> SelftestCompat.key(screen(), GLFW.GLFW_KEY_ESCAPE, 1));
        check("plain.first_escape_only_closes_popup", () -> {
            waitTicks(2);
            require(client(() -> mc.screen instanceof GridScreen), "Escape closed the grid under the popup");
            require(client(() -> !SelftestAccess.isPopupOpen(widget())), "the popup is still up");
        });
        client(() -> SelftestCompat.key(screen(), GLFW.GLFW_KEY_ESCAPE, 1));
        check("plain.second_escape_closes_grid", () -> await("the screen to close", 40, () -> mc.screen == null));

        open(plain);
        check("plain.no_side_button", () -> require(client(() -> sideButtons().isEmpty()), "a plain grid got the button"));
        check("plain.no_widget", () -> require(client(() -> field(screen(), WIDGET_FIELD) == null), "a plain grid got a widget"));
        check("plain.clicks_reach_slots", () -> {
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            await("the picked up stack", 20, () -> !mc.player.inventory.getCarried().isEmpty());
            await("the server's picked up stack", 20, () -> server(() -> !serverPlayer().inventory.getCarried().isEmpty()));
            client(() -> {
                Slot slot = freeInventorySlot(menu());
                SelftestCompat.click(screen(), slotX(slot), slotY(slot), 0);
                return null;
            });
            await("the stack to go back", 20, () -> mc.player.inventory.getCarried().isEmpty());
        });
        check("plain.slot_hover_for_tooltips", () -> {
            client(() -> {
                Slot slot = inventorySlot(menu(), Items.CLAY_BALL);
                SelftestCompat.movePointer(mc, slotX(slot), slotY(slot));
                return null;
            });
            await("the slot to be hovered", 20, () -> {
                SelftestCompat.holdPointer(mc);
                Slot hovered = SelftestCompat.hoveredSlot(screen());
                return hovered != null && hovered.getItem().getItem() == Items.CLAY_BALL;
            });
        });
        waitTicks(3);
        screenshot("plain_grid_tooltip");
        closeScreen();
        command("clear @a");
    }

    // ---- 10. NBT-only variant -----------------------------------------------

    /**
     * Two plain white banners match one test recipe. A banner with a pattern is the same item with NBT,
     * and on top of the test recipe it now also matches vanilla's banner duplicate recipe: the input
     * changed only by its NBT, and the button must follow.
     */
    private void nbtVariant() throws Exception {
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
        check("nbt.plain_stacks_no_warning", () -> {
            waitTicks(5);
            require(client(() -> !sideButton().visible), "button visible with plain banners");
        });
        client(() -> {
            // Swap the second banner for the patterned one: same item, same count, more NBT.
            Slot patterned = inventorySlot(menu(), Items.WHITE_BANNER, true);
            pickup(patterned.index, 0);
            pickup(matrixSlots(menu()).get(1).index, 0);
            pickup(patterned.index, 0);
            return null;
        });
        check("nbt.variant_shows_button", () -> {
            await("the button to show", 40, () -> sideButton().visible);
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
        check("nbt.plain_again_hides_button", () -> await("the button to hide", 40, () -> !sideButton().visible));
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

        check("pattern.button_warning_when_conflict", () -> await("the side button to show", 40, () -> sideButton().visible));
        ResourceLocation otherId = conflictId(other.getItem());
        ResourceLocation firstId = conflictId(first.getItem());
        check("pattern.preview_follows_selection", () -> {
            clickSideButton();
            require(client(() -> SelftestAccess.isPopupOpen(widget())), "no popup");
            waitTicks(3);
            screenshot("pattern_popup_open");
            clickPopupEntry(other.getItem());
            await("the server's preview to change", 20, () -> isStack(serverResult(), other));
            await("the client's preview to change", 20, () -> isStack(clientResult(), other));
            require(otherId.equals(server(this::selection)), "stored: " + server(this::selection));
        });

        // Printed with the selected recipe.
        ItemStack tagged = print();
        check("pattern.print_tagged_with_selection", () -> require(otherId.equals(tagOf(tagged)), "tag: " + tagOf(tagged)));
        check("pattern.factory_gives_selection", () -> {
            ItemStack output = resolveOnServer(tagged);
            require(isStack(output, other), "the server resolves " + output);
            ItemStack clientOutput = client(() -> resolve(tagged, mc.level));
            require(isStack(clientOutput, other), "the client resolves " + clientOutput);
        });
        check("pattern.cache_gives_selection", () -> {
            ItemStack output = server(() -> firstOutput(PatternItem.fromCache(SelftestCompat.level(serverPlayer()), tagged)));
            require(isStack(output, other), "the pattern cache resolves " + output);
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
        check("pattern.factory_gives_second", () -> {
            ItemStack output = resolveOnServer(taggedFirst);
            require(isStack(output, first), "resolves " + output);
        });
        takePatternOutput();

        line("SKIP pattern.reload_restores_selection: this line keeps the grid's selection when a pattern is reloaded");

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
        await("a pattern to be printable", 40, () -> ((GridNetworkNode) grid()).canCreatePattern());
        ItemStack before = server(() -> patternOutput(serverPlayer().containerMenu).getItem().copy());
        client(() -> {
            GridScreen screen = (GridScreen) screen();
            SelftestCompat.click(screen, screen.getGuiLeft() + 172 + 8,
                    screen.getGuiTop() + screen.getTopHeight() + screen.getVisibleRows() * 18 + 22 + 8, 0);
            return null;
        });
        await("the printed pattern", 40, () -> {
            ItemStack now = server(() -> patternOutput(serverPlayer().containerMenu).getItem().copy());
            return !now.isEmpty() && !ItemStack.matches(now, before);
        });
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
        return server(() -> resolve(pattern, SelftestCompat.level(serverPlayer())));
    }

    private static ItemStack resolve(ItemStack pattern, World level) {
        return firstOutput(CraftingPatternFactory.INSTANCE.create(level, null, pattern.copy()));
    }

    private static ItemStack firstOutput(ICraftingPattern pattern) {
        if (!pattern.isValid() || pattern.getOutputs().isEmpty()) return ItemStack.EMPTY;
        return pattern.getOutputs().get(0);
    }

    private static ResourceLocation tagOf(ItemStack pattern) {
        return RecipeAccess.taggedRecipe(pattern);
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
        await("the side button to show", 40, () -> sideButton().visible);
        clickSideButton();
        clickPopupEntry(other.getItem());
        await("the selection", 20, () -> isStack(serverResult(), other) && isStack(clientResult(), other));

        closeScreen();
        waitTicks(5);
        open(pos);
        check("persist.after_reopen", () -> {
            await("the result on the server", 20, () -> isStack(serverResult(), other));
            await("the result on the client", 20, () -> isStack(clientResult(), other));
            require(otherId.equals(server(this::selection)), "stored: " + server(this::selection));
            await("the side button", 20, () -> sideButton().visible);
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
            SelftestCompat.reopenWorld(mc, WORLD);
            return null;
        }, 300);
        awaitInWorld();
        open(pos);
        check("persist.after_world_reload", () -> {
            await("the result on the server", 40, () -> isStack(serverResult(), other));
            await("the result on the client", 40, () -> isStack(clientResult(), other));
            require(otherId.equals(server(this::selection)), "stored: " + server(this::selection));
        });
        check("persist.conflict_still_detected_after_reload", () -> {
            await("the side button", 40, () -> sideButton().visible);
            clickSideButton();
            require(client(() -> popupEntries().size() == 2), "wrong entries");
            client(() -> SelftestCompat.click(screen(), 2, 2, 0));
            waitTicks(2);
        });
        closeScreen();
    }

    // ---- 11. Polymorph or Polymorph+ next to the mod ------------------------

    /**
     * Polymorph redirects the recipe lookup of the grid node, and draws its own selector over the
     * screen. Its widget's selectRecipe is what a click on one of its icons calls, so this goes through
     * its real client path and its real packet. Reflection only: it is not on the compile classpath.
     */
    private void polymorphOwnsGrids() throws Exception {
        check("polymorph.selection_mixins_left_out", () -> {
            require(!IRsPolymorphGrid.class.isAssignableFrom(GridNetworkNode.class), "MixinGridNetworkNode was applied");
            boolean hasField = false;
            for (Field f : GridScreen.class.getDeclaredFields()) hasField |= f.getName().equals(WIDGET_FIELD);
            require(!hasField, "MixinGridScreen was applied");
        });

        command("give @a minecraft:clay_ball 8");
        waitTicks(3);
        open(placeGrid("crafting_grid"));
        client(() -> {
            putEach(Items.CLAY_BALL, 2, 0, 1);
            return null;
        });
        await("the result", 40, () -> !serverResult().isEmpty());
        ItemStack first = serverResult();
        ItemStack other = other(first);
        ResourceLocation otherId = conflictId(other.getItem());

        check("polymorph.own_button_absent", () -> {
            await("the result on the client", 40, () -> !clientResult().isEmpty());
            waitTicks(5);
            require(client(() -> sideButtons().isEmpty()), "our side button is on the screen");
        });
        check("polymorph.its_widget_is_up", () -> await("Polymorph's widget", 100, () -> polymorphWidget().isPresent()));
        waitTicks(3);
        screenshot("polymorph_crafting_grid");

        int containerId = client(() -> menu().containerId);
        check("polymorph.our_packet_is_inert", () -> {
            server(() -> {
                SelectRecipeHandler.handle(serverPlayer(), containerId, otherId);
                return null;
            });
            waitTicks(3);
            require(isStack(serverResult(), first), "our handler changed the result");
        });

        check("polymorph.selection_from_its_path", () -> {
            polymorphSelect(otherId);
            await("the server's result to change", 40, () -> isStack(serverResult(), other));
            await("the client's result to change", 40, () -> isStack(clientResult(), other));
        });
        check("polymorph.take_gives_selected", () -> {
            client(() -> {
                pickup(resultSlot(menu()).index, 0);
                pickup(freeInventorySlot(menu()).index, 0);
                return null;
            });
            await("the taken item in the inventory", 40, () ->
                    server(() -> count(serverPlayer(), other.getItem())) == other.getCount());
            require(server(() -> count(serverPlayer(), first.getItem())) == 0, "the other recipe's output was given too");
        });
        closeScreen();
        command("clear @a");

        command("give @a minecraft:clay_ball 4");
        command("give @a refinedstorage:pattern 8");
        waitTicks(3);
        open(placeGrid("pattern_grid"));
        client(() -> {
            Slot patterns = emptyPatterns(menu());
            pickup(patterns.index, 0);
            pickup(patternInput(menu()).index, 0);
            ghost(Items.CLAY_BALL, 0, 1);
            return null;
        });
        await("the preview", 40, () -> !serverResult().isEmpty());
        check("polymorph.pattern_own_button_absent", () -> {
            waitTicks(5);
            require(client(() -> sideButtons().isEmpty()), "our side button is on the screen");
        });

        // Whatever Polymorph left as the player's selection, the tag must be what the grid shows.
        ItemStack shown = serverResult();
        ItemStack printedShown = print();
        check("polymorph.pattern_tagged_with_current_recipe", () -> {
            require(conflictId(shown.getItem()).equals(tagOf(printedShown)), "shows " + shown + ", tag " + tagOf(printedShown));
            require(isStack(resolveOnServer(printedShown), shown), "resolves " + resolveOnServer(printedShown));
        });
        takePatternOutput();

        ItemStack otherShown = other(shown);
        check("polymorph.pattern_follows_its_selection", () -> {
            await("Polymorph's widget", 100, () -> polymorphWidget().isPresent());
            polymorphSelect(conflictId(otherShown.getItem()));
            await("the preview to change", 40, () -> isStack(serverResult(), otherShown) && isStack(clientResult(), otherShown));
        });
        ItemStack printedOther = print();
        check("polymorph.pattern_tagged_after_its_selection", () -> {
            require(conflictId(otherShown.getItem()).equals(tagOf(printedOther)), "tag " + tagOf(printedOther));
            require(isStack(resolveOnServer(printedOther), otherShown), "resolves " + resolveOnServer(printedOther));
        });
        closeScreen();
        command("clear @a");
    }

    /** Polymorph+ keeps the later package, the original Polymorph of this Minecraft has the older one. */
    private static String polymorphPackage() {
        boolean plus = Selftest.class.getClassLoader()
                .getResource("com/illusivesoulworks/polymorph/client/recipe/RecipesWidget.class") != null;
        return plus ? "com.illusivesoulworks.polymorph" : "top.theillusivec4.polymorph";
    }

    private Optional<?> polymorphWidget() throws ReflectiveOperationException {
        Class<?> widgets = Class.forName(polymorphPackage() + ".client.recipe.RecipesWidget");
        return (Optional<?>) widgets.getMethod("get").invoke(null);
    }

    private void polymorphSelect(ResourceLocation recipe) {
        client(() -> {
            Object widget = polymorphWidget().orElseThrow(() -> new Failure("Polymorph has no widget on this screen"));
            Class<?> api = Class.forName(polymorphPackage() + ".api.client.base.IRecipesWidget");
            Method select = api.getMethod("selectRecipe", ResourceLocation.class);
            select.invoke(widget, recipe);
            return null;
        });
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
        BlockPos pos = placeBlock(block);
        await("the grid to join the network", 200, () -> mc.level.getBlockState(pos).getValue(NetworkNodeBlock.CONNECTED));
        return pos;
    }

    /**
     * RS only looks for a network when a player places a block, which /setblock is not: asks the
     * controller to scan again, what it would have done.
     */
    private BlockPos placeBlock(String block) {
        BlockPos pos = origin.offset(3 + nextGrid++, 0, 3);
        command("setblock " + at(pos) + " refinedstorage:" + block);
        await("the block on the client", 40, () -> !mc.level.getBlockState(pos).isAir());
        server(() -> {
            BlockPos controller = origin.offset(2, 0, 3);
            World level = SelftestCompat.level(serverPlayer());
            ControllerTile entity = (ControllerTile) level.getBlockEntity(controller);
            entity.getNetwork().getNodeGraph().invalidate(Action.PERFORM, level, controller);
            return null;
        });
        return pos;
    }

    private void open(BlockPos pos) {
        closeScreen();
        command("tp @a " + pos.getX() + ".5 " + pos.getY() + " " + (pos.getZ() - 2) + ".5");
        await("the player to arrive", 40, () -> mc.player.blockPosition().closerThan(pos, 3.5));
        waitTicks(10);
        client(() -> {
            SelftestCompat.useOn(mc, pos);
            return null;
        });
        await("the grid screen", 60, () -> mc.screen instanceof GridScreen);
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
            SelftestCompat.command(mc.getSingleplayerServer(), command);
            return null;
        });
    }

    private static String at(BlockPos pos) {
        return pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    private void installDatapack(Path worldDir) throws IOException {
        Path pack = worldDir.resolve("datapacks").resolve(WORLD);
        Path recipes = pack.resolve("data").resolve("rsp_selftest").resolve(SelftestCompat.recipeFolder());
        Files.createDirectories(recipes);
        writeText(pack.resolve("pack.mcmeta"), SelftestCompat.packMcmeta());
        writeText(recipes.resolve("conflict_bowl.json"), SelftestCompat.shapedPair("minecraft:clay_ball", "minecraft:bowl", 2));
        writeText(recipes.resolve("conflict_ladder.json"), SelftestCompat.shapedPair("minecraft:clay_ball", "minecraft:ladder", 3));
        writeText(recipes.resolve("unique_stick.json"), SelftestCompat.shapedPair("minecraft:flint", "minecraft:stick", 4));
        writeText(recipes.resolve("banner_plain.json"), SelftestCompat.shapedPair("minecraft:white_banner", "minecraft:bowl", 1));
    }

    private static void writeText(Path file, String text) throws IOException {
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
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

    private ContainerScreen<?> screen() {
        if (mc.screen instanceof ContainerScreen) return (ContainerScreen<?>) mc.screen;
        throw new Failure("no container screen is open, the screen is " + mc.screen);
    }

    private Container menu() {
        return screen().getMenu();
    }

    /** The grid of the open screen, on the client. */
    private GridNetworkNode grid() {
        return gridOf(menu());
    }

    private static GridNetworkNode gridOf(Container menu) {
        if (menu instanceof GridContainer && ((GridContainer) menu).getGrid() instanceof GridNetworkNode) {
            return (GridNetworkNode) ((GridContainer) menu).getGrid();
        }
        throw new Failure("the open menu is no block grid: " + menu);
    }

    private GridRecipeWidget widget() {
        GridRecipeWidget widget = (GridRecipeWidget) field(screen(), WIDGET_FIELD);
        if (widget == null) throw new Failure("no selection widget on the screen");
        return widget;
    }

    private List<RecipeSelectionSideButton> sideButtons() {
        List<RecipeSelectionSideButton> found = new ArrayList<>();
        for (Object child : screen().children()) {
            if (child instanceof RecipeSelectionSideButton) found.add((RecipeSelectionSideButton) child);
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
            SelftestCompat.click(screen(), b.x + b.getWidth() / 2.0, b.y + b.getHeight() / 2.0, 0);
            return null;
        });
        waitTicks(2);
    }

    private List<PopupEntry> popupEntries() {
        return SelftestAccess.popupEntries(widget());
    }

    private PopupEntry entryFor(Item item) {
        for (PopupEntry e : popupEntries()) {
            if (e.output().getItem() == item) return e;
        }
        throw new Failure("no popup entry for " + item + " in " + popupEntries());
    }

    /** Clicks the icon of the entry with this output, at the place the player sees it. */
    private void clickPopupEntry(Item item) {
        client(() -> {
            PopupEntry entry = entryFor(item);
            SelftestCompat.click(screen(), entry.clickX(), entry.clickY(), 0);
            return null;
        });
        waitTicks(2);
    }

    private double slotX(Slot slot) {
        return screen().getGuiLeft() + slot.x + 8;
    }

    private double slotY(Slot slot) {
        return screen().getGuiTop() + slot.y + 8;
    }

    private String gridSearchText() {
        Object search = field(screen(), "searchField");
        if (search == null) return "";
        return ((TextFieldWidget) search).getValue();
    }

    private static List<Slot> matrixSlots(Container menu) {
        GridNetworkNode node = gridOf(menu);
        List<Slot> found = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container == node.getCraftingMatrix()) found.add(slot);
        }
        found.sort(Comparator.comparingInt(Slot::getSlotIndex));
        if (found.size() != 9) throw new Failure(found.size() + " crafting matrix slots in the menu");
        return found;
    }

    /** Never through Slot#isActive: RS's pattern slots read a client-only value there. */
    private static Slot resultSlot(Container menu) {
        GridNetworkNode node = gridOf(menu);
        for (Slot slot : menu.slots) {
            if (slot.container == node.getCraftingResult()) return slot;
        }
        throw new Failure("no result slot in the menu");
    }

    private static Slot patternInput(Container menu) {
        return patternSlot(menu, 0);
    }

    private static Slot patternOutput(Container menu) {
        return patternSlot(menu, 1);
    }

    /** The two pattern slots are the only ones backed by the node's pattern handler. */
    private static Slot patternSlot(Container menu, int index) {
        GridNetworkNode node = gridOf(menu);
        require(node.getGridType() == GridType.PATTERN, "not a pattern grid");
        for (Slot slot : menu.slots) {
            if (SelftestCompat.isSlotOf(slot, node.getPatterns()) && slot.getSlotIndex() == index) return slot;
        }
        throw new Failure("no pattern slot " + index + " in the menu");
    }

    private static Slot inventorySlot(Container menu, Item item) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof PlayerInventory && slot.getItem().getItem() == item) return slot;
        }
        throw new Failure("no " + item + " in the inventory");
    }

    /** The slot with this item with or without NBT, for two stacks of one item. */
    private static Slot inventorySlot(Container menu, Item item, boolean tagged) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof PlayerInventory && slot.getItem().getItem() == item
                    && tagged == slot.getItem().hasTag()) {
                return slot;
            }
        }
        throw new Failure("no " + item + " (tagged " + tagged + ") in the inventory");
    }

    private static Slot freeInventorySlot(Container menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof PlayerInventory && slot.getItem().isEmpty()) return slot;
        }
        throw new Failure("the inventory is full");
    }

    private static Slot emptyPatterns(Container menu) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof PlayerInventory && slot.getItem().getItem() instanceof PatternItem
                    && !slot.getItem().hasTag()) {
                return slot;
            }
        }
        throw new Failure("no empty pattern in the inventory");
    }

    private void pickup(int slot, int button) {
        SelftestCompat.pickup(mc, menu().containerId, slot, button);
    }

    /** Takes a stack of the item, right-clicks {@code each} into every given matrix slot, puts the rest back. */
    private void putEach(Item item, int each, int... indexes) {
        Container menu = menu();
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
        if (!mc.player.inventory.getCarried().isEmpty()) pickup(source.index, 0);
    }

    /** Moves a whole inventory stack of the item into one matrix slot. */
    private void putStack(Item item, int index) {
        pickup(inventorySlot(menu(), item).index, 0);
        pickup(matrixSlots(menu()).get(index).index, 0);
    }

    /** Sets ghost copies in a pattern grid's matrix, replacing what they hold. */
    private void ghost(Item item, int... indexes) {
        Container menu = menu();
        Slot source = inventorySlot(menu, item);
        pickup(source.index, 0);
        for (int i : indexes) {
            pickup(matrixSlots(menu).get(i).index, 0);
        }
        pickup(source.index, 0);
    }

    private void clearMatrix() {
        Container menu = menu();
        for (Slot slot : matrixSlots(menu)) {
            if (!slot.getItem().isEmpty()) {
                pickup(slot.index, 0);
                pickup(freeInventorySlot(menu).index, 0);
            }
        }
    }

    private static int matrixCount(Container menu, Item item) {
        int total = 0;
        for (Slot slot : matrixSlots(menu)) {
            if (slot.getItem().getItem() == item) total += slot.getItem().getCount();
        }
        return total;
    }

    private static int count(PlayerEntity player, Item item) {
        PlayerInventory inventory = player.inventory;
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).getItem() == item) total += inventory.getItem(i).getCount();
        }
        return total;
    }

    private ServerPlayerEntity serverPlayer() {
        return mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
    }

    private ItemStack serverResult() {
        return server(() -> resultSlot(serverPlayer().containerMenu).getItem().copy());
    }

    private ItemStack clientResult() {
        return client(() -> resultSlot(menu()).getItem().copy());
    }

    /** The selection of the open grid's node. Server thread only. */
    private ResourceLocation selection() {
        return (ResourceLocation) field(gridOf(serverPlayer().containerMenu), SELECTED_FIELD);
    }

    private static boolean isStack(ItemStack actual, ItemStack expected) {
        return actual.getCount() == expected.getCount() && RecipeAccess.sameStack(actual, expected);
    }

    /** The output of the other conflicting recipe. */
    private static ItemStack other(ItemStack output) {
        return output.getItem() == Items.BOWL ? new ItemStack(Items.LADDER, 3) : new ItemStack(Items.BOWL, 2);
    }

    private static ResourceLocation conflictId(Item output) {
        return new ResourceLocation("rsp_selftest", output == Items.BOWL ? "conflict_bowl" : "conflict_ladder");
    }

    // -------------------------------------------------------------------------
    // Reflection on private fields of the code under test
    // -------------------------------------------------------------------------

    private static Object field(Object owner, String name) {
        try {
            return find(owner.getClass(), name).get(owner);
        } catch (ReflectiveOperationException e) {
            throw new Failure("cannot read field " + name + ": " + e);
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
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
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
            Files.write(reportFile, (full + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.error("Cannot write {}", reportFile, e);
        }
    }

    private static String oneLine(Object o) {
        String text = o instanceof Failure ? ((Failure) o).getMessage() : String.valueOf(o);
        return text.replaceAll("\\s+", " ");
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
