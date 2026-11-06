# Error log

## [2026-10-03 18:14] Capture camera registered under the wrong mod id
**Context:** Installing the scripted page camera into the `neoforge` subproject to record the CurseForge banners.
**Error:** The install tool reported `mod id neoforge` and wrote `MOD_ID = "neoforge"` into `CaptureStudio.java`, so its `@EventBusSubscriber(modid = ...)` pointed at a mod that does not exist.
**Root cause:** In this multiloader layout the loader subproject's `neoforge.mods.toml` uses `${mod_id}` placeholders, so the tool fell back to the folder name.
**Fix:** Set `MOD_ID = "rspolymorph"` in the temporary camera before the first run.
**Prevention:** After installing the camera in a multiloader repository, check `MOD_ID` against `mod_id` in `gradle.properties` before launching `runCapture`.

## [2026-11-06 12:44] Fabric Loom 1.18 needs Java 25 and Gradle 9.7 to even configure
**Context:** Enabling the fabric-26.1.2 target with net.fabricmc.fabric-loom 1.18.2.
**Error:** Could not resolve net.fabricmc:fabric-loom:1.18.2: Dependency requires at least JVM runtime version 25. This build uses a Java 21 JVM, then No matching variant found on the plugin API attribute.
**Root cause:** The plugin is built for a Java 25 Gradle daemon and a newer Gradle plugin API than the 9.5.1 wrapper.
**Fix:** Added gradle/gradle-daemon-jvm.properties (toolchainVersion=25) and moved this target's wrapper to Gradle 9.7.1, as in the other house Fabric 26.1.2 targets.
**Prevention:** Start a new Fabric 26.x target from a known-good one, copying its wrapper and daemon JVM file; the error is reported as a download failure but is a version requirement.

## [2026-11-06 12:44] Fabric development runs fail: cloth-config2 and team_reborn_energy missing
**Context:** Booting a dedicated server from fabric-1.21.1 and fabric-26.1.2.
**Error:** Mod Refined Storage requires any version of cloth-config2, which is missing; same for team_reborn_energy.
**Root cause:** Refined Storage nests both in its jar for players, but a Gradle dependency does not bring nested jars into the development classpath.
**Fix:** Added the same versions as development-only runtime dependencies (modLocalRuntime on 1.21.1, localRuntime on 26.1.2).
**Prevention:** Players are not affected. When bumping Refined Storage, check META-INF/jars of its jar and keep these versions in step.

## [2026-11-06 13:02] Deleted common source still compiled on the 26.1.2 targets
**Context:** Removing MixinAbstractGridContainerMenu from common/1.21.1 during the 1.3.0 refactor, then building neoforge-26.1.2 and fabric-26.1.2.
**Error:** cannot find symbol: method unregisterMatrix(RecipeMatrixContainer) in build/remapped-common/java/.../MixinAbstractGridContainerMenu.java
**Root cause:** remapCommonSources and mergeResources were Copy tasks: they never delete from build/remapped-common or build/merged-resources, so a source removed from common stayed in the generated tree.
**Fix:** Made both tasks Sync in neoforge-26.1.2/build.gradle and fabric-26.1.2/build.gradle.
**Prevention:** Generated source folders must be produced by a Sync task, never Copy.

## [2026-11-06 13:23] Gradle project property named description breaks generateModMetadata
**Context:** Adding forge-1.20.1 with a mods.toml template expanded from gradle.properties
**Error:** Type ProcessResources property 'description' doesn't have a configured value
**Root cause:** Inside the task closure the names description and license resolve to fields of the task or project first, so the task's null description was passed to inputs.properties
**Fix:** Renamed the properties to mod_description, mod_license and mod_credits
**Prevention:** Prefix every metadata property with mod_ in target gradle.properties

## [2026-11-06 13:23] Polymorph+ jar does not run in a ModDevGradle legacy dev run
**Context:** Proving coexistence of forge-1.20.1 with Polymorph+ 1.4.2 through modRuntimeOnly
**Error:** polymorph.mixins.json:MixinSmithingMenu @Shadow field f_40242_ was not located in the target class, then NoSuchMethodError on Commands and MinecraftServer SRG methods
**Root cause:** The jar keeps SRG member names in its mixin classes and in its nested spectrelib jar, which the dev remap does not rewrite. Nothing to do with RS Polymorph
**Fix:** For the proof only, a scratch copy of the jar with its SRG constants renamed to dev names, including the nested jar; the original stays untouched
**Prevention:** Dev-run coexistence proofs with Polymorph+ need the renamed copy; mixin failures in polymorph.mixins.json in dev are not a bug of this mod

## [2026-11-06 13:48] Slot isActive of RS pattern slots throws on the server
**Context:** Selftest reading the Pattern Grid result slot from the server menu, with Slot isActive to skip the hidden smithing slot.
**Error:** ClassCastException: ServerProperty cannot be cast to ClientProperty, thrown from the anonymous slots of PatternGridContainerMenu.
**Root cause:** RS reads its pattern type through a client-only property cast in isActive, which only ever runs client side in RS itself.
**Fix:** The selftest locates the preview slot by position, right after the crafting matrix slots.
**Prevention:** Never call Slot isActive on an RS pattern grid menu from server code, including in mod logic.

## [2026-11-06 13:48] Exit 0 does not prove a Fabric dedicated server ran
**Context:** Using runServer on fabric-1.21.1 and fabric-26.1.2 as server proof.
**Error:** OK (exit 0) although the server stopped at You need to agree to the EULA, before Done.
**Root cause:** A server without an accepted EULA exits normally and eula.txt stays false in a development run.
**Fix:** Added runServerSelftest through the game test launch, which needs no EULA, and which reports RSP-SELFTEST lines.
**Prevention:** Trust the RSP-SELFTEST DONE line of the run, never the exit code. Never accept the EULA for the user.

## [2026-11-06 13:50] NeoForge 20.4 does not read neoforge.mods.toml
**Context:** Adding the neoforge-1.20.4 target, whose brief asked for neoforge.mods.toml
**Error:** Metadata name in the brief does not match the loader: FML 2.0.17 only looks for META-INF/mods.toml
**Root cause:** neoforge.mods.toml arrived after 1.20.4; FML 2.0.17 and the NeoForge 20.4.253 and RS 1.13 jars all use mods.toml
**Fix:** Target ships META-INF/mods.toml with type=required dependencies and a mixins entry
**Prevention:** Check the loader jar for the metadata file name on any NeoForge line older than 1.20.5

## [2026-11-06 14:39] Forge 39 dev runs die on UnionFileSystem
**Context:** Running runClient and runGametest of forge-1.18.1 through ModDevGradle legacy
**Error:** InaccessibleObjectException: module java.base does not opens java.lang.invoke to cpw.mods.securejarhandler
**Root cause:** The Forge 39 userdev run config omits that add-opens, later lines declare it
**Fix:** jvmArgument --add-opens=java.base/java.lang.invoke=cpw.mods.securejarhandler in forge-1.18.1/build.gradle
**Prevention:** When a Forge line below 40 exits at once in dev, compare its VM args with the next line

## [2026-11-06 14:52] Occultism on 26.1 breaks world exit without JEI
**Context:** Compat selftest on neoforge-26.1.2 with the Polymorph family
**Error:** NoClassDefFoundError mezz/jei/api/IModPlugin from Occultism JeiPlugin ClientRecipeSync.onLoggingOut, then 9 stopped FAIL lines; the pinned NeoForge build was also refused by Occultism and Modonomicon
**Root cause:** Occultism registers a logout listener in its JEI plugin class while declaring JEI optional; not related to this mod
**Fix:** Ran with a newer NeoForge through -P neoforge_version and with JEI in the mods folder
**Prevention:** Read a stopped FAIL stack trace first: it may come from another mod; add its optional dependency for the run

## [2026-11-06 14:55] Filter on a ProcessResources task is invisible to Gradle up-to-date checks
**Context:** Adding the selftest mixin config to the mods.toml of neoforge-1.20.4 with filesMatching and filter, as neoforge-1.21.1 does
**Error:** The run compiled the selftest but the game never loaded its mixin, so no report file and the client stayed on the title screen
**Root cause:** A filter closure is not a task input: the task stayed up to date or came from the build cache, and on this ProcessResources task the filter sees the template after expand
**Fix:** Appended the block in doLast and declared the config name with inputs.property
**Prevention:** Any conditional output of a metadata task needs a declared input; check the mods.toml in build/resources after the change

## [2026-11-06 14:55] Forge 1.20.1 builds GameTestServer without its create factory
**Context:** Porting MixinGameTestServerSelftest to forge-1.20.1 with the injector on create that the modern line uses
**Error:** All 0 required tests passed and no rspolymorph-selftest-server.txt, loop verdict OK
**Root cause:** Forge calls the GameTestServer constructor, so create never runs; NeoForge 1.20.4 calls create, which refuses an empty test list before initServer
**Fix:** The selftest mixin injects at the head of both create and initServer, ServerSelftest runs once
**Prevention:** Only the DONE line counts: an OK verdict with 0 tests proved nothing

## [2026-11-06 14:55] RS 1.x nodes placed with setblock never join the network
**Context:** Selftest placing a creative controller and grids with the setblock command
**Error:** timed out waiting for the grid to join the network; grids worked but with no network
**Root cause:** RS discovers nodes from the entity place event only, which setblock does not fire, so the first run tested the no-network path of RS
**Fix:** The selftest asks the controller's node graph to invalidate after each placed block and waits for the connected state before opening the grid
**Prevention:** A selftest on RS 1.x checks isGridActive before trusting a crafting result

## [2026-11-06 15:11] fg.deobf on a flat folder jar needs a version in the coordinate
**Context:** Adding a Polymorph+ jar to the forge-1.16.5 dev runs with -PpolymorphJar, through ForgeGradle 6
**Error:** Could not find local.polymorph:polymorph_plus-forge-1.4.2+1.16.5:null_mapped_official_1.16.5
**Root cause:** fg.deobf builds the mapped coordinate from group, name and version; a coordinate with no version turns into the literal null
**Fix:** Split the jar name at the first dash followed by a digit into name and version, a flat folder finds name-version.jar again
**Prevention:** On ForgeGradle always give fg.deobf a full group:name:version coordinate, also for local jars

## [2026-11-06 15:11] 1.16.5 selftest server has neither game test launch nor class load log
**Context:** Porting the dedicated server selftest to Forge 36 on Java 8
**Error:** No GameTestServer class in 1.16.5, -Xlog:class+load does not exist on Java 8, and four net.minecraft.client classes count as loaded on a dedicated server
**Root cause:** The game test launch came with 1.17; Main reads the EULA before Bootstrap and ServerModLoader; the mappings of the time name some common classes under net.minecraft.client
**Fix:** A selftest mixin at the head of net.minecraft.server.Main loads the mods itself and halts before eula.txt is read; ClassLoader.classes replaces the class load log; the four shared classes are listed by name
**Prevention:** On the Java 8 line prove the server distribution from Main HEAD and never accept the EULA; world and packet proofs come from the client run

## [2026-11-06 15:32] Selftest shift craft check flaky under machine load
**Context:** forge-1.16.5 client selftest with many other JVMs running, server 11 s behind
**Error:** RSP-SELFTEST FAIL crafting.shift_craft_full_stack: timed out waiting for the result, while the next check on the same selection passed
**Root cause:** Most likely a race in the test: clicks read the client copy of the inventory right after the server count matched, and the wait was 20 ticks. Inferred from the log, not reproduced in isolation
**Fix:** Wait for the client copy and for the first stack in the matrix before the next click, and 100 ticks for the result. The next run gave fail=0
**Prevention:** A selftest click must wait on the client state it reads, never only on the server state

## [2026-11-06 16:06] Forge 1.12.2 mod lang files ignored without pack.mcmeta
**Context:** First selftest screenshot of the forge-1.12.2 target
**Error:** Tutorial card shows raw keys such as rspolymorph.tutorial.title
**Root cause:** FML treats a mod with no pack.mcmeta as pack format 2 and wraps it in LegacyV2Adapter, which looks for en_US.lang rather than en_us.lang
**Fix:** Added pack.mcmeta with pack_format 3 to common/1.12.2/src/main/resources and a selftest check of both languages
**Prevention:** Any 1.11 or 1.12 target with lowercase lang files ships a pack.mcmeta of format 3

## [2026-11-06 16:06] RS re-inits GuiGrid with no Forge event and drops the side button
**Context:** Side button added from InitGuiEvent.Post on the 1.12.2 grid screen
**Error:** Selftest: crafting.button_opens_popup closed, and the button missing from the popup screenshot though visible was true
**Root cause:** TileGrid parameters call GuiBase.initGui through executeLater directly, which clears buttonList without firing InitGuiEvent
**Fix:** ClientEvents re-attaches the button when the last frame did not draw it
**Prevention:** Do not assume a button added from InitGuiEvent stays on an RS screen; check the real button list in the selftest, not only a flag

## [2026-11-06 16:06] RetroFuturaGradle 2.x needs a Java 25 Gradle JVM
**Context:** forge-1.12.2 toolchain with RFG 2.0.6 and 2.0.3 on JDK 21
**Error:** UnsupportedClassVersionError: UserDevPlugin class file version 69.0
**Root cause:** RFG 2.0.x is compiled for Java 25
**Fix:** Pinned RFG 1.4.9 on Gradle 8.14.4, which runs on JDK 21 and still compiles for Java 8
**Prevention:** Pin RFG 1.4.9 unless the daemon runs on Java 25

## [2026-11-06 16:10] Selftest reflected on development names, unusable on a production client
**Context:** Running the forge-1.16.5 selftest jar in a Forge client installed by its installer, on SRG names
**Error:** Fields hoveredSlot, xpos, ypos and the method getValue exist only under development names
**Root cause:** Reobfuscation rewrites code references but not the strings given to reflection
**Fix:** ObfuscationReflectionHelper with SRG names for the fields, a plain cast for the search box
**Prevention:** A selftest meant to prove the released jar reflects only on the mod's own names or through SRG names

## [2026-11-06 16:18] Forge 1.18.1 selftest server hook
**Context:** Porting MixinGameTestServerSelftest to forge-1.18.1
**Error:** IllegalArgumentException: No test batches were given, then Redirector failed injection check (0/1), scanned 1 target
**Root cause:** 1.18.1 has no GameTestServer.create and its private constructor throws on an empty list before initServer; method <init> matched only the first constructor
**Fix:** A 1.18.1 fork redirects Collection.isEmpty in method <init>* and starts the selftest there
**Prevention:** Read the constructors of the target when a mixin hook must run before them; use <init>* when a class has several

## [2026-11-06 16:18] Sync task keeps stale filtered sources
**Context:** Changing adaptLine in forge-1.19.2/build.gradle
**Error:** javac kept reporting the line the new filter should have rewritten
**Root cause:** Gradle does not track a filter closure as an input, so assembleCommonSources stayed up to date
**Fix:** inputs.file(buildFile) on assembleCommonSources
**Prevention:** A Sync with a filter closure declares the file holding the closure as an input
