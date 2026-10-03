# RS Polymorph

[Consult the full CurseForge description](./CURSEFORGE_PAGE.md)

RS Polymorph is a standalone Minecraft add-on for **NeoForge and Fabric** that adds recipe selection to the [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) crafting and pattern grids — **no Polymorph mod required**. When several recipes match the same ingredients, a side button opens a popup that lets you pick which one the Crafting Grid crafts and which one the Pattern Grid prints for autocrafting.

## Features

- **Crafting Grid** — Adds a recipe selection button to the Refined Storage Crafting Grid, opening a polished popup (titled header, slot frames, a gold highlight on the active recipe)
- **Wireless Crafting Grid** — Recipe selection also works in the [Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal) Wireless Crafting Grid; the choice is driven through the grid's recipe matrix even though it has no block entity (optional compat, no hard dependency)
- **Pattern Grid** — Adds a recipe selection button to the Pattern Grid, with the selection persisted on the printed pattern via a custom data component
- **Non-unique craft warning** — When more than one recipe matches the current grid, the recipe button shows Refined Storage's native red warning icon (with an explanatory tooltip line) and pulses a gold halo so you never miss the choice and never craft the wrong item by accident
- **First-open tutorial** — A one-time card explains the feature the first time you open an RS grid; dismiss it with a click or Space/Enter (with narrator support)
- **Autocrafting integration** — `MixinPatternResolver` prefers the pattern's stored recipe ID over the default first-match, so autocrafting always resolves the recipe you chose
- **Server-safe** — All client classes are isolated behind `FMLEnvironment.dist.isClient()`; client mixins are split into the `"client"` block of the mixin config, so dedicated servers start cleanly with no missing-class errors
- **Unified SP / MP path** — Recipe selection goes through a single `SelectRecipePacket`, which works via loopback in singleplayer and over the network in multiplayer
- **Bilingual UI** — English and French lang files

## Commands

This mod does not add any commands — it integrates transparently with the Refined Storage 2 UI.

## Requirements

| Target | Minecraft | Loader | Refined Storage | Java |
|--------|-----------|--------|-----------------|------|
| 1.21.1 NeoForge | 1.21.1 | NeoForge 21.1.219+ | RS 2.x (tested 2.0.8) | 21 |
| 1.21.1 Fabric | 1.21.1 | Fabric Loader 0.16.10+ (+ Fabric API) | RS 2.x (tested 2.0.8) | 21 |
| 26.1.2 NeoForge | 26.1.2 | NeoForge 26.1.2.x | RS 3.x (tested 3.2.0) | 25 |

- **No Polymorph dependency** — recipe selection is built in.
- **Optional:** Refined Storage - Quartz Arsenal >= 1.0.7 for the Wireless Crafting Grid.

Pick the jar matching your Minecraft version and loader (e.g. `rspolymorph-neoforge-1.21.1-*.jar`, `rspolymorph-fabric-1.21.1-*.jar`, `rspolymorph-neoforge-26.1.2-*.jar`). On 1.21.1 a single combined NeoForge+Fabric jar is also available. *Fabric for 26.1.2 is implemented but pending a stable Fabric Loom with Minecraft 26.x support (see CHANGELOG).*

## Installation

1. Install [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) in your `mods/` folder
2. Place the RS Polymorph jar matching your Minecraft version and loader in your `mods/` folder
3. (Optional) Install [Refined Storage - Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal) for Wireless Crafting Grid support
4. Start the game (singleplayer) or server — **no Polymorph mod needed**

## Usage

1. Open a Crafting Grid or Pattern Grid with a Refined Storage network
2. Place ingredients that match multiple recipes (e.g. 4 planks → sticks vs. a dye variant)
3. Click the **recipe selection side button** on the left side of the grid
4. Select your preferred recipe from the popup
5. The grid preview (Crafting Grid) or the printed pattern (Pattern Grid) will use your selection

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — Version history and per-version test procedures
- [RULES.md](RULES.md) — Project conventions, architecture, and AI assistant guidelines
- [CONTRIBUTING.md](.github/CONTRIBUTING.md) — Contribution guide
- [SECURITY.md](.github/SECURITY.md) — Security policy

## Credits

Author: vyrriox
Organization: Team Arcadia
License: Apache-2.0, see [LICENSE](LICENSE). Forks and derivative works are welcome, provided they keep the [NOTICE](NOTICE) file, credit "vyrriox / Team Arcadia" and link back to the upstream repository.
Discord: [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Website: [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)

---

# RS Polymorph (Version Française)

[Consulter la description CurseForge complète](./CURSEFORGE_PAGE.md)

RS Polymorph est un add-on autonome pour Minecraft (**NeoForge et Fabric**) qui ajoute la sélection de recette aux grilles de craft et de patron de [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) — **sans nécessiter le mod Polymorph**. Lorsque plusieurs recettes correspondent aux mêmes ingrédients, un bouton latéral ouvre un popup qui vous laisse choisir laquelle la Crafting Grid fabrique et laquelle la Pattern Grid imprime pour l'autocraft.

## Caractéristiques

- **Crafting Grid** — Ajoute un bouton de sélection de recette à la Crafting Grid de Refined Storage, ouvrant un popup soigné (en-tête titré, slots en relief, surbrillance dorée sur la recette active)
- **Wireless Crafting Grid** — La sélection de recette fonctionne aussi dans la Wireless Crafting Grid de [Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal) ; le choix est appliqué via la matrice de recette de la grille bien qu'elle n'ait aucun block entity (compat optionnelle, sans dépendance forte)
- **Pattern Grid** — Ajoute un bouton de sélection de recette à la Pattern Grid, avec la sélection persistée sur le patron imprimé via un data component custom
- **Tutoriel au premier lancement** — Une carte unique explique la fonctionnalité à la première ouverture d'une grille RS ; fermez-la d'un clic ou avec Espace/Entrée (support narrateur)
- **Intégration autocraft** — `MixinPatternResolver` privilégie l'ID de recette stocké sur le patron plutôt que le premier match par défaut, donc l'autocraft résout toujours la recette choisie
- **Sûr côté serveur** — Toutes les classes client sont isolées derrière `FMLEnvironment.dist.isClient()` ; les mixins client sont dans le bloc `"client"` du fichier mixins, donc les serveurs dédiés démarrent proprement sans erreur de classe manquante
- **Chemin SP / MP unifié** — La sélection de recette passe par un unique `SelectRecipePacket`, fonctionnant en boucle locale en solo et sur le réseau en multijoueur
- **Interface bilingue** — Fichiers de langue anglais et français

## Commandes

Ce mod n'ajoute aucune commande — il s'intègre de manière transparente à l'interface Refined Storage 2.

## Prérequis

| Cible | Minecraft | Loader | Refined Storage | Java |
|-------|-----------|--------|-----------------|------|
| 1.21.1 NeoForge | 1.21.1 | NeoForge 21.1.219+ | RS 2.x (testé 2.0.8) | 21 |
| 1.21.1 Fabric | 1.21.1 | Fabric Loader 0.16.10+ (+ Fabric API) | RS 2.x (testé 2.0.8) | 21 |
| 26.1.2 NeoForge | 26.1.2 | NeoForge 26.1.2.x | RS 3.x (testé 3.2.0) | 25 |

- **Aucune dépendance à Polymorph** — la sélection de recette est intégrée.
- **Optionnel :** Refined Storage - Quartz Arsenal >= 1.0.7 pour la Wireless Crafting Grid.

Choisissez le jar correspondant à votre version de Minecraft et loader (ex. `rspolymorph-neoforge-1.21.1-*.jar`, `rspolymorph-fabric-1.21.1-*.jar`, `rspolymorph-neoforge-26.1.2-*.jar`). Sur 1.21.1, un jar combiné NeoForge+Fabric unique est aussi disponible. *Fabric pour 26.1.2 est implémenté mais en attente d'un Fabric Loom stable supportant Minecraft 26.x (voir CHANGELOG).*

## Installation

1. Installez [Refined Storage](https://www.curseforge.com/minecraft/mc-mods/refined-storage) dans votre dossier `mods/`
2. Placez le jar RS Polymorph correspondant à votre version de Minecraft et loader dans votre dossier `mods/`
3. (Optionnel) Installez [Refined Storage - Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal) pour le support de la Wireless Crafting Grid
4. Démarrez le jeu (solo) ou le serveur — **aucun mod Polymorph requis**

## Utilisation

1. Ouvrez une Crafting Grid ou Pattern Grid avec un réseau Refined Storage
2. Placez des ingrédients qui correspondent à plusieurs recettes (ex. 4 planches → bâtons vs. une variante de teinture)
3. Cliquez sur le **bouton latéral de sélection de recette** sur la gauche de la grille
4. Sélectionnez votre recette préférée dans le popup
5. L'aperçu de la grille (Crafting Grid) ou le patron imprimé (Pattern Grid) utilisera votre sélection

## Documentation

- [CHANGELOG.md](CHANGELOG.md) — Historique des versions et procédures de test
- [RULES.md](RULES.md) — Conventions du projet, architecture et règles pour les assistants IA
- [CONTRIBUTING.md](.github/CONTRIBUTING.md) — Guide de contribution
- [SECURITY.md](.github/SECURITY.md) — Politique de sécurité

## Credits

Auteur : vyrriox
Organisation : Team Arcadia
Licence : Apache-2.0, voir [LICENSE](LICENSE). Les forks et travaux dérivés sont les bienvenus, à condition de conserver le fichier [NOTICE](NOTICE), de créditer « vyrriox / Team Arcadia » et de pointer vers le dépôt d'origine.
Discord : [discord.gg/xjF8Rtzyd4](https://discord.gg/xjF8Rtzyd4)
Site web : [arcadia-echoes-of-power.fr](https://arcadia-echoes-of-power.fr/)
