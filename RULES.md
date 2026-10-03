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
1. Bump `mod_version` in the `gradle.properties` of every target folder
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

Every target is a standalone Gradle project (own `build.gradle`, `settings.gradle`,
`gradle.properties` and wrapper); nothing builds from the root. Folder names carry the loader and
the Minecraft version, like the version branches. Loader-independent code lives once per Minecraft
version under `common/`, and each target adds it as a source folder (`../common/<mc>/src/main`).

```
mods-mc-rspolymorph/
├── common/
│   ├── 1.21.1/src/main/                 Loader-agnostic code for MC 1.21.1 / RS 2.x: ALL gameplay logic and every mixin
│   │   ├── java/com/vyrriox/rspolymorph/
│   │   │   ├── RsPolymorph.java         Core (registry maps, selection state), no @Mod
│   │   │   ├── IRsRecipeMatrix.java     Duck-type interface for the RecipeMatrix accessor
│   │   │   ├── platform/                Services, NetworkPlatform, GridRecipeStore (ServiceLoader)
│   │   │   ├── client/                  Side button, popup, grid widget, tutorial card
│   │   │   ├── mixin/                   All mixins (common + "client" split in the config)
│   │   │   └── network/                 SelectRecipePacket
│   │   └── resources/                   rspolymorph-common.mixins.json, assets/rspolymorph (lang, sprites), data
│   └── 26.1.2/src/main/                 Only the files whose 26.x API differs (forks), plus 26.x resources
├── neoforge-1.21.1/                     NeoForge entrypoint and loader wiring only; builds rspolymorph-neoforge-1.21.1
├── fabric-1.21.1/                       Fabric entrypoint and loader wiring only; builds rspolymorph-fabric-1.21.1
├── neoforge-26.1.2/                     NeoForge 26.1.2 entrypoint; builds rspolymorph-neoforge-26.1.2
├── fabric-26.1.2/                       Fabric 26.1.2 entrypoint, implemented but not built yet (issue #5)
├── tools/collect_jars.sh                Copies every target's jar into jars/<version>/ (local, ignored)
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
> loader API: reach loader behaviour through `Services` (ServiceLoader). All mixins stay
> `remap=false` (targets are RS classes, never remapped on either loader).
> Refined Storage is a Modrinth maven dependency pinned by version id per loader
> (`refinedstorage_neoforge` / `refinedstorage_fabric` in each `gradle.properties`): the same version
> number exists for both loaders.

### MC 26.1.2 / RS 3.x line (`common/26.1.2`, `neoforge-26.1.2`, `fabric-26.1.2`)

- Shares the 1.21.1 `common` sources via a build-time remap (task `remapCommonSources` of each 26.1.2 target):
  `ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`. Files whose 26.x API
  differs **semantically** (recipe lookup, `assemble`, `RecipeHolder.id()` as `ResourceKey`, the
  `extractRenderState`/`extractContents` GUI pipeline) are **forked** under `common/26.1.2/src` and
  excluded from the remap (see the `forkedFor261` list in `neoforge-26.1.2/build.gradle`, kept in step
  with `fabric-26.1.2/build.gradle`).
- MC 26.1.x needs **Java 25** and runs **un-obfuscated**. `cd neoforge-26.1.2 && ./gradlew build`:
  Gradle toolchains provide Java 25 whatever JDK starts the build.
- `fabric-26.1.2` is implemented but excluded from the default build: un-obfuscated 26.x needs
  `fabric-loom` 1.17.0-alpha+, which conflicts with the stable Loom 1.16.3 used by the 1.21.1
  Fabric module (issue #5).
- NeoForge 26.x API deltas already handled: `FMLEnvironment.getDist()`, `ClientPacketDistributor`
  (`net.neoforged.neoforge.client.network`), `AttachmentType.Builder.serialize(MapCodec)`.

## 5. Adding a New Feature (Step by Step)

1. Create branch `feat/my-feature` from `main`
2. If the feature touches RS2 internals, decompile the relevant Refined Storage class first (jar in the Gradle cache) (`javap -p -c`) to verify field/method signatures before writing the Mixin
3. Implement common logic first (package `com.vyrriox.rspolymorph`)
4. If UI is needed, add under `client/` and register via `ClientSetup.init()`
5. If a new mixin is introduced, add to `mixins.rspolymorph.json` under `"mixins"` (common) or `"client"` (client-only)
6. If a new packet is needed, register under `RsPolymorph.registerPayloads`
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
- [ ] No client-only class referenced from `RsPolymorph`, `RsGridRecipeData`, `SelectRecipePacket`, or common mixins
- [ ] `mixins.rspolymorph.json` correctly separates common vs client mixins

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

1. Never add client imports to common-side code (`RsPolymorph`, `SelectRecipePacket`, `mixin/MixinPatternResolver`, `mixin/MixinPatternGrid`, `mixin/MixinCraftingGrid`, `mixin/MixinRecipeMatrix`, accessors)
2. When overriding RS2's recipe result, ALWAYS update `currentRecipe` via `AccessorRecipeMatrix.rspolymorph$setCurrentRecipe` alongside `invokeSetResult` — otherwise the preview reverts on the next tick
3. For recipe selection, always dispatch `SelectRecipePacket` (works in both SP and MP via loopback)
4. Decompile the Refined Storage jar (from the Gradle cache, Modrinth maven) before speculating about their internal API — field and method signatures change between RS2 versions
5. Keep `createCraftingPattern` tagging dual-source: read the static `selectedRecipeId` first, fall back to the per-grid store (`GridRecipeStore`)
6. Use `instanceof` for slot type detection — never `getClass().getName().contains(...)` (fails on anonymous inner classes)
7. Always add EN + FR translations for any new user-facing string
8. Run `tools/collect_jars.sh --build` before committing
9. Bump the mod version ONLY when the user explicitly asks for it; default is VERSION LOCK
10. Generate `TEST_PROCEDURE_vX.Y.Z.html` on every version bump
