# Project Rules & AI/IDE Instructions

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project | RS Polymorph |
| Mod ID | `rspolymorph` |
| Package | `com.vyrriox.rspolymorph` |
| Tech Stack | Java 21 (MC 1.21.1) / Java 25 (MC 26.1.2), MultiLoader (NeoForge + Fabric), Gradle 9.5.1 |
| Author | vyrriox |
| Organization | Team Arcadia |
| License | Apache-2.0 (NOTICE file: attribution to "vyrriox / Team Arcadia" must be kept) |
| Version | 1.2.2 |
| Dependencies | **Standalone — NO Polymorph.** Refined Storage 2.x (MC 1.21.1, tested 2.0.8) or 3.x (MC 26.1.2, tested 3.2.0) |
| Targets | 1.21.1 NeoForge ✓ · 1.21.1 Fabric ✓ · 26.1.2 NeoForge ✓ · 26.1.2 Fabric (coded, pending Loom 26.x) |
| Optional compat | Refined Storage - Quartz Arsenal >= 1.0.7 (wireless crafting grid) |

## 2. Git Workflow

| Branch | Purpose | Merges into |
|--------|---------|-------------|
| `main` | Stable releases, tagged versions; default target of pull requests | - |
| `neoforge-1.21.1` | Maintenance of the 1.21.1 NeoForge line | `main` (fixes carried over) |
| `fabric-1.21.1` | Maintenance of the 1.21.1 Fabric line | `main` (fixes carried over) |
| `neoforge-26.1.2` | Maintenance of the 26.1.2 NeoForge line | `main` (fixes carried over) |
| `feat/*` | New features | `main` |
| `fix/*` | Bug fixes | `main`, or the version branch for a fix to one line only |
| `hotfix/*` | Critical production patches | `main`, then the affected version branches |

Version branches start from `main` and keep the full multiloader tree; a fix made on one is carried to `main` and to the other lines it affects. A `fabric-26.1.2` branch will be added once that build is enabled.

**Commit conventions:** `type: descriptive message` (feat, fix, refactor, docs, perf, release)

**Release process:**
1. Bump `mod_version` in `gradle.properties`
2. Move changelog entries into the new `[X.Y.Z]` section
3. Generate `TEST_PROCEDURE_vX.Y.Z.html`
4. Tag `vX.Y.Z` on `main` → triggers `release.yml` workflow

## 3. Code Conventions

- **Language:** Code, variables, comments in English. UI text in EN + FR via lang files.
- **Naming:** PascalCase (classes), camelCase (methods/fields), UPPER_SNAKE (constants)
- **Indentation:** 4 spaces
- **Architecture:**
  - Main `@Mod` class (`RsPolymorph`) must NEVER reference client-only types directly — not even inside lambdas. The JVM verifier resolves types at class-loading time, and client classes don't exist on a dedicated server.
  - All client-only code lives in `com.vyrriox.rspolymorph.client.*` and is reached only via `FMLEnvironment.dist.isClient()`.
  - Mixins that target client classes (Screens, Widgets) are declared in the `"client"` block of `mixins.rspolymorph.json`; common mixins stay in the `"mixins"` block.
  - Polymorph ↔ RS2 bridge: `RsGridRecipeData` persists the user's selection per `RecipeType` in the Polymorph `IBlockEntityRecipeData` capability.
  - `MixinRecipeMatrix` overrides RS2's result post-resolve and MUST sync `currentRecipe` via the accessor, otherwise RS2's `currentRecipe.matches(input)` fast path will revert the preview.
  - Selection packet: `SelectRecipePacket` is the unified client→server path for both SP (local loopback) and MP. Never schedule `matrix.updateResult` with a client-side BlockEntity — always resolve the server BE via `player.containerMenu`.
  - `IRecipeDataFactory` registration must guard with `instanceof <ExpectedBE>` and return `null` for everything else. Polymorph's `createBlockEntityRecipeData` iterates a flat list and accepts the first non-null factory — a class-agnostic factory leaks `RsGridRecipeData` onto every BE in the world and pollutes Polymorph's input-keyed `RecipeCache` across recipe types (caused issue #1, the Create encased fan `ClassCastException`).
- **Do NOT:**
  - Import `net.minecraft.client.*` or `com.mojang.blaze3d.*` from any common-side class or mixin
  - Use class-name string matching to detect slot types — use `instanceof` (anonymous inner classes like `PatternGridContainerMenu$5` break `contains("DisabledSlot")`)
  - Tag patterns with recipe IDs read only from the static `selectedRecipeId` — always fall back to the per-grid store (`GridRecipeStore`) (the static is cleared between the packet and the `createCraftingPattern` call on dedicated servers)
  - Store raw client-level BlockEntity references on the server thread

## 4. Project Structure

Folders carry the Minecraft version, like the version branches. Gradle project names stay short
(`:common`, `:neoforge`, `:fabric`, `:common-261`, `:neoforge-261`) because they name the jars
(`rspolymorph-<project>-<mc>-<version>.jar`); `settings.gradle` maps each one to its folder.

```
mods-mc-rspolymorph/
├── common/
│   ├── 1.21.1/            :common        Loader-agnostic code for MC 1.21.1 / RS 2.x: ALL gameplay logic and every mixin
│   │   └── src/main/
│   │       ├── java/com/vyrriox/rspolymorph/
│   │       │   ├── RsPolymorph.java        Core (registry maps, selection state), no @Mod
│   │       │   ├── IRsRecipeMatrix.java    Duck-type interface for the RecipeMatrix accessor
│   │       │   ├── platform/               Services, NetworkPlatform, GridRecipeStore (ServiceLoader)
│   │       │   ├── client/                 Side button, popup, grid widget, tutorial card
│   │       │   ├── mixin/                  All mixins (common + "client" split in the config)
│   │       │   └── network/                SelectRecipePacket
│   │       └── resources/                  rspolymorph-common.mixins.json, assets/rspolymorph (lang, sprites), data
│   └── 26.1.2/            :common-261    MC 26.1.2 / RS 3.x: remaps the 1.21.1 sources at build time, forks the files whose API differs
├── neoforge-1.21.1/       :neoforge      NeoForge entrypoint and loader wiring only (mods.toml, network, attachments)
├── fabric-1.21.1/         :fabric        Fabric entrypoint and loader wiring only (fabric.mod.json, network, store)
├── neoforge-26.1.2/       :neoforge-261  NeoForge 26.1.2 entrypoint
├── fabric-26.1.2/                        Fabric 26.1.2 entrypoint, implemented but not built yet (issue #5)
├── buildSrc/                             MultiLoader convention plugins (multiloader-common, multiloader-loader)
├── libs/                                 rs2.jar, rs3.jar, quartz-arsenal.jar (tracked so CI can compile)
├── docs/                                 BUILDING.md, COMPATIBILITY.md
├── images/                               CurseForge page pictures and the tools that make them
├── .github/                              Issue forms, PR template, labels.json, workflows, FUNDING, COMMUNICATION
├── build.gradle, settings.gradle, gradle.properties, gradlew
└── README, CHANGELOG, CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, RULES, LICENSE, NOTICE, CURSEFORGE_PAGE
```

> Build: `./gradlew build` produces `neoforge-1.21.1/build/libs/rspolymorph-neoforge-1.21.1-<version>.jar`,
> `fabric-1.21.1/build/libs/rspolymorph-fabric-1.21.1-<version>.jar` and
> `neoforge-26.1.2/build/libs/rspolymorph-neoforge-26.1.2-<version>.jar`. `./gradlew fusejars` adds an
> optional fused `artifacts/fused/rspolymorph-<version>.jar` that loads on both loaders (1.21.1).
>
> Rule: gameplay logic and mixins go in `common/` ONLY. The loader folders contain loader wiring
> (entrypoint, registration, networking impl) and nothing else. `common` must never import a
> loader API: reach loader behaviour through `Services` (ServiceLoader). All mixins stay
> `remap=false` (targets are RS classes, never remapped on either loader).
> Paths in build files are written from the root (`rootProject.files('libs/rs2.jar')`,
> `project(':common').file(...)`), never with `../`.

### MC 26.1.2 / RS 3.x line (`common/26.1.2`, `neoforge-26.1.2`, `fabric-26.1.2`)

- Shares the 1.21.1 `common` sources via a build-time remap (`common-261:remapCommonSources`):
  `ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`. Files whose 26.x API
  differs **semantically** (recipe lookup, `assemble`, `RecipeHolder.id()` as `ResourceKey`, the
  `extractRenderState`/`extractContents` GUI pipeline) are **forked** under `common/26.1.2/src` and
  excluded from the remap (see the `forkedFor261` list in `common/26.1.2/build.gradle`).
- MC 26.1.x needs **Java 25** and runs **un-obfuscated**. Build the 26.1.2 modules with Gradle on
  JDK 25: `./gradlew :neoforge-261:build -Dorg.gradle.java.home=<jdk25>`. The default
  `./gradlew build` (Java 21) still compiles them via Gradle toolchains.
- `fabric-26.1.2` is implemented but excluded from the default build: un-obfuscated 26.x needs
  `fabric-loom` 1.17.0-alpha+, which conflicts with the stable Loom 1.16.3 used by the 1.21.1
  Fabric module (issue #5).
- NeoForge 26.x API deltas already handled: `FMLEnvironment.getDist()`, `ClientPacketDistributor`
  (`net.neoforged.neoforge.client.network`), `AttachmentType.Builder.serialize(MapCodec)`.

## 5. Adding a New Feature (Step by Step)

1. Create branch `feat/my-feature` from `main`
2. If the feature touches RS2 internals, decompile the relevant `rs2.jar` class first (`javap -p -c`) to verify field/method signatures before writing the Mixin
3. Implement common logic first (package `com.vyrriox.rspolymorph`)
4. If UI is needed, add under `client/` and register via `ClientSetup.init()`
5. If a new mixin is introduced, add to `mixins.rspolymorph.json` under `"mixins"` (common) or `"client"` (client-only)
6. If a new packet is needed, register under `RsPolymorph.registerPayloads`
7. Add translations to `common/1.21.1/src/main/resources/assets/rspolymorph/lang/{en_us,fr_fr}.json`
8. Run `./gradlew build`
9. Test in singleplayer AND dedicated server
10. Commit and PR into `main`

## 6. Testing Checklist

- [ ] `./gradlew build` passes with no warnings related to missing types
- [ ] Pattern Grid: recipe selection updates the preview immediately (no need to print an intermediate pattern)
- [ ] Pattern Grid: printed pattern is tagged with the selected recipe ID
- [ ] Autocraft resolves the tagged recipe via `MixinPatternResolver`
- [ ] Crafting Grid: recipe selection produces the chosen output on craft
- [ ] Dedicated server starts cleanly — no `ClassNotFoundException` / `NoClassDefFoundError` mentioning `net.minecraft.client.*`
- [ ] Multiplayer: two players can open separate grids without selection bleed
- [ ] No client-only class referenced from `RsPolymorph`, `RsGridRecipeData`, `SelectRecipePacket`, or common mixins
- [ ] `mixins.rspolymorph.json` correctly separates common vs client mixins

## 7. Environment Setup

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
# libs/rs2.jar, libs/rs3.jar and libs/quartz-arsenal.jar are tracked in the repo
./gradlew build
./gradlew runClient
./gradlew runServer
```

## 8. AI Assistant Instructions

1. Never add client imports to common-side code (`RsPolymorph`, `SelectRecipePacket`, `mixin/MixinPatternResolver`, `mixin/MixinPatternGrid`, `mixin/MixinCraftingGrid`, `mixin/MixinRecipeMatrix`, accessors)
2. When overriding RS2's recipe result, ALWAYS update `currentRecipe` via `AccessorRecipeMatrix.rspolymorph$setCurrentRecipe` alongside `invokeSetResult` — otherwise the preview reverts on the next tick
3. For recipe selection, always dispatch `SelectRecipePacket` (works in both SP and MP via loopback)
4. Decompile `rs2.jar` / `rs3.jar` before speculating about their internal API — field and method signatures change between RS2 versions
5. Keep `createCraftingPattern` tagging dual-source: read the static `selectedRecipeId` first, fall back to the per-grid store (`GridRecipeStore`)
6. Use `instanceof` for slot type detection — never `getClass().getName().contains(...)` (fails on anonymous inner classes)
7. Always add EN + FR translations for any new user-facing string
8. Run `./gradlew build` before committing
9. Bump the mod version ONLY when the user explicitly asks for it; default is VERSION LOCK
10. Generate `TEST_PROCEDURE_vX.Y.Z.html` on every version bump
