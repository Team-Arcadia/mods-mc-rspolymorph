# Contributing to RS Polymorph / Contribuer

Thank you for your interest in contributing! | Merci de votre interet !

## Prerequisites / Prerequis
- The Java of the target you build, from 8 to 25 (Temurin recommended): see [docs/BUILDING.md](docs/BUILDING.md)
- No Gradle install: each target has its wrapper

## Setup / Installation

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
tools/collect_jars.sh --build   # or: cd neoforge-1.21.1 && ./gradlew build
```

Each target folder is a standalone Gradle project; Refined Storage is fetched from the Modrinth maven, so no manual download is required. See [docs/BUILDING.md](docs/BUILDING.md).

## Code Conventions

- **Code, variables, logs**: English only
- **Naming**: PascalCase for classes, camelCase for methods/fields
- **Indentation**: 4 spaces
- Never import `net.minecraft.client.*` from common-side code (main `@Mod`, common mixins, packet handlers). Client code lives under `com.vyrriox.rspolymorph.client`.
- Client-only mixins go in the `"client"` block of `rspolymorph-common.mixins.json`; common mixins go in the `"mixins"` block.
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
| `<loader>-<minecraft version>` | Maintenance of one target, named like its folder: see [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md) | main |
| feat/* | New features | main |
| fix/* | Bug fixes | main, or one version branch |
| hotfix/* | Critical patches | main, then version branches |

Labels are defined in `.github/labels.json` (type, priority, status, area, loader, mc).

## Community / Communaute

- [Discord](https://discord.gg/xjF8Rtzyd4)
- [Website](https://arcadia-echoes-of-power.fr/)
