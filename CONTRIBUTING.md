# Contributing to RS Polymorph / Contribuer

Thank you for your interest in contributing! | Merci de votre interet !

## Prerequisites / Prerequis
- Java 21 (Temurin recommended)
- Gradle 8.x
- NeoForge MDK knowledge
- [Polymorph](https://www.curseforge.com/minecraft/mc-mods/polymorph) >= 1.1.0
- [Refined Storage 2](https://www.curseforge.com/minecraft/mc-mods/refined-storage-2) >= 2.0.1

## Setup / Installation

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
./gradlew build
```

`libs/polymorph.jar` and `libs/rs2.jar` are tracked in the repository — no manual download is required.

## Code Conventions

- **Code, variables, logs**: English only
- **Naming**: PascalCase for classes, camelCase for methods/fields
- **Indentation**: 4 spaces
- Never import `net.minecraft.client.*` from common-side code (main `@Mod`, common mixins, packet handlers, `RsGridRecipeData`). Client code lives under `com.vyrriox.rspolymorph.client`.
- Client-only mixins go in the `"client"` block of `mixins.rspolymorph.json`; common mixins go in the `"mixins"` block.
- Use `instanceof` for slot type detection, not class-name string matching.
- When overriding RS2's `RecipeMatrix` result, always sync `currentRecipe` via the accessor so subsequent `updateResult` calls don't revert the preview.

## Commit Messages

```
feat: add new feature
fix: resolve bug
refactor: restructure code
docs: update documentation
perf: improve performance
release: version bump
```

## Branch Strategy

| Branch | Purpose | Merges into |
|--------|---------|-------------|
| main | Stable releases, default PR target | - |
| neoforge-1.21.1 | 1.21.1 NeoForge maintenance | main |
| fabric-1.21.1 | 1.21.1 Fabric maintenance | main |
| neoforge-26.1.2 | 26.1.2 NeoForge maintenance | main |
| feat/* | New features | main |
| fix/* | Bug fixes | main, or one version branch |
| hotfix/* | Critical patches | main, then version branches |

Labels are defined in `.github/labels.json` (type, priority, status, area, loader, mc).

## Community / Communaute

- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Website](https://arcadia-echoes-of-power.fr/)
