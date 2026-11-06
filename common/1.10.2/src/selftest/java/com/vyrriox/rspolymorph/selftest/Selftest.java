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

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.RSBlocks;
import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeGrid;
import com.raoulvdberge.refinedstorage.block.BlockController;
import com.raoulvdberge.refinedstorage.block.BlockGrid;
import com.raoulvdberge.refinedstorage.block.enums.ControllerType;
import com.raoulvdberge.refinedstorage.gui.grid.GuiGrid;
import com.raoulvdberge.refinedstorage.inventory.item.ItemHandlerBase;
import com.raoulvdberge.refinedstorage.network.MessageGridPatternCreate;
import com.raoulvdberge.refinedstorage.tile.TileNode;
import com.vyrriox.rspolymorph.IRsPolymorphGrid;
import com.vyrriox.rspolymorph.RecipeAccess;
import com.vyrriox.rspolymorph.RsPolymorph;
import com.vyrriox.rspolymorph.client.SelftestAccess;
import com.vyrriox.rspolymorph.coremod.GridTransformer;
import com.vyrriox.rspolymorph.network.RsPolymorphNetwork;
import com.vyrriox.rspolymorph.network.SelectRecipeHandler;
import com.vyrriox.rspolymorph.network.SelectRecipeMessage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

/**
 * Plays the mod in a real client and integrated server, from a thread of its own that hands each step
 * to the client or server thread. Same output as the other targets: every check prints
 * {@code RSP-SELFTEST PASS name} or {@code RSP-SELFTEST FAIL name: why}, and the run ends with
 * {@code RSP-SELFTEST DONE pass=N fail=M}, the only verdict that counts.
 *
 * Author: vyrriox
 */
public final class Selftest {

    static final String WORLD = "rsp-selftest";
    static final long WAIT_MS = 15000L;
    final ResourceLocation DIAMOND = SelftestMod.RECIPE_DIAMOND;
    final ResourceLocation EMERALD = SelftestMod.RECIPE_EMERALD;

    static int pass;
    static int fail;

    final Minecraft mc = Minecraft.getMinecraft();
    BlockPos base;
    BlockPos crafting;
    BlockPos pattern;
    BlockPos plain;
    BlockPos second;
    volatile Runnable onTick;

    private Selftest() {
    }

    public static void start() {
        Selftest test = new Selftest();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(test);
        Thread thread = new Thread(test::run, "rsp-selftest");
        thread.setDaemon(true);
        thread.start();
    }

    void run() {
        try {
            prepareFolders();
            waitFor("title screen", () -> mc.currentScreen instanceof GuiMainMenu);
            client(() -> {
                mc.gameSettings.pauseOnLostFocus = false;
                return null;
            });
            section("metadata", this::metadata);
            section("world", this::world);
            section("tutorial", this::tutorial);
            section("crafting", this::craftingGrid);
            section("isolation", this::isolation);
            section("packet", this::packets);
            section("unique", this::unique);
            section("plain", this::plainGrid);
            section("remainders", this::remainders);
            section("sync", this::syncList);
            section("pattern", () -> SelftestPatterns.run(this));
            section("persist", this::persistence);
        } catch (Throwable t) {
            t.printStackTrace();
            check("scenario", false, String.valueOf(t));
        }
        print("DONE pass=" + pass + " fail=" + fail);
        System.out.flush();
        try {
            leaveWorld();
            client(() -> {
                mc.shutdown();
                return null;
            });
        } catch (Throwable t) {
            FMLCommonHandler.instance().exitJava(fail == 0 ? 0 : 1, true);
        }
    }

    /** Runs a step from a client tick: outside the scheduled tasks, whose lock world leaving would deadlock on. */
    @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
    public void onClientTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        Runnable step = onTick;
        if (step != null && event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
            onTick = null;
            step.run();
        }
    }

    void leaveWorld() throws Exception {
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        onTick = () -> {
            try {
                if (mc.world != null) {
                    // Without the quit packet: a server that stops on it first can leave loadWorld waiting forever.
                    mc.loadWorld(null);
                }
                mc.displayGuiScreen(new GuiMainMenu());
            } finally {
                done.countDown();
            }
        };
        if (!done.await(300, TimeUnit.SECONDS)) {
            throw new IllegalStateException("the world did not close");
        }
    }

    void prepareFolders() throws Exception {
        File dir = mc.gameDir;
        Path saved = new File(dir, "saves/" + WORLD).toPath();
        if (Files.exists(saved)) {
            try (Stream<Path> files = Files.walk(saved)) {
                files.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        flagFile().delete();
    }

    File flagFile() {
        return new File(mc.gameDir, "config/rspolymorph_tutorial.flag");
    }

    // ---------------------------------------------------------------- sections

    void metadata() throws Exception {
        ModContainer mod = Loader.instance().getIndexedModList().get(RsPolymorph.MOD_ID);
        check("metadata.english_loaded", I18n.hasKey("rspolymorph.gui.recipe_selection")
                && "Recipe Selection".equals(I18n.format("rspolymorph.gui.recipe_selection")), "key not translated");
        try (java.io.InputStream in = mc.getResourceManager()
                .getResource(new ResourceLocation(RsPolymorph.MOD_ID, "lang/fr_FR.lang")).getInputStream()) {
            String french = new String(org.apache.commons.io.IOUtils.toByteArray(in), java.nio.charset.StandardCharsets.UTF_8);
            check("metadata.french_loaded", french.contains("rspolymorph.gui.recipe_selection=S\u00e9lection de recette"), "no French text");
        }
        check("metadata.version_from_mcmod_info", "1.3.0".equals(mod.getVersion()), mod.getVersion());
        check("metadata.minecraft_range_is_exact",
                mod.acceptableMinecraftVersionRange().containsVersion(new DefaultArtifactVersion("1.12.2"))
                        && !mod.acceptableMinecraftVersionRange().containsVersion(new DefaultArtifactVersion("1.12.1")),
                String.valueOf(mod.acceptableMinecraftVersionRange()));
        boolean rsRequired = false;
        for (ArtifactVersion required : mod.getRequirements()) {
            rsRequired |= "refinedstorage".equals(required.getLabel())
                    && required.containsVersion(new DefaultArtifactVersion("1.6.16"))
                    && !required.containsVersion(new DefaultArtifactVersion("1.6.15"));
        }
        check("metadata.requires_refinedstorage_floor", rsRequired, String.valueOf(mod.getRequirements()));
    }

    void world() throws Exception {
        joinWorld(true);
        check("world.recipes_loaded", RecipeAccess.byId(DIAMOND) != null && RecipeAccess.byId(EMERALD) != null, "missing");
        server(s -> {
            WorldServer world = s.worldServerForDimension(0);
            base = world.getSpawnPoint().up(8);
            crafting = base.east();
            pattern = base.west();
            plain = base.north();
            second = base.south();
            world.setBlockState(base, RSBlocks.CONTROLLER.getDefaultState().withProperty(BlockController.TYPE, ControllerType.CREATIVE));
            world.setBlockState(crafting, gridState(GridType.CRAFTING));
            world.setBlockState(pattern, gridState(GridType.PATTERN));
            world.setBlockState(plain, gridState(GridType.NORMAL));
            world.setBlockState(second, gridState(GridType.CRAFTING));
            EntityPlayerMP player = player(s);
            player.setPositionAndUpdate(base.getX() + 0.5, base.getY() + 2, base.getZ() + 0.5);
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            return null;
        });
        pause(500);
        Class.forName("com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingPattern");
        check("audit.all_hooks_applied", GridTransformer.applied().size() == 6, GridTransformer.applied().toString());
        check("audit.node_implements_interface",
                server(s -> node(s, crafting) instanceof IRsPolymorphGrid), "no interface");
    }

    IBlockState gridState(GridType type) {
        return RSBlocks.GRID.getDefaultState().withProperty(BlockGrid.TYPE, type);
    }

    void tutorial() throws Exception {
        open(crafting);
        check("tutorial.shows_on_first_open", client(SelftestAccess::tutorialShowing), "no card");
        pause(600);
        screenshot("rsp-tutorial.png");
        waitFor("flag file", () -> flagFile().exists());
        pass("tutorial.marks_state_file");
        check("tutorial.space_dismisses", client(() -> SelftestAccess.key(Keyboard.KEY_SPACE)) && !client(SelftestAccess::tutorialShowing), "still showing");
        closeScreen();
        open(crafting);
        check("tutorial.hidden_on_second_open", !client(SelftestAccess::tutorialShowing), "shown again");
        closeScreen();

        flagFile().delete();
        client(() -> {
            SelftestAccess.resetTutorialState();
            return null;
        });
        open(crafting);
        check("tutorial.shows_again_without_flag", client(SelftestAccess::tutorialShowing), "no card");
        client(() -> {
            SelftestAccess.click(mc.currentScreen, 5, 5);
            return null;
        });
        check("tutorial.click_dismisses", !client(SelftestAccess::tutorialShowing), "still showing");
        closeScreen();
    }

    void craftingGrid() throws Exception {
        open(crafting);
        check("crafting.button_inactive_when_empty", !client(SelftestAccess::buttonVisible), "visible");

        fillConflict(crafting, 16);
        waitFor("two candidates", () -> SelftestAccess.candidates() == 2);
        ItemStack defaultResult = waitResult();
        check("crafting.default_result_on_client", isItem(defaultResult, Items.DIAMOND) || isItem(defaultResult, Items.EMERALD), str(defaultResult));
        Item other = isItem(defaultResult, Items.DIAMOND) ? Items.EMERALD : Items.DIAMOND;
        ResourceLocation otherId = other == Items.EMERALD ? EMERALD : DIAMOND;
        pause(100);
        check("crafting.button_warning_when_conflict", client(SelftestAccess::buttonVisible), "hidden");
        check("crafting.button_on_the_screen", client(() -> SelftestAccess.hasOurSideButton(mc.currentScreen)), "not in the button list");

        // The re-init Refined Storage does with no Forge event must not lose the button.
        client(() -> {
            mc.currentScreen.initGui();
            return null;
        });
        pause(300);
        check("crafting.button_survives_rs_reinit", client(() -> SelftestAccess.hasOurSideButton(mc.currentScreen)), "lost");

        // Nothing scans while the matrix does not change.
        int scans = client(SelftestAccess::scans);
        pause(1500);
        check("crafting.no_scan_without_change", client(SelftestAccess::scans) == scans, "scanned again");

        clickButton();
        check("crafting.button_opens_popup", client(SelftestAccess::popupOpen), "closed");
        client(() -> {
            SelftestAccess.click(mc.currentScreen, mc.currentScreen.width - 1, 1);
            return null;
        });
        check("crafting.click_outside_closes_popup", !client(SelftestAccess::popupOpen), "open");

        clickButton();
        List<ResourceLocation> ids = client(SelftestAccess::popupIds);
        check("crafting.popup_lists_both_outputs", ids.contains(DIAMOND) && ids.contains(EMERALD) && ids.size() == 2, ids.toString());
        hoverCheck();
        pause(300);
        screenshot("rsp-popup.png");

        client(() -> {
            SelftestAccess.clickPopupCell(mc.currentScreen, ids.indexOf(otherId));
            return null;
        });
        check("crafting.pick_closes_popup", !client(SelftestAccess::popupOpen), "open");
        waitFor("selected result on the client", () -> isItem(resultOnClient(), other));
        pass("crafting.pick_other_recipe");
        check("crafting.selection_stored_on_server",
                otherId.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "not stored");
        check("crafting.result_on_server", server(s -> isItem(node(s, crafting).getCraftingResult().getStackInSlot(0), other)), "other result");

        clickButton();
        check("crafting.popup_reopens_with_both", client(SelftestAccess::popupIds).size() == 2, "wrong size");
        clickButton();
        check("crafting.button_closes_popup", !client(SelftestAccess::popupOpen), "open");

        // Take one with a real window click.
        clientClick(ClickType.PICKUP);
        pause(300);
        check("crafting.take_one_gives_selected", server(s -> isItem(player(s).inventory.getItemStack(), other)), "wrong stack in hand");
        check("crafting.take_one_used_one_input", server(s -> node(s, crafting).getCraftingMatrix().getStackInSlot(0).stackSize == 15), "count");
        check("crafting.result_stays_selected", server(s -> isItem(node(s, crafting).getCraftingResult().getStackInSlot(0), other)), "reverted");
        server(s -> {
            player(s).inventory.setItemStack(null);
            player(s).updateHeldItem();
            player(s).inventory.clear();
            return null;
        });

        clientClick(ClickType.QUICK_MOVE);
        pause(500);
        int gotOther = server(s -> player(s).inventory.clearMatchingItems(other, -1, 0, null));
        int gotDefault = server(s -> player(s).inventory.clearMatchingItems(other == Items.EMERALD ? Items.DIAMOND : Items.EMERALD, -1, 0, null));
        check("crafting.shift_craft_full_stack", gotOther == 15 && gotDefault == 0, "selected=" + gotOther + " default=" + gotDefault);

        // A selection that stops matching is kept, and applies again when its inputs come back.
        check("crafting.selection_kept_when_empty", otherId.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "lost");
        fillConflict(crafting, 16);
        waitFor("selection back on the client", () -> isItem(resultOnClient(), other));
        pass("crafting.selection_applies_again");
        closeScreen();
    }

    void isolation() throws Exception {
        ResourceLocation first = server(s -> grid(s, crafting).rspolymorph$getSelected());
        check("isolation.second_grid_has_no_selection", server(s -> grid(s, second).rspolymorph$getSelected()) == null, "shared");
        open(second);
        fillConflict(second, 2);
        waitFor("two candidates", () -> SelftestAccess.candidates() == 2);
        ResourceLocation pick = DIAMOND.equals(first) ? EMERALD : DIAMOND;
        int window = client(() -> mc.player.openContainer.windowId);
        client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(window, pick));
            return null;
        });
        pause(300);
        check("isolation.selection_on_this_grid_only",
                pick.equals(server(s -> grid(s, second).rspolymorph$getSelected()))
                        && first.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "bled");
        closeScreen();
    }

    void packets() throws Exception {
        open(crafting);
        pause(200);
        int window = client(() -> mc.player.openContainer.windowId);
        ResourceLocation current = server(s -> grid(s, crafting).rspolymorph$getSelected());
        ResourceLocation valid = EMERALD.equals(current) ? DIAMOND : EMERALD;

        rejected("packet.rejects_unknown_recipe", window, new ResourceLocation("rspolymorph_selftest", "nope"), current);
        rejected("packet.rejects_recipe_not_matching", window, SelftestMod.RECIPE_STICK, current);
        rejected("packet.rejects_wrong_window", window + 1, valid, current);

        // Through the real channel: too long an id is dropped before the handler, no disconnect.
        client(() -> {
            StringBuilder huge = new StringBuilder("a:");
            for (int i = 0; i < 600; i++) {
                huge.append('b');
            }
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(window, new ResourceLocation(huge.toString())));
            return null;
        });
        pause(300);
        check("packet.oversized_id_ignored", current.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())) && client(() -> mc.getConnection() != null), "changed or dropped");

        server(s -> {
            SelectRecipeHandler.handle(player(s), window, valid);
            return null;
        });
        check("packet.still_accepts_a_valid_recipe", valid.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "refused");
        server(s -> {
            SelectRecipeHandler.handle(player(s), window, current);
            return null;
        });
        closeScreen();
        pause(200);
        rejected("packet.rejects_when_no_grid_open", window, valid, current);
    }

    void rejected(String name, int window, ResourceLocation recipe, ResourceLocation expected) throws Exception {
        server(s -> {
            SelectRecipeHandler.handle(player(s), window, recipe);
            return null;
        });
        check(name, expected.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "selection changed");
    }

    void unique() throws Exception {
        open(second);
        server(s -> {
            clearMatrix(node(s, second));
            node(s, second).getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.LOG, 4));
            return null;
        });
        waitFor("unique result", () -> isItem(resultOnClient(), Item.getItemFromBlock(Blocks.PLANKS)));
        pause(200);
        check("unique.no_warning", !client(SelftestAccess::buttonVisible) && client(SelftestAccess::candidates) == 0, "warned");
        check("unique.no_side_button", !client(() -> SelftestAccess.hasOurSideButton(mc.currentScreen)), "visible");

        fillConflict(second, 2);
        waitFor("conflict", () -> SelftestAccess.candidates() == 2);
        pause(100);
        clickButton();
        check("unique.popup_opened_for_conflict", client(SelftestAccess::popupOpen), "closed");
        server(s -> {
            clearMatrix(node(s, second));
            node(s, second).getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.DIRT));
            return null;
        });
        waitFor("no candidates", () -> SelftestAccess.candidates() == 0);
        check("unique.non_matching_clears_result", client(() -> resultOnClient() == null), "still a result");
        check("unique.non_matching_closes_popup", !client(SelftestAccess::popupOpen), "open");
        server(s -> {
            clearMatrix(node(s, second));
            return null;
        });
        closeScreen();
    }

    void plainGrid() throws Exception {
        open(plain);
        pause(200);
        check("plain.no_widget", !client(SelftestAccess::hasWidget), "widget");
        check("plain.no_side_button", !client(() -> SelftestAccess.hasOurSideButton(mc.currentScreen)), "button");
        int window = client(() -> mc.player.openContainer.windowId);
        server(s -> {
            SelectRecipeHandler.handle(player(s), window, DIAMOND);
            return null;
        });
        check("plain.packet_ignored", server(s -> grid(s, plain).rspolymorph$getSelected()) == null, "stored");
        closeScreen();
    }

    /** The remainders follow the recipe the grid shows, not the first match of vanilla. */
    void remainders() throws Exception {
        BlockPos at = base.down();
        server(s -> {
            s.worldServerForDimension(0).setBlockState(at, gridState(GridType.CRAFTING));
            return null;
        });
        pause(300);
        open(at);
        fillRemainderConflict(at);
        boolean coalFirst = waitResult().getItem() == Items.COAL;
        Item defaultRemainder = coalFirst ? Items.STICK : Items.BONE;
        Item otherRemainder = coalFirst ? Items.BONE : Items.STICK;
        Item otherOutput = coalFirst ? Items.REDSTONE : Items.COAL;
        ResourceLocation otherId = coalFirst ? SelftestMod.RECIPE_REMAINDER_BONE : SelftestMod.RECIPE_REMAINDER_STICK;

        clientClick(ClickType.PICKUP);
        pause(300);
        check("crafting.remainders_of_default_recipe",
                server(s -> isItem(node(s, at).getCraftingMatrix().getStackInSlot(0), defaultRemainder)),
                server(s -> str(node(s, at).getCraftingMatrix().getStackInSlot(0))));
        emptyHand();

        fillRemainderConflict(at);
        waitResult();
        client(() -> {
            RsPolymorphNetwork.channel().sendToServer(new SelectRecipeMessage(mc.player.openContainer.windowId, otherId));
            return null;
        });
        waitFor("selected remainder recipe", () -> isItem(resultOnClient(), otherOutput));
        clientClick(ClickType.PICKUP);
        pause(300);
        check("crafting.remainders_follow_selection",
                server(s -> isItem(node(s, at).getCraftingMatrix().getStackInSlot(0), otherRemainder)),
                server(s -> str(node(s, at).getCraftingMatrix().getStackInSlot(0))));
        emptyHand();
        closeScreen();
    }

    /** Where Forge has no tooltip event, the overlay has its own way to hide them: see SelftestHover. */
    void hoverCheck() throws Exception {
        Class<?> type;
        try {
            type = Class.forName("com.vyrriox.rspolymorph.selftest.SelftestHover");
        } catch (ClassNotFoundException e) {
            return;
        }
        String problem = client(() -> (String) type.getMethod("problem").invoke(null));
        check("crafting.overlay_hides_screen_tooltips", problem == null, problem);
    }

    /** Where the selection is synced, a change reaches everyone with the grid open: see SelftestSync. */
    void syncList() throws Exception {
        Class<?> type = syncCheck();
        if (type == null) {
            skip("sync.sent_to_every_viewer", "Refined Storage 1.6 and later send the result slot, there is no selection message");
            return;
        }
        open(crafting);
        String problem = server(s -> (String) type.getMethod("viewers", WorldServer.class, EntityPlayerMP.class, Object.class, Object.class)
                .invoke(null, s.worldServerForDimension(0), player(s), node(s, crafting), node(s, second)));
        check("sync.sent_to_every_viewer", problem == null, problem);
        closeScreen();
    }

    static Class<?> syncCheck() {
        try {
            return Class.forName("com.vyrriox.rspolymorph.selftest.SelftestSync");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    void fillRemainderConflict(BlockPos pos) throws Exception {
        server(s -> {
            NetworkNodeGrid n = node(s, pos);
            clearMatrix(n);
            n.getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Items.CLAY_BALL));
            n.getCraftingMatrix().setInventorySlotContents(1, new ItemStack(Blocks.SAND));
            return null;
        });
    }

    void emptyHand() throws Exception {
        server(s -> {
            player(s).inventory.setItemStack(null);
            player(s).updateHeldItem();
            player(s).inventory.clear();
            return null;
        });
        pause(100);
    }

    void persistence() throws Exception {
        ResourceLocation craftingSelected = server(s -> grid(s, crafting).rspolymorph$getSelected());
        Item craftingItem = EMERALD.equals(craftingSelected) ? Items.EMERALD : Items.DIAMOND;
        ResourceLocation patternSelected = server(s -> grid(s, pattern).rspolymorph$getSelected());
        Item patternItem = EMERALD.equals(patternSelected) ? Items.EMERALD : Items.DIAMOND;

        open(crafting);
        waitFor("result after reopen", () -> isItem(resultOnClient(), craftingItem));
        pass("persist.after_reopen");
        closeScreen();

        leaveWorld();
        waitFor("title screen", () -> mc.world == null && mc.getIntegratedServer() == null);
        pass("persist.world_left");

        joinWorld(false);
        pause(500);
        check("persist.selection_read_back", craftingSelected.equals(server(s -> grid(s, crafting).rspolymorph$getSelected())), "lost");
        check("persist.result_on_server_after_load", server(s -> isItem(node(s, crafting).getCraftingResult().getStackInSlot(0), craftingItem)), "other result");
        open(crafting);
        waitFor("result after reload", () -> isItem(resultOnClient(), craftingItem));
        pass("persist.after_world_reload");
        waitFor("conflict after reload", () -> SelftestAccess.candidates() == 2);
        pass("persist.conflict_still_detected_after_reload");
        closeScreen();

        open(pattern);
        waitFor("pattern result after reload", () -> isItem(resultOnClient(), patternItem));
        pass("persist.pattern_grid_after_reload");
        closeScreen();
    }

    // ---------------------------------------------------------------- world and grid steps

    void joinWorld(boolean create) throws Exception {
        client(() -> {
            WorldSettings settings = null;
            if (create) {
                settings = new WorldSettings(12345L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
            }
            mc.launchIntegratedServer(WORLD, WORLD, settings);
            return null;
        });
        waitFor("world", 120000L, () -> mc.world != null && mc.player != null && mc.getIntegratedServer() != null
                && mc.getIntegratedServer().getPlayerList().getCurrentPlayerCount() > 0 && mc.currentScreen == null);
    }

    void open(BlockPos pos) throws Exception {
        server(s -> {
            WorldServer world = s.worldServerForDimension(0);
            IBlockState state = world.getBlockState(pos);
            state.getBlock().onBlockActivated(world, pos, state, player(s), EnumHand.MAIN_HAND, null, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
            return null;
        });
        waitFor("grid screen", () -> mc.currentScreen instanceof GuiGrid && mc.player.openContainer.windowId != 0);
        pause(200);
    }

    void closeScreen() throws Exception {
        client(() -> {
            mc.player.closeScreen();
            return null;
        });
        waitFor("closed", () -> mc.currentScreen == null);
        pause(100);
    }

    void clickButton() throws Exception {
        client(() -> {
            SelftestAccess.clickButton(mc.currentScreen);
            return null;
        });
    }

    /** A real window click on the result slot, as the mouse would send it. */
    void clientClick(ClickType type) throws Exception {
        client(() -> {
            int slotNumber = -1;
            NetworkNodeGrid clientNode = SelftestClientNode.of(mc);
            for (Slot slot : mc.player.openContainer.inventorySlots) {
                if (slot.inventory == clientNode.getCraftingResult()) {
                    slotNumber = slot.slotNumber;
                }
            }
            mc.playerController.windowClick(mc.player.openContainer.windowId, slotNumber, 0, type, mc.player);
            return null;
        });
    }

    ItemStack print() throws Exception {
        server(s -> {
            ((ItemHandlerBase) node(s, pattern).getPatterns()).setStackInSlot(1, null);
            return null;
        });
        pause(100);
        client(() -> {
            RS.INSTANCE.network.sendToServer(new MessageGridPatternCreate(pattern.getX(), pattern.getY(), pattern.getZ()));
            return null;
        });
        waitForServer("printed pattern", s -> node(s, pattern).getPatterns().getStackInSlot(1) != null);
        return server(s -> node(s, pattern).getPatterns().getStackInSlot(1).copy());
    }

    void fillConflict(BlockPos pos, int count) throws Exception {
        server(s -> {
            NetworkNodeGrid n = node(s, pos);
            clearMatrix(n);
            n.getCraftingMatrix().setInventorySlotContents(0, new ItemStack(Blocks.GRAVEL, count));
            n.getCraftingMatrix().setInventorySlotContents(1, new ItemStack(Items.FLINT, count));
            return null;
        });
    }

    static void clearMatrix(NetworkNodeGrid n) {
        for (int i = 0; i < n.getCraftingMatrix().getSizeInventory(); i++) {
            n.getCraftingMatrix().setInventorySlotContents(i, null);
        }
    }

    ItemStack waitResult() throws Exception {
        waitFor("a result", () -> resultOnClient() != null);
        return client(this::resultOnClient);
    }

    ItemStack resultOnClient() {
        return SelftestClientNode.of(mc).getCraftingResult().getStackInSlot(0);
    }

    static boolean isItem(ItemStack stack, Item item) {
        return stack != null && stack.getItem() == item;
    }

    static String str(ItemStack stack) {
        return stack == null ? "null" : stack + " " + stack.getTagCompound();
    }

    void screenshot(String name) throws Exception {
        client(() -> {
            ScreenShotHelper.saveScreenshot(mc.gameDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            return null;
        });
    }

    static NetworkNodeGrid node(MinecraftServer s, BlockPos pos) {
        return (NetworkNodeGrid) ((TileNode<?>) s.worldServerForDimension(0).getTileEntity(pos)).getNode();
    }

    static IRsPolymorphGrid grid(MinecraftServer s, BlockPos pos) {
        return (IRsPolymorphGrid) node(s, pos);
    }

    static EntityPlayerMP player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    // ---------------------------------------------------------------- threads and output

    interface Section {
        void run() throws Exception;
    }

    interface ServerStep<T> {
        T run(MinecraftServer server) throws Exception;
    }

    interface ServerCondition {
        boolean test(MinecraftServer server) throws Exception;
    }

    void section(String name, Section section) {
        print("SECTION " + name);
        try {
            section.run();
        } catch (Throwable t) {
            t.printStackTrace();
            check("section." + name, false, String.valueOf(t));
            try {
                client(() -> {
                    if (mc.player != null && mc.currentScreen != null) {
                        mc.player.closeScreen();
                    }
                    return null;
                });
            } catch (Throwable ignored) {
                // The next section reports its own failure.
            }
        }
    }

    <T> T client(Callable<T> step) throws Exception {
        return mc.addScheduledTask(step).get(60, TimeUnit.SECONDS);
    }

    <T> T server(ServerStep<T> step) throws Exception {
        MinecraftServer s = FMLCommonHandler.instance().getMinecraftServerInstance();
        return s.callFromMainThread(() -> step.run(s)).get(60, TimeUnit.SECONDS);
    }

    void waitFor(String what, BooleanSupplier condition) throws Exception {
        waitFor(what, WAIT_MS, condition);
    }

    void waitFor(String what, long timeout, BooleanSupplier condition) throws Exception {
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            if (client(condition::getAsBoolean)) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new IllegalStateException("timed out waiting for " + what);
    }

    void waitForServer(String what, ServerCondition condition) throws Exception {
        long end = System.currentTimeMillis() + WAIT_MS;
        while (System.currentTimeMillis() < end) {
            if (server(condition::test)) {
                return;
            }
            Thread.sleep(50L);
        }
        throw new IllegalStateException("timed out waiting for " + what);
    }

    static void pause(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    static void check(String name, boolean ok, String why) {
        if (ok) {
            pass(name);
        } else {
            fail++;
            print("FAIL " + name + ": " + why);
        }
    }

    static void skip(String name, String why) {
        print("SKIP " + name + ": " + why);
    }

    static void pass(String name) {
        pass++;
        print("PASS " + name);
    }

    static void print(String text) {
        System.out.println("RSP-SELFTEST " + text);
    }

    /** The grid node of the open screen on the client. */
    static final class SelftestClientNode {
        static NetworkNodeGrid of(Minecraft mc) {
            return (NetworkNodeGrid) ((com.raoulvdberge.refinedstorage.container.ContainerGrid) mc.player.openContainer).getGrid();
        }
    }
}
