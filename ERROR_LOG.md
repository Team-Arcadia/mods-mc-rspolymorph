# Error log

## [2026-10-03 18:14] Capture camera registered under the wrong mod id
**Context:** Installing the scripted page camera into the `neoforge` subproject to record the CurseForge banners.
**Error:** The install tool reported `mod id neoforge` and wrote `MOD_ID = "neoforge"` into `CaptureStudio.java`, so its `@EventBusSubscriber(modid = ...)` pointed at a mod that does not exist.
**Root cause:** In this multiloader layout the loader subproject's `neoforge.mods.toml` uses `${mod_id}` placeholders, so the tool fell back to the folder name.
**Fix:** Set `MOD_ID = "rspolymorph"` in the temporary camera before the first run.
**Prevention:** After installing the camera in a multiloader repository, check `MOD_ID` against `mod_id` in `gradle.properties` before launching `runCapture`.
