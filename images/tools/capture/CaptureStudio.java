package __PACKAGE__;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Scripted camera for the images of the mod page. It is not part of the mod: it is copied into the
 * sources to record footage, then removed. It plays the script named by the system property
 * {@code capture.script} or the environment variable {@code CAPTURE_SCRIPT} (an absolute path, or a
 * file of the game directory), one command per line; lines that start with # are comments.
 *
 * Written for Minecraft 1.21.1 with official (Mojang) names. On another version a few calls
 * differ (see the notes marked VERSION); the commands themselves stay the same.
 *
 * Commands:
 *   world name seed [flat] [easy|normal|hard]   open the world, or create it (creative, cheats, peaceful by default)
 *   wait ticks | log text | quit
 *   size width height            size of the window (1600 900 by default)
 *   cmd command                  any server command, as the player with every permission
 *   chat message                 what the player would type in chat (a leading / runs it as a command)
 *   time ticks | freeze          set the time of day; freeze stops the sun, the weather and the growth of plants
 *   fov n | rd chunks | guiscale n | hud on|off | notoast on|off | view first|back|front | lang code
 *   mouse x y | mouse free       hold the pointer at a point of the window, so that no tooltip shows by accident
 *   tp x y z [yaw pitch] | tpr dx dy dz | rot yaw pitch | ground [dy] | look x y z | pos
 *   biome id radius              go to the nearest biome of that kind
 *   flat radius [biome]          go to the flattest open spot around, preferably near trees
 *   scan x1 z1 x2 z2             log the height of the ground of every column, to plan a scene
 *   target player | target pos x y z [yaw] | target entity type [nth]
 *                                what `cam` and `track` look at; an entity is the nth nearest of its type
 *   cam distance height angle lookHeight [yawShift]
 *                                put the camera on a circle around the target: angle 0 faces it, from the front
 *   track on|off [dy]            keep looking at the target while it moves
 *   key name [ticks]             press a key binding by its name (key.inventory, key.use, key.modid.menu...)
 *   click x y [button]           click in the open screen, at a point given in window pixels as on a screenshot
 *   type text | press KEY        type text in the open screen, press a key (ENTER, ESCAPE, TAB, E...)
 *   quickmove n...               shift-click the player inventory slots n (0-8 hotbar, 9-35 main) in the open menu
 *   screen close
 *   shot name                    one screenshot, saved as screenshots/name.png
 *   record name ticks            one screenshot per tick, name_0001.png and so on
 *   watch x1 y1 z1 x2 y2 z2 block   while recording, log each change of the number of that block in the box
 *                                (the log lines give the frame numbers to time an overlay on)
 *
 *   mark label                   log the number of the next recorded frame, to time an overlay on what the script does
 *   until entity|block|screen|noscreen maxTicks
 *                                wait until the crosshair is on an entity or a block, or a screen is open or closed
 *   find x1 y1 z1 x2 y2 z2 block log where that block is in the box (to write the coordinates of a tree trunk in a script)
 *   status                       log the state of the window, of the pointer and of the open screen, what the crosshair is
 *                                on, and the health of the player
 *   fly on|off                   off: the camera stops making the player fly when it moves it (a survival scene, where
 *                                a flying player hovers and mines five times slower); on by default
 *   pin on|off                   hold the view where it is now, whatever the real mouse does (first person scenes)
 *   grab on|off                  act as if the pointer were captured by the game even when its window is not the active one:
 *                                without it a held attack key does not go on breaking a block in survival
 *   blur n | tilt x              strength of the blur behind menus (0 to 10) and of the tilt of the view when hurt (0 to 1)
 *
 * A player found dead when the world opens (a session that ended badly) is respawned before the script goes on.
 * The pointer is only held (`mouse x y`) while a screen is open: held in game, the next real cursor event would turn
 * the view. While recording, each change of the player's health is logged with its frame number.
 *
 * To drive something the mod has no command, key or button for, add a command of your own to `run`, next to these.
 *
 * @author vyrriox
 */
/*ANNOTATION*/
public final class CaptureStudio {
    public static final String MOD_ID = "__MODID__";
    private static final Logger LOG = LogManager.getLogger("capture");
    private static final String SCRIPT = System.getProperty("capture.script", System.getenv().getOrDefault("CAPTURE_SCRIPT", ""));

    private static List<String> lines;
    private static int pc;
    private static int wait;
    private static int idle;
    private static boolean worldAsked;
    private static boolean sized;
    private static String recName = "";
    private static int recLeft;
    private static int recIndex;
    private static boolean track;
    private static double trackDy = 1.2;
    private static boolean noToast;
    private static boolean mouseLock;
    private static double mouseX;
    private static double mouseY;
    private static KeyMapping heldKey;
    private static int heldTicks;
    private static String targetKind = "player";
    private static Vec3 targetPos = Vec3.ZERO;
    private static float targetYaw;
    private static String targetType = "";
    private static int targetNth = 1;
    private static BlockPos watchMin;
    private static BlockPos watchMax;
    private static Block watchBlock;
    private static int watchCount = -1;
    private static float lastHealth = -1;
    private static boolean flyAllowed = true;
    private static boolean forceGrab;
    private static String untilKind = "";
    private static int untilLeft;
    private static boolean pinned;
    private static float pinYaw;
    private static float pinPitch;
    private static int deadTicks;

    private CaptureStudio() {
    }

    /*HOOK*/

    public static void tick() {
        if (SCRIPT.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            step(mc);
        } catch (Throwable t) {
            LOG.error("[CAPTURE] line {} failed", pc, t);
        }
    }

    private static void step(Minecraft mc) throws Exception {
        if (lines == null) {
            lines = new ArrayList<>();
            for (String raw : Files.readAllLines(mc.gameDirectory.toPath().resolve(SCRIPT), StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    lines.add(line);
                }
            }
            LOG.info("[CAPTURE] {} commands from {}", lines.size(), SCRIPT);
        }
        if (!sized) {
            sized = true;
            GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
        }
        if (mc.player == null || mc.level == null) {
            openWorld(mc);
            return;
        }
        if (mc.getSingleplayerServer() == null) {
            return;
        }
        mc.options.pauseOnLostFocus = false;
        if (mc.player.isDeadOrDying()) {
            if (deadTicks++ % 40 == 0) {
                LOG.warn("[CAPTURE] the player is dead: respawning before the script goes on");
                mc.player.respawn();
            }
            return;
        }
        deadTicks = 0;
        if (mc.screen instanceof PauseScreen) {
            mc.setScreen(null);
        }
        if (noToast) {
            mc.getToasts().clear();                 // VERSION: getToastManager() on recent versions
        }
        if (mouseLock && mc.screen != null) {
            holdPointer(mc);
        }
        if (pinned) {
            mc.player.setYRot(pinYaw);
            mc.player.setXRot(pinPitch);
            mc.player.yRotO = pinYaw;
            mc.player.xRotO = pinPitch;
        }
        if (forceGrab && mc.screen == null && !mc.mouseHandler.isMouseGrabbed()) {
            Field grabbed = net.minecraft.client.MouseHandler.class.getDeclaredField("mouseGrabbed");
            grabbed.setAccessible(true);
            grabbed.setBoolean(mc.mouseHandler, true);
        }
        if (heldKey != null && --heldTicks <= 0) {
            heldKey.setDown(false);
            heldKey = null;
        }
        if (track) {
            Vec3 p = targetPosition(mc);
            if (p != null) {
                mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, p.add(0, trackDy, 0));
            }
        }
        if (recLeft > 0) {
            watch(mc);
            if (mc.player.getHealth() != lastHealth) {
                lastHealth = mc.player.getHealth();
                LOG.info("[CAPTURE] health {} {} {}", recName, recIndex, fmt(lastHealth));
            }
            grab(mc, String.format(Locale.ROOT, "%s_%04d.png", recName, recIndex++));
            recLeft--;
        }
        if (untilLeft > 0) {
            boolean met = switch (untilKind) {
                case "entity" -> mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY;
                case "block" -> mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
                case "screen" -> mc.screen != null;
                case "noscreen" -> mc.screen == null;
                default -> true;
            };
            if (!met && --untilLeft > 0) {
                return;
            }
            LOG.info("[CAPTURE] until {} {} {} {}", untilKind, met ? "met" : "gave up", recName, recIndex);
            untilLeft = 0;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (pc >= lines.size()) {
            return;
        }
        String line = lines.get(pc++);
        LOG.info("[CAPTURE] > {}", line);
        wait = 2;
        run(mc, line);
    }

    /** From the title screen: opens the world named by the first line of the script, or creates it. */
    private static void openWorld(Minecraft mc) {
        if (worldAsked || mc.getOverlay() != null) {
            return;
        }
        if (!(mc.screen instanceof TitleScreen)) {
            // A first launch shows other screens before the title (accessibility, warnings): skip them.
            if (++idle > 60) {
                idle = 0;
                mc.options.onboardAccessibility = false;
                mc.setScreen(new TitleScreen());
            }
            return;
        }
        worldAsked = true;
        String[] w = lines.get(0).split("\\s+");
        if (!w[0].equals("world")) {
            LOG.error("[CAPTURE] the script must start with: world <name> <seed>");
            return;
        }
        pc = 1;
        String name = w[1];
        long seed = w.length > 2 ? Long.parseLong(w[2]) : 0L;
        boolean flat = line0Has(w, "flat");
        Difficulty difficulty = line0Has(w, "hard") ? Difficulty.HARD : line0Has(w, "normal") ? Difficulty.NORMAL
                : line0Has(w, "easy") ? Difficulty.EASY : Difficulty.PEACEFUL;
        if (mc.getLevelSource().levelExists(name)) {
            LOG.info("[CAPTURE] opening world {}", name);
            // VERSION: on 1.20.1, mc.createWorldOpenFlows().loadLevel(mc.screen, name)
            mc.createWorldOpenFlows().openWorld(name, () -> LOG.error("[CAPTURE] could not open {}", name));
        } else {
            LOG.info("[CAPTURE] creating world {} seed {}", name, seed);
            LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, difficulty, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            // VERSION: on 1.20.1 createFreshLevel takes four arguments (no screen at the end)
            mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(seed, true, false),
                    flat ? access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions()
                            : WorldPresets::createNormalWorldDimensions, mc.screen);
        }
    }

    private static boolean line0Has(String[] words, String word) {
        for (int i = 3; i < words.length; i++) {
            if (words[i].equalsIgnoreCase(word)) {
                return true;
            }
        }
        return false;
    }

    private static void grab(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
        });
    }

    private static void holdPointer(Minecraft mc) {
        try {
            for (String name : new String[]{"xpos", "ypos"}) {
                Field f = net.minecraft.client.MouseHandler.class.getDeclaredField(name);
                f.setAccessible(true);
                f.setDouble(mc.mouseHandler, name.equals("xpos") ? mouseX : mouseY);
            }
        } catch (ReflectiveOperationException e) {
            mouseLock = false;
            LOG.error("[CAPTURE] cannot move the pointer", e);
        }
    }

    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        MinecraftServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(mc.player.getUUID());
            if (player != null) {
                try {
                    action.accept(player);
                } catch (Throwable t) {
                    LOG.error("[CAPTURE] server action failed", t);
                }
            }
        });
    }

    private static void command(Minecraft mc, String command) {
        onServer(mc, player -> player.getServer().getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command));
    }

    private static void fly(ServerPlayer player) {
        if (!flyAllowed) {
            return;         // a survival scene: the player stays on the ground and mines at the normal speed
        }
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();
    }

    private static int ground(ServerLevel level, double x, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        level.getChunk(bx >> 4, bz >> 4);
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
    }

    private static boolean on(String s) {
        return s.equalsIgnoreCase("on") || s.equalsIgnoreCase("true");
    }

    private static ResourceLocation id(String s) {
        return ResourceLocation.parse(s);           // VERSION: new ResourceLocation(s) before 1.21
    }

    /** Where the target of the camera is, as the client sees it. */
    private static Vec3 targetPosition(Minecraft mc) {
        Entity e = targetEntity(mc);
        if (e != null) {
            return e.position();
        }
        return targetKind.equals("pos") ? targetPos : null;
    }

    private static Entity targetEntity(Minecraft mc) {
        if (targetKind.equals("player")) {
            return null;
        }
        if (!targetKind.equals("entity")) {
            return null;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id(targetType));
        List<Entity> found = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e.getType() == type && e != mc.player) {
                found.add(e);
            }
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(mc.player)));
        return found.size() >= targetNth ? found.get(targetNth - 1) : null;
    }

    private static void watch(Minecraft mc) {
        if (watchMin == null) {
            return;
        }
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(watchMin, watchMax)) {
            if (mc.level.getBlockState(pos).is(watchBlock)) {
                count++;
            }
        }
        if (count != watchCount) {
            watchCount = count;
            LOG.info("[CAPTURE] watch {} {} {}", recName, recIndex, count);
        }
    }

    private static void run(Minecraft mc, String line) throws Exception {
        String[] a = line.split("\\s+");
        String rest = line.contains(" ") ? line.substring(line.indexOf(' ') + 1).strip() : "";
        switch (a[0]) {
            case "wait" -> wait = Integer.parseInt(a[1]);
            case "log" -> LOG.info("[CAPTURE] {}", rest);
            case "quit" -> mc.stop();
            case "size" -> GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), Integer.parseInt(a[1]), Integer.parseInt(a[2]));
            case "cmd" -> command(mc, rest);
            case "chat" -> {
                if (rest.startsWith("/")) {
                    mc.player.connection.sendCommand(rest.substring(1));
                } else {
                    mc.player.connection.sendChat(rest);
                }
            }
            case "time" -> onServer(mc, player -> player.serverLevel().setDayTime(Long.parseLong(a[1])));
            case "freeze" -> {
                command(mc, "gamerule doDaylightCycle false");
                command(mc, "gamerule doWeatherCycle false");
                command(mc, "gamerule randomTickSpeed 0");
                command(mc, "weather clear");
            }
            case "fov" -> mc.options.fov().set(Integer.parseInt(a[1]));
            case "rd" -> mc.options.renderDistance().set(Integer.parseInt(a[1]));
            case "guiscale" -> {
                mc.options.guiScale().set(Integer.parseInt(a[1]));
                mc.resizeDisplay();
                // The game lowers a scale too large for the window without a word: say what it really is.
                LOG.info("[CAPTURE] guiscale asked {}, effective {} (window {}x{}: set `size` before `guiscale`)", a[1],
                        mc.getWindow().getGuiScale(), mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight());
            }
            case "hud" -> mc.options.hideGui = !on(a[1]);
            case "notoast" -> noToast = on(a[1]);
            case "view" -> mc.options.setCameraType(a[1].equals("back") ? CameraType.THIRD_PERSON_BACK
                    : a[1].equals("front") ? CameraType.THIRD_PERSON_FRONT : CameraType.FIRST_PERSON);
            case "lang" -> {
                mc.getLanguageManager().setSelected(a[1]);
                mc.options.languageCode = a[1];
                mc.reloadResourcePacks();           // takes a few seconds: follow with `wait 260`
            }
            case "mouse" -> {
                mouseLock = !a[1].equals("free");
                if (mouseLock) {
                    mouseX = Double.parseDouble(a[1]);
                    mouseY = Double.parseDouble(a[2]);
                }
            }
            case "tp" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]),
                        a.length > 4 ? Float.parseFloat(a[4]) : player.getYRot(), a.length > 5 ? Float.parseFloat(a[5]) : player.getXRot());
            });
            case "tpr" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX() + Double.parseDouble(a[1]), player.getY() + Double.parseDouble(a[2]),
                        player.getZ() + Double.parseDouble(a[3]), player.getYRot(), player.getXRot());
            });
            case "rot" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), Float.parseFloat(a[1]), Float.parseFloat(a[2]));
            });
            case "ground" -> onServer(mc, player -> {
                fly(player);
                player.teleportTo(player.serverLevel(), player.getX(), ground(player.serverLevel(), player.getX(), player.getZ())
                        + (a.length > 1 ? Double.parseDouble(a[1]) : 0), player.getZ(), player.getYRot(), player.getXRot());
            });
            case "look" -> mc.player.lookAt(EntityAnchorArgument.Anchor.EYES,
                    new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
            case "pos" -> LOG.info("[CAPTURE] pos {} {} {} yaw {} pitch {}", fmt(mc.player.getX()), fmt(mc.player.getY()), fmt(mc.player.getZ()),
                    fmt(mc.player.getYRot()), fmt(mc.player.getXRot()));
            case "biome" -> onServer(mc, player -> {
                ServerLevel level = player.serverLevel();
                ResourceLocation biome = id(a[1]);
                var found = level.findClosestBiome3d(h -> h.is(biome), player.blockPosition(), Integer.parseInt(a[2]), 32, 64);
                if (found == null) {
                    LOG.warn("[CAPTURE] biome {} not found", biome);
                    return;
                }
                BlockPos p = found.getFirst();
                fly(player);
                player.teleportTo(level, p.getX() + 0.5, ground(level, p.getX(), p.getZ()), p.getZ() + 0.5, player.getYRot(), player.getXRot());
                LOG.info("[CAPTURE] biome {} at {} {}", biome, p.getX(), p.getZ());
            });
            case "flat" -> onServer(mc, player -> flat(player, Integer.parseInt(a[1]), a.length > 2 ? a[2] : ""));
            case "scan" -> onServer(mc, player -> {
                ServerLevel level = player.serverLevel();
                for (int z = Integer.parseInt(a[2]); z <= Integer.parseInt(a[4]); z++) {
                    StringBuilder sb = new StringBuilder();
                    for (int x = Integer.parseInt(a[1]); x <= Integer.parseInt(a[3]); x++) {
                        int g = ground(level, x, z);
                        sb.append(g).append(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > g ? "* " : "  ");
                    }
                    LOG.info("[CAPTURE] scan z={} : {}", z, sb);
                }
            });
            case "target" -> {
                targetKind = a[1];
                if (a[1].equals("pos")) {
                    targetPos = new Vec3(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]));
                    targetYaw = a.length > 5 ? Float.parseFloat(a[5]) : 0;
                } else if (a[1].equals("entity")) {
                    targetType = a[2];
                    targetNth = a.length > 3 ? Integer.parseInt(a[3]) : 1;
                }
            }
            case "cam" -> {
                // cam <distance> <eye height above the target's feet> <angle around it> <height looked at> [yaw shift]
                double dist = Double.parseDouble(a[1]);
                double height = Double.parseDouble(a[2]);
                float angle = Float.parseFloat(a[3]);
                double lookHeight = Double.parseDouble(a[4]);
                float shift = a.length > 5 ? Float.parseFloat(a[5]) : 0;
                Entity e = targetEntity(mc);
                Vec3 base = e != null ? e.position() : targetKind.equals("pos") ? targetPos : mc.player.position();
                float yaw = e != null ? e.getYRot() : targetKind.equals("pos") ? targetYaw : mc.player.getYRot();
                Vec3 eye = base.add(Vec3.directionFromRotation(0, yaw + angle).scale(dist)).add(0, height, 0);
                Vec3 look = base.add(0, lookHeight, 0);
                double dx = look.x - eye.x;
                double dy = look.y - eye.y;
                double dz = look.z - eye.z;
                float yRot = (float) (Mth.atan2(dz, dx) * 180 / Math.PI) - 90 + shift;
                float xRot = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI);
                onServer(mc, player -> {
                    fly(player);
                    player.teleportTo(player.serverLevel(), eye.x, eye.y - player.getEyeHeight(), eye.z, yRot, xRot);
                });
            }
            case "track" -> {
                track = on(a[1]);
                if (a.length > 2) {
                    trackDy = Double.parseDouble(a[2]);
                }
            }
            case "key" -> {
                KeyMapping found = null;
                for (KeyMapping k : mc.options.keyMappings) {
                    if (k.getName().equals(a[1])) {
                        found = k;
                    }
                }
                if (found == null) {
                    LOG.warn("[CAPTURE] no key binding named {}", a[1]);
                    return;
                }
                // Counts as one press even when no key is bound to it.
                Field clicks = KeyMapping.class.getDeclaredField("clickCount");
                clicks.setAccessible(true);
                clicks.setInt(found, clicks.getInt(found) + 1);
                found.setDown(true);
                heldKey = found;
                heldTicks = a.length > 2 ? Integer.parseInt(a[2]) : 2;
            }
            case "click" -> {
                Screen screen = mc.screen;
                if (screen == null) {
                    LOG.warn("[CAPTURE] click: no screen is open");
                    return;
                }
                double x = Double.parseDouble(a[1]) * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
                double y = Double.parseDouble(a[2]) * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
                int button = a.length > 3 ? Integer.parseInt(a[3]) : 0;
                screen.mouseClicked(x, y, button);
                screen.mouseReleased(x, y, button);
            }
            case "quickmove" -> {
                // quickmove n...: shift-click the player's inventory slots n (0-8 hotbar, 9-35 main) in the open menu
                if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                    var menu = cs.getMenu();
                    for (int k = 1; k < a.length; k++) {
                        int want = Integer.parseInt(a[k]);
                        for (net.minecraft.world.inventory.Slot slot : menu.slots) {
                            if (slot.container instanceof net.minecraft.world.entity.player.Inventory && slot.getContainerSlot() == want) {
                                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
                                break;
                            }
                        }
                    }
                }
            }
            case "type" -> {
                if (mc.screen != null) {
                    for (char c : rest.toCharArray()) {
                        mc.screen.charTyped(c, 0);
                    }
                }
            }
            case "press" -> {
                int code = GLFW.class.getField("GLFW_KEY_" + a[1].toUpperCase(Locale.ROOT)).getInt(null);
                if (mc.screen != null) {
                    mc.screen.keyPressed(code, 0, 0);
                } else {
                    KeyMapping.click(InputConstants.Type.KEYSYM.getOrCreate(code));
                }
            }
            case "screen" -> mc.setScreen(null);
            case "shot" -> grab(mc, a[1] + ".png");
            case "record" -> {
                recName = a[1];
                recLeft = Integer.parseInt(a[2]);
                recIndex = 1;
                watchCount = -1;
            }
            case "watch" -> {
                if (a[1].equals("off")) {
                    watchMin = null;
                } else {
                    watchMin = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                    watchMax = new BlockPos(Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6]));
                    watchBlock = BuiltInRegistries.BLOCK.get(id(a[7]));
                }
            }
            // Commands that are specific to the mod go here, next to the generic ones.
            case "mark" -> LOG.info("[CAPTURE] mark {} {} {}", rest, recName, recIndex);
            case "status" -> LOG.info("[CAPTURE] status window {}x{} gui {}x{} active {} grabbed {} screen {} focus {} crosshair {} health {}",
                    mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight(), mc.isWindowActive(), mc.mouseHandler.isMouseGrabbed(),
                    mc.screen == null ? "none" : mc.screen.getClass().getSimpleName(),
                    mc.screen == null || mc.screen.getFocused() == null ? "none" : mc.screen.getFocused().getClass().getSimpleName(),
                    mc.hitResult == null ? "none" : mc.hitResult.getType(),
                    fmt(mc.player.getHealth()));
            case "until" -> {
                untilKind = a[1];
                untilLeft = a.length > 2 ? Integer.parseInt(a[2]) : 200;
            }
            case "find" -> {
                Block wanted = BuiltInRegistries.BLOCK.get(id(a[7]));
                StringBuilder found = new StringBuilder();
                int count = 0;
                for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])),
                        new BlockPos(Integer.parseInt(a[4]), Integer.parseInt(a[5]), Integer.parseInt(a[6])))) {
                    if (mc.level.getBlockState(pos).is(wanted) && count++ < 40) {
                        found.append(pos.getX()).append(" ").append(pos.getY()).append(" ").append(pos.getZ()).append("; ");
                    }
                }
                LOG.info("[CAPTURE] find {} {}: {}", a[7], count, found);
            }
            case "grab" -> forceGrab = on(a[1]);
            case "fly" -> flyAllowed = on(a[1]);
            case "pin" -> {
                pinned = on(a[1]);
                pinYaw = mc.player.getYRot();
                pinPitch = mc.player.getXRot();
            }
            case "blur" -> mc.options.menuBackgroundBlurriness().set(Integer.parseInt(a[1]));    // VERSION: no such option before 1.20.5, remove the line
            case "tilt" -> mc.options.damageTiltStrength().set(Double.parseDouble(a[1]));
            default -> LOG.warn("[CAPTURE] unknown command {}", line);
        }
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Moves the player to the flattest dry open spot around, preferring one with trees nearby. */
    private static void flat(ServerPlayer player, int radius, String biome) {
        ServerLevel level = player.serverLevel();
        BlockPos c = player.blockPosition();
        double bestScore = Double.MAX_VALUE;
        BlockPos best = null;
        for (int dx = -radius; dx <= radius; dx += 4) {
            for (int dz = -radius; dz <= radius; dz += 4) {
                int x = c.getX() + dx;
                int z = c.getZ() + dz;
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;
                int canopy = 0;
                boolean wet = false;
                for (int ox = -4; ox <= 4; ox++) {
                    for (int oz = -4; oz <= 4; oz++) {
                        int g = ground(level, x + ox, z + oz);
                        min = Math.min(min, g);
                        max = Math.max(max, g);
                        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz) > g) {
                            canopy++;
                        }
                        if (!level.getFluidState(new BlockPos(x + ox, g - 1, z + oz)).isEmpty()) {
                            wet = true;
                        }
                    }
                }
                int y = ground(level, x, z);
                if (wet || !biome.isEmpty() && !level.getBiome(new BlockPos(x, y, z)).is(id(biome))) {
                    continue;
                }
                // The floor of a cave mouth or of a ravine is flat too: stay on the surface, under the open sky.
                if (y < level.getSeaLevel() - 2 || !level.canSeeSky(new BlockPos(x, y + 1, z))) {
                    continue;
                }
                int trees = 0;
                for (int ox = -16; ox <= 16; ox += 2) {
                    for (int oz = -16; oz <= 16; oz += 2) {
                        if ((Math.abs(ox) > 6 || Math.abs(oz) > 6)
                                && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz) > ground(level, x + ox, z + oz)) {
                            trees++;
                        }
                    }
                }
                double score = (max - min) * 8.0 + canopy * 2.0 - Math.min(trees, 60) * 0.4;
                if (score < bestScore) {
                    bestScore = score;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        if (best == null) {
            LOG.warn("[CAPTURE] no flat spot");
            return;
        }
        fly(player);
        player.teleportTo(level, best.getX() + 0.5, best.getY(), best.getZ() + 0.5, player.getYRot(), player.getXRot());
        LOG.info("[CAPTURE] flat spot {} {} {}", best.getX(), best.getY(), best.getZ());
    }
}
