# Project Rules & AI/IDE Instructions

## 1. Project Identity

| Field | Value |
|-------|-------|
| Project | RS Polymorph |
| Mod ID | `rspolymorph` |
| Package | `com.vyrriox.rspolymorph` |
| Tech Stack | Java 8 to 25 per target, NeoForge, Forge and Fabric, Gradle 3.5 to 9.7 per target (see docs/BUILDING.md) |
| Author | vyrriox |
| Organization | Team Arcadia |
| License | Apache-2.0 (NOTICE file: attribution to "vyrriox / Team Arcadia" must be kept) |
| Version | 1.3.0 |
| Dependencies | **Standalone — NO Polymorph.** Refined Storage 3.2.0+ (26.1.2), 2.0.8+ (1.21.1), 1.x below; floor per jar in docs/COMPATIBILITY.md |
| Targets | NeoForge 26.1.2, 1.21.1, 1.20.4 · Fabric 26.1.2, 1.21.1 · Forge 1.20.1 (also NeoForge 47.1), 1.19.2, 1.18.2, 1.18.1, 1.16.5, 1.16.4, 1.16.3, 1.16.2, 1.16.1, 1.15.2, 1.15.1, 1.14.4, 1.12.2, 1.12 (also 1.12.1), 1.11.2, 1.11, 1.10.2, 1.10, 1.9.4, 1.9 |
| Optional compat | Refined Storage - Quartz Arsenal >= 1.0.7 (wireless crafting grid) |

## 2. Git Workflow

| Branch | Purpose | Merges into |
|--------|---------|-------------|
| `main` | Stable releases, tagged versions; default target of pull requests | - |
| `<loader>-<minecraft version>` | One per target folder, for example `forge-1.20.1`: maintenance of that line | `main` (fixes carried over) |
| `feat/*` | New features | `main` |
| `fix/*` | Bug fixes | `main`, or the version branch for a fix to one line only |
| `hotfix/*` | Critical production patches | `main`, then the affected version branches |

Version branches start from `main` and keep the full multiloader tree; a fix made on one is carried to `main` and to the other lines it affects. `tools/make_branches.sh` points every target branch at `main` at release time; it refuses a branch that is not a fast-forward and lists what diverged (`--dry-run` to preview).

**Commit conventions:** `type: descriptive message` (feat, fix, refactor, docs, perf, release)

**Release process:**
1. Bump `mod_version` in the `gradle.properties` of every target folder
2. Move changelog entries into the new `[X.Y.Z]` section
3. Generate `TEST_PROCEDURE_vX.Y.Z.html`
4. Tag `vX.Y.Z` on `main` → triggers `release.yml` workflow

## 3. Code Conventions

- **Language:** Code, variables, comments in English. UI text in EN + FR via lang files.
- **Naming:** PascalCase (classes), camelCase (methods/fields), UPPER_SNAKE (constants)
- **Indentation:** 4 spaces
- **Architecture:**
  - The loader entrypoints (`RsPolymorphNeoForge`, `RsPolymorphFabric`) and the core (`RsPolymorph`) must NEVER reference client-only types directly, not even inside lambdas. Client classes don't exist on a dedicated server.
  - All client-only code lives in `com.vyrriox.rspolymorph.client.*` and is reached only from the mixins of the `"client"` block.
  - Mixins that target client classes (Screens, Widgets) are declared in the `"client"` block of `rspolymorph-common.mixins.json`; common mixins stay in the `"mixins"` block. Both Minecraft lines have their own copy of that file (`common/<mc>/src/main/resources`): keep them in step.
  - No global per-grid or per-player state. The owner block entity and the wrapping matrix are `@Unique` fields on RS's `RecipeMatrixContainer` (`IRsMatrixContainer`), so they die with the grid: no static map, no cleanup hook. A grid with a block entity persists its selection per `RecipeType` through `GridRecipeStore` (a data attachment); a BlockEntity-free grid (wireless) keeps it on its `RecipeMatrix` (`IRsRecipeMatrix`). `RsPolymorph.selectionOf` / `select` hide the difference.
  - `MixinRecipeMatrix` overrides RS's result post-resolve and MUST sync `currentRecipe` via the accessor, otherwise RS's `currentRecipe.matches(input)` check will revert the preview. It must return first when RS itself returned early (client side or muted matrix), and must never run a recipe scan: use `RsPolymorph.resolve` (direct lookup, type check BEFORE `matches`).
  - Selection packet: `SelectRecipePacket` is the unified client→server path for both SP (local loopback) and MP. It carries the menu's container id, and the server validates everything: open menu, known recipe, a matrix of that type in the menu, and a match with the current input.
- **Do NOT:**
  - Import `net.minecraft.client.*` or `com.mojang.blaze3d.*` from any common-side class or mixin
  - Use class-name string matching to detect slot types — use `instanceof` (anonymous inner classes like `PatternGridContainerMenu$5` break `contains("DisabledSlot")`)
  - Tag patterns with anything but the crafting matrix's current recipe: it is what the grid previews, the stored selection may be for other inputs
  - Store raw client-level BlockEntity references on the server thread

## 4. Project Structure

Every target is a standalone Gradle project (own `build.gradle`, `settings.gradle`,
`gradle.properties` and wrapper); nothing builds from the root. Folder names carry the loader and
the Minecraft version, like the version branches. Loader-independent code lives once per Minecraft
version under `common/`, and each target adds it as a source folder (`../common/<mc>/src/main`).

```
mods-mc-rspolymorph/
├── common/
│   ├── 1.21.1/src/main/                 Loader-agnostic code for MC 1.21.1 / RS 2.x: ALL gameplay logic and every mixin
│   │   ├── java/com/vyrriox/rspolymorph/
│   │   │   ├── RsPolymorph.java         Core (data component, grid links, selection logic, no state), no @Mod
│   │   │   ├── IRsRecipeMatrix.java     Duck-type interface of the RecipeMatrix (type, input, BE-free selection)
│   │   │   ├── IRsMatrixContainer.java  Duck-type interface of the RecipeMatrixContainer (owner BE, matrix)
│   │   │   ├── platform/                Services, NetworkPlatform, GridRecipeStore (ServiceLoader)
│   │   │   ├── client/                  Side button, popup, grid widget, tutorial card
│   │   │   ├── mixin/                   All mixins (common + "client" split in the config)
│   │   │   └── network/                 SelectRecipePacket
│   │   └── resources/                   rspolymorph-common.mixins.json, assets/rspolymorph (lang, sprites), data
│   ├── 26.1.2/src/main/                 Only the files whose 26.x API differs (forks), plus 26.x resources
│   ├── 1.20.1/, 1.20.4/, 1.19.2/        Refined Storage 1.x with mixins: 1.20.1 is the base, the others hold forks
│   ├── 1.16.5/, 1.16.1/, 1.15.2/        Java 8 line: 1.16.5 is the base, the others hold forks
│   ├── 1.15.1/, 1.14.4/                 Forge JS coremods and events instead of mixins, plus forks
│   └── 1.12.2/, 1.12/, 1.11.2/, 1.10.2/, 1.10/, 1.9.4/, 1.9/ FML coremod line: 1.12.2 is the base, the others hold forks
├── neoforge-1.21.1/                     NeoForge entrypoint and loader wiring only; builds rspolymorph-neoforge-1.21.1
├── fabric-1.21.1/                       Fabric entrypoint and loader wiring only; builds rspolymorph-fabric-1.21.1
├── neoforge-26.1.2/                     NeoForge 26.1.2 entrypoint; builds rspolymorph-neoforge-26.1.2
├── neoforge-1.20.4/, forge-<mc>/        One folder per target, same role, 18 Forge folders
├── fabric-26.1.2/                       Fabric 26.1.2 entrypoint and loader wiring only; builds rspolymorph-fabric-26.1.2
├── tools/collect_jars.sh                Copies every target's jar into jars/<version>/ (local, ignored)
├── tools/make_branches.sh               Points every target branch at main, never forces
├── tools/targets.tsv, release_notes.sh  One row per jar; the notes of a GitHub release
├── docs/                                BUILDING.md, COMPATIBILITY.md
├── images/                              CurseForge page pictures and the tools that make them
├── .github/                             Issue forms, PR template, labels.json, workflows, FUNDING, COMMUNICATION
└── README, CHANGELOG, CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, RULES, LICENSE, NOTICE, CURSEFORGE_PAGE
```

> Build: `cd <target> && ./gradlew build` produces `<target>/build/libs/rspolymorph-<target>-<version>.jar`.
> `tools/collect_jars.sh --build` builds every target and gathers the jars in `jars/<version>/`.
>
> Rule: gameplay logic and mixins go in `common/` ONLY. The target folders contain loader wiring
> (entrypoint, registration, networking impl) and nothing else. `common` must never import a
> loader API: reach loader behaviour through `Services` (ServiceLoader). Mixins on RS classes use
> `remap=false`, except injectors whose target is an inherited vanilla method. Fabric Loom still
> remaps the Minecraft names in every annotation string of the built jar.
> Refined Storage is a Modrinth maven dependency pinned by version id per loader
> (`refinedstorage_neoforge` / `refinedstorage_fabric` in each `gradle.properties`): the same version
> number exists for both loaders.

### How forks assemble

A fork replaces a file of the closest line; it never duplicates what stays the same. Each target's
`build.gradle` lists its layers (`docs/BUILDING.md` has the table): a file of a later layer that an
earlier one also holds is left out, and on the ForgeGradle 2.x targets `adaptLine` rewrites the names
that moved between versions. Adding a target means one folder, one line in `.github/workflows/build.yml`
and one row in `tools/targets.tsv`.

### MC 26.1.2 / RS 3.x line (`common/26.1.2`, `neoforge-26.1.2`, `fabric-26.1.2`)

- Shares the 1.21.1 `common` sources via a build-time remap (task `remapCommonSources` of each 26.1.2 target):
  `ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`. Files whose 26.x API
  differs **semantically** (the `extractRenderState`/`extractContents` GUI pipeline, the stream-based
  recipe provider of `MixinPatternResolver`, `KeyEvent`) are **forked** under `common/26.1.2/src` and
  excluded from the remap (see the `forkedFor261` list in `neoforge-26.1.2/build.gradle`, kept in step
  with `fabric-26.1.2/build.gradle`). Recipe API differences (lookup, `assemble`, `RecipeHolder.id()`
  as `ResourceKey`) live only in the "Version-specific recipe API" part of the forked `RsPolymorph`,
  so server logic such as `MixinRecipeMatrix` and `SelectRecipePacket` is not forked.
- MC 26.1.x needs **Java 25** and runs **un-obfuscated**. `cd neoforge-26.1.2 && ./gradlew build`:
  Gradle toolchains provide Java 25 whatever JDK starts the build.
- NeoForge 26.x API deltas already handled: `FMLEnvironment.getDist()`, `ClientPacketDistributor`
  (`net.neoforged.neoforge.client.network`), `AttachmentType.Builder.serialize(MapCodec)`.

## 5. Adding a New Feature (Step by Step)

1. Create branch `feat/my-feature` from `main`
2. If the feature touches RS2 internals, decompile the relevant Refined Storage class first (jar in the Gradle cache) (`javap -p -c`) to verify field/method signatures before writing the Mixin
3. Implement common logic first (package `com.vyrriox.rspolymorph`)
4. If UI is needed, add under `client/` and reach it from a client mixin
5. If a new mixin is introduced, add to `rspolymorph-common.mixins.json` of BOTH lines under `"mixins"` (common) or `"client"` (client-only)
6. If a new packet is needed, register it in the loader entrypoints (`RsPolymorphNeoForge`, `RsPolymorphFabric`)
7. Add translations to `common/1.21.1/src/main/resources/assets/rspolymorph/lang/{en_us,fr_fr}.json`
8. Run `tools/collect_jars.sh --build` (builds every target)
9. Test in singleplayer AND dedicated server
10. Commit and PR into `main`

## 6. Testing Checklist

- [ ] `tools/collect_jars.sh --build` builds every target with no warnings related to missing types
- [ ] Pattern Grid: recipe selection updates the preview immediately (no need to print an intermediate pattern)
- [ ] Pattern Grid: printed pattern is tagged with the selected recipe ID
- [ ] Autocraft resolves the tagged recipe via `MixinPatternResolver`
- [ ] Crafting Grid: recipe selection produces the chosen output on craft
- [ ] Dedicated server starts cleanly — no `ClassNotFoundException` / `NoClassDefFoundError` mentioning `net.minecraft.client.*`
- [ ] Multiplayer: two players can open separate grids without selection bleed
- [ ] No client-only class referenced from `RsPolymorph`, `SelectRecipePacket`, or common mixins
- [ ] `rspolymorph-common.mixins.json` correctly separates common vs client mixins

## 7. Environment Setup

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
cd neoforge-1.21.1        # or any target folder
./gradlew build
./gradlew runClient
./gradlew runServer
```

## 8. AI Assistant Instructions

1. Never add client imports to common-side code (`RsPolymorph`, `SelectRecipePacket`, `mixin/MixinPatternResolver`, `mixin/MixinPatternGrid`, `mixin/MixinCraftingGrid`, `mixin/MixinRecipeMatrix`, `mixin/MixinRecipeMatrixContainer`, accessors)
2. When overriding RS's recipe result, ALWAYS update `currentRecipe` via `AccessorRecipeMatrix.rspolymorph$setCurrentRecipe` alongside `invokeSetResult`, otherwise the preview reverts on the next update
3. For recipe selection, always dispatch `SelectRecipePacket` (works in both SP and MP via loopback)
4. Decompile the Refined Storage jar (from the Gradle cache, Modrinth maven) before speculating about their internal API — field and method signatures change between RS2 versions
5. Keep `createCraftingPattern` tagging on the crafting matrix's current recipe, and never add a global map or static selection: grid state goes in `@Unique` fields on RS's objects
6. Use `instanceof` for slot type detection — never `getClass().getName().contains(...)` (fails on anonymous inner classes)
7. Always add EN + FR translations for any new user-facing string
8. Run `tools/collect_jars.sh --build` before committing
9. Bump the mod version ONLY when the user explicitly asks for it; default is VERSION LOCK
10. Generate `TEST_PROCEDURE_vX.Y.Z.html` on every version bump
