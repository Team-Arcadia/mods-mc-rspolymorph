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
