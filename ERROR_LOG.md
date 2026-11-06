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
