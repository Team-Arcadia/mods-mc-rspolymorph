# Changelog

All notable changes to RS Polymorph are documented here.

---

## [1.2.2] - 2026-06-29

### Added

- **Non-unique craft warning on the recipe button (issue #3)** — When the open grid currently matches more than one recipe, the recipe-selection side button now flags it so the choice is never missed and the wrong item is not crafted by accident. It uses Refined Storage's native warning idiom — a red warning icon drawn on the button plus a red explanatory line appended to its tooltip — and additionally pulses a soft gold halo around the button to draw the eye to it (players reported not noticing the button at all). The pulse stops once the popup is open. The indicator clears automatically when the craft becomes unique. Implemented on all three jars (1.21.1 Fabric/NeoForge, 26.1.2 NeoForge).

### Removed

- **Debug "Test Stick" items** — The two development-only items (`test_stick_1` / `test_stick_2`), which existed only to demonstrate the multi-recipe case, were removed from the released mod: their registration (both loaders, both MC lines), creative-tab entries, recipes, models, textures, and lang strings are gone. Real packs already provide genuine non-unique crafts, so the debug items served no purpose in a release build.

### Changed

- **License changed to Apache-2.0** — RS Polymorph is now under the Apache License 2.0 (official text, recognised by GitHub). A new `NOTICE` file carries the attribution that forks and derivative works must keep: credit to "vyrriox / Team Arcadia" and a link to the upstream repository.
- **Repository reorganised** — The repository is now `Team-Arcadia/mods-mc-rspolymorph`. Branches are `main` plus maintenance branches per line (`neoforge-1.21.1`, `fabric-1.21.1`, `neoforge-26.1.2`); `develop`, `staging` and `hotfix` were retired. A label system, updated issue forms and a CI trigger that watches the multiloader source folders were added.

### Ajouts

- **Avertissement de craft non unique sur le bouton de recette (issue #3)** — Quand la grille ouverte correspond à plusieurs recettes, le bouton latéral de sélection le signale désormais pour que le choix ne soit jamais manqué et qu'un mauvais objet ne soit pas crafté par accident. Il reprend l'idiome d'avertissement natif de Refined Storage — une icône d'avertissement rouge dessinée sur le bouton plus une ligne rouge explicative ajoutée à son infobulle — et fait en plus pulser un léger halo doré autour du bouton pour attirer l'œil (des joueurs signalaient ne pas remarquer le bouton du tout). La pulsation s'arrête dès que le popup est ouvert. L'indicateur disparaît automatiquement quand le craft redevient unique. Implémenté sur les trois jars (1.21.1 Fabric/NeoForge, 26.1.2 NeoForge).

### Modifications

- **Licence passée en Apache-2.0** — RS Polymorph est désormais sous licence Apache 2.0 (texte officiel, reconnu par GitHub). Un nouveau fichier `NOTICE` porte la mention que les forks et travaux dérivés doivent conserver : le crédit « vyrriox / Team Arcadia » et un lien vers le dépôt d'origine.
- **Dépôt réorganisé** — Le dépôt est désormais `Team-Arcadia/mods-mc-rspolymorph`. Les branches sont `main` plus une branche de maintenance par ligne (`neoforge-1.21.1`, `fabric-1.21.1`, `neoforge-26.1.2`) ; `develop`, `staging` et `hotfix` ont été retirées. Un système de labels, des formulaires d'issues à jour et un déclencheur CI qui surveille les dossiers sources multiloader ont été ajoutés.

### Suppressions

- **Objets de débogage « Bâton de test »** — Les deux objets réservés au développement (`test_stick_1` / `test_stick_2`), qui n'existaient que pour illustrer le cas multi-recettes, ont été retirés du mod publié : leur enregistrement (deux loaders, deux lignes MC), leurs entrées dans l'onglet créatif, leurs recettes, modèles, textures et chaînes de traduction ont disparu. Les packs réels fournissent déjà de vrais crafts non uniques, ces objets de débogage n'avaient donc aucune utilité dans un build de publication.

---

## [1.2.1] - 2026-06-14

### Fixed

- **Recipe-selection popup drawn under the grid items (z-order)** — The recipe-selection popup rendered *behind* the crafting-grid item icons, so the grid items showed through it and the recipe icons could not be read or clicked. Cause: Minecraft renders all GUI item icons on a fixed high depth layer (`renderItem` translates to Z=232 on 1.21.1; the carried-item / tooltip phases composite on later strata on 26.x), so even though the popup was drawn at the screen's render tail its flat background sat below the items. On **1.21.1** the whole popup (and the first-open tutorial card) is now drawn inside a pose pushed to Z=400, above the item layer. On **26.1.2** the popup render was moved off RS's `extractContents` (the first of four extract phases) onto vanilla `AbstractContainerScreen.extractRenderState` at RETURN — the only point that runs after `extractContents` → `extractCarriedItem` → `extractSnapbackItem` → `extractTooltip` — via a new `MixinScreenPopupRender`, so the popup composites on the topmost stratum. The recipe popup is now fully on top and selectable on all three jars.

### Ajouts / Correctifs / Modifications / Performance (French mirror)

### Correctifs

- **Popup de sélection de recette affichée sous les items de la grille (ordre de profondeur)** — La popup de sélection s'affichait *derrière* les icônes d'items de la grille de craft : les items transparaissaient par-dessus et les icônes de recette étaient illisibles et incliquables. Cause : Minecraft rend toutes les icônes d'items de GUI sur une couche de profondeur élevée fixe (`renderItem` translate en Z=232 sur 1.21.1 ; les phases item-porté / infobulle se composent sur des strates ultérieures en 26.x), donc même dessinée en fin de rendu, le fond plat de la popup restait sous les items. Sur **1.21.1**, toute la popup (et la carte de tutoriel de première ouverture) est désormais dessinée dans une pose poussée à Z=400, au-dessus de la couche des items. Sur **26.1.2**, le rendu de la popup a été déplacé de `extractContents` de RS (la première des quatre phases d'extraction) vers `AbstractContainerScreen.extractRenderState` vanilla en RETURN — le seul point exécuté après `extractContents` → `extractCarriedItem` → `extractSnapbackItem` → `extractTooltip` — via un nouveau `MixinScreenPopupRender`, pour que la popup se compose sur la strate la plus haute. La popup de recette est maintenant entièrement au premier plan et sélectionnable sur les trois jars.

---

## [1.2.0] - 2026-06-05

### Added

- **Standalone — no Polymorph dependency** — RS Polymorph no longer requires the Polymorph mod. The recipe-selection UI (side button + popup) and the per-grid selection persistence are now provided entirely by RS Polymorph itself. The side button is the native Refined Storage `AbstractSideButtonWidget`; the popup is a self-contained `RecipeSelectorPopup` (own item-grid widget + render/click mixin on the grid screen); the per-grid choice is persisted on the grid block entity via loader Data Attachments (NeoForge `AttachmentType` / Fabric persistent attachment) behind a `Services.GRID_STORE` abstraction. The printed-pattern tag stays a vanilla `DataComponentType`. The Polymorph-only classes (`RsGridRecipeData`, `MixinSelectionWidget`, `MixinRecipeManagerSafety`) were removed. Result: a true Refined Storage add-on that works everywhere RS does, with no third-party recipe-selection dependency.
- **Minecraft 26.1.2 / Refined Storage 3.2.0 (NeoForge)** — A parallel build targets the new Minecraft 26.1.2 (year-based versioning, Java 25) against Refined Storage 3.2.0, sharing the 1.21.1 codebase. RS 3.x kept the same `com.refinedmods.refinedstorage.common.*` structure, so the mixins carry over; the few MC 26.x renames (`ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`) are applied by a build-time source remap, and the handful of files touching the reworked recipe API (`server.getRecipeManager()`/RS3 `RecipeProvider`, single-arg `assemble`, `RecipeHolder.id()` now a `ResourceKey`) and the new two-phase GUI render pipeline (`extractRenderState`/`extractContents`) are forked under `common-261`. `MixinPatternResolver` is re-targeted with `@WrapOperation` onto RS3's `Platform.getClientRecipeProvider().getRecipesFor(...)`. Build with Gradle on JDK 25: `./gradlew :neoforge-261:build -Dorg.gradle.java.home=<jdk25>`.
- **Fabric support — one mod, two loaders** — RS Polymorph now ships for **Fabric** in addition to NeoForge, both for Minecraft 1.21.1. The project was restructured into a MultiLoader layout (`common` + `neoforge` + `fabric`): all gameplay logic and every mixin live in `common` and are shared verbatim by both loaders. Because the mixins target Refined Storage 2 and Polymorph classes (never Minecraft) with `remap=false`, and both RS2 and Polymorph ship their public surface in a shared cross-loader module, the injection logic resolves byte-for-byte identically on NeoForge and Fabric with no per-loader rewrite.
- **Loader-agnostic core** — Introduced a small platform abstraction so `common` never references a loader API: `Services`/`NetworkPlatform` resolve the active loader's networking via `ServiceLoader`, the `selected_recipe` data component is registered by each loader and injected into the shared core, and `SelectRecipePacket` exposes a pure `applyOnServer(ServerPlayer, ResourceLocation)` that each loader drives from its own packet receiver (NeoForge `IPayloadContext`, Fabric `ServerPlayNetworking`).
- **Combined single jar (optional)** — In addition to the two per-loader jars, an optional fused jar (`rspolymorph-<version>.jar`) that loads on both NeoForge and Fabric is produced via ModFusioner (`./gradlew fusejars`). The per-loader jars remain the primary, most robust distribution.
- **First-open tutorial card** — The first time a player opens an RS grid, a one-time, bilingual card explains the recipe-selection feature (button + popup) and is dismissed by a click or by pressing Space/Enter. The "seen" flag is persisted client-side (`config/rspolymorph_tutorial.flag`, written off the render thread, fail-soft) so it never reappears. Screen-reader users get a one-shot narrator announcement.
- **Redesigned recipe selector popup** — The selection popup was reworked to look native: a titled header, inset slot frames, a gold highlight on the recipe the grid is currently producing, on-screen clamping, item-count decorations, and a single readable recipe name on hover. Per-frame allocations in the render path were removed.

### Fixed

- **Recipe selection on NeoForge** — Selecting a recipe threw `UnsupportedOperationException` server-side: the per-grid map decoded from the attachment codec is a Guava `ImmutableMap`, so `put`/`remove` failed. `NeoForgeGridRecipeStore.set` now copies-on-write into a fresh `HashMap` before `setData`. Affected both 1.21.1 and 26.1.2 NeoForge.
- **Overlapping slot tooltip** — Refined Storage's grid screens override `renderTooltip` per subclass (`CraftingGridScreen`/`PatternGridScreen`/`AbstractGridScreen`) and draw the hovered-slot tooltip themselves, so a mixin on vanilla `AbstractContainerScreen.renderTooltip` was bypassed and the underlying "Stick" tooltip drew over the popup. The suppression mixin now targets those RS classes and fires whenever the popup is open. The 26.x render pipeline differs, so its fork is a fail-soft no-op.
- **Per-MC-version recipe parsing** — Crafting-recipe ingredient JSON differs between MC versions (1.21.1 uses object form `{"item":"..."}`, 26.x uses bare strings). The test recipes are split per version and merged by a build task, so the test items now craft on every target.

### Changed

- **Refined Storage 2 baseline bumped to 2.0.8** — The Fabric build pulls Refined Storage 2.0.8 and Polymorph 1.1.0+1.21.1 from the Modrinth maven; the declared `refinedstorage` dependency floor stays `>= 2.0.1`. RS 2.x and RS 3.x share the same internal package structure (`com.refinedmods.refinedstorage.common.*`), so the mixins are forward-compatible across the RS 2.x line with no signature changes.
- **Build toolchain** — Migrated to the MultiLoader-Template build (Gradle 9.5.1, Fabric Loom 1.16.3 for the Fabric module, ModDevGradle 2.0.140 for the common/NeoForge modules). The Gradle wrapper was bumped from 9.2.1 to 9.5.1 to satisfy Loom 1.16.3.

### Known limitations

- **Fabric on Minecraft 26.1.2 not yet enabled** — Minecraft 26.1.x ships un-obfuscated (no Mojang ProGuard mappings), which only `fabric-loom` 1.17.0-alpha+ supports, and that alpha conflicts with the stable Loom 1.16.3 used by the 1.21.1 Fabric module in the same build. The `fabric-261` module (sources, entrypoints, metadata) is implemented but excluded from the default build; enable it on a branch that bumps Loom once a stable Loom with 26.x support lands. NeoForge 26.1.2 ships normally.

### Ajouts

- **Autonome — plus de dépendance à Polymorph** — RS Polymorph ne requiert plus le mod Polymorph. L'UI de sélection de recette (bouton latéral + popup) et la persistance du choix par grille sont désormais fournies entièrement par RS Polymorph. Le bouton latéral est l'`AbstractSideButtonWidget` natif de Refined Storage ; le popup est un `RecipeSelectorPopup` autonome (widget de grille d'items + mixin de rendu/clic sur l'écran de grille) ; le choix par grille est persisté sur le block entity via les Data Attachments du loader (NeoForge `AttachmentType` / attachment persistant Fabric) derrière une abstraction `Services.GRID_STORE`. Le tag du patron imprimé reste un `DataComponentType` vanilla. Les classes spécifiques à Polymorph (`RsGridRecipeData`, `MixinSelectionWidget`, `MixinRecipeManagerSafety`) ont été supprimées. Résultat : un véritable add-on Refined Storage qui fonctionne partout où RS fonctionne, sans dépendance tierce de sélection de recette.
- **Minecraft 26.1.2 / Refined Storage 3.2.0 (NeoForge)** — Un build parallèle cible le nouveau Minecraft 26.1.2 (versioning par année, Java 25) avec Refined Storage 3.2.0, en partageant la base de code 1.21.1. RS 3.x a conservé la même structure `com.refinedmods.refinedstorage.common.*`, donc les mixins sont repris ; les quelques renommages MC 26.x (`ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`) sont appliqués par un remap de sources au build, et les fichiers touchant l'API de recette remaniée (`server.getRecipeManager()`/`RecipeProvider` RS3, `assemble` à un argument, `RecipeHolder.id()` désormais un `ResourceKey`) et le nouveau pipeline de rendu GUI à deux phases (`extractRenderState`/`extractContents`) sont forkés sous `common-261`. `MixinPatternResolver` est re-ciblé avec `@WrapOperation` sur `Platform.getClientRecipeProvider().getRecipesFor(...)` de RS3. Build avec Gradle sur JDK 25 : `./gradlew :neoforge-261:build -Dorg.gradle.java.home=<jdk25>`.
- **Support Fabric — un seul mod, deux loaders** — RS Polymorph est désormais publié pour **Fabric** en plus de NeoForge, tous deux pour Minecraft 1.21.1. Le projet a été restructuré en architecture MultiLoader (`common` + `neoforge` + `fabric`) : toute la logique de jeu et chaque mixin vivent dans `common` et sont partagés à l'identique par les deux loaders. Comme les mixins ciblent des classes de Refined Storage 2 et de Polymorph (jamais de Minecraft) avec `remap=false`, et que RS2 comme Polymorph exposent leur surface publique dans un module commun cross-loader, la logique d'injection se résout octet pour octet de façon identique sur NeoForge et Fabric, sans réécriture par loader.
- **Noyau agnostique du loader** — Ajout d'une petite abstraction de plateforme pour que `common` ne référence jamais une API de loader : `Services`/`NetworkPlatform` résolvent le réseau du loader actif via `ServiceLoader`, le data component `selected_recipe` est enregistré par chaque loader puis injecté dans le noyau partagé, et `SelectRecipePacket` expose un `applyOnServer(ServerPlayer, ResourceLocation)` pur que chaque loader pilote depuis son propre récepteur de paquet (NeoForge `IPayloadContext`, Fabric `ServerPlayNetworking`).
- **Jar unique combiné (optionnel)** — En plus des deux jars par loader, un jar fusionné optionnel (`rspolymorph-<version>.jar`) chargeable sur NeoForge ET Fabric est produit via ModFusioner (`./gradlew fusejars`). Les jars par loader restent la distribution principale, la plus robuste.
- **Carte tuto au premier lancement** — La première fois qu'un joueur ouvre une grille RS, une carte unique et bilingue explique la sélection de recette (bouton + popup) ; elle se ferme d'un clic ou en appuyant sur Espace/Entrée. Le drapeau « vu » est persisté côté client (`config/rspolymorph_tutorial.flag`, écrit hors du thread de rendu, fail-soft) pour ne jamais réapparaître. Les utilisateurs de lecteur d'écran reçoivent une annonce narrateur unique.
- **Popup de sélection redesigné** — Le popup a été retravaillé pour un rendu natif : en-tête titré, slots en relief, surbrillance dorée sur la recette actuellement produite, calage à l'écran, décorations de quantité, et un seul nom de recette lisible au survol. Les allocations par frame du rendu ont été supprimées.

### Correctifs

- **Sélection de recette sur NeoForge** — La sélection levait `UnsupportedOperationException` côté serveur : la map par grille décodée du codec d'attachment est une `ImmutableMap` Guava, donc `put`/`remove` échouaient. `NeoForgeGridRecipeStore.set` fait désormais une copie dans un nouveau `HashMap` avant `setData`. Affectait NeoForge 1.21.1 et 26.1.2.
- **Tooltip de slot superposé** — Les écrans de grille de Refined Storage overrident `renderTooltip` par sous-classe (`CraftingGridScreen`/`PatternGridScreen`/`AbstractGridScreen`) et dessinent eux-mêmes le tooltip du slot survolé ; un mixin sur le `AbstractContainerScreen.renderTooltip` vanilla était donc contourné et le tooltip « Stick » s'affichait par-dessus le popup. Le mixin de suppression cible désormais ces classes RS et s'active dès que le popup est ouvert. Le pipeline de rendu 26.x diffère, son fork est un no-op fail-soft.
- **Parsing des recettes par version MC** — Le JSON des ingrédients de craft diffère selon la version MC (1.21.1 en objet `{"item":"..."}`, 26.x en chaîne brute). Les recettes de test sont séparées par version et fusionnées par une tâche de build, donc les items de test se craftent désormais sur chaque cible.

### Modifications

- **Base Refined Storage 2 portée à 2.0.8** — Le build Fabric récupère Refined Storage 2.0.8 et Polymorph 1.1.0+1.21.1 depuis le maven Modrinth ; le plancher de dépendance `refinedstorage` déclaré reste `>= 2.0.1`. RS 2.x et RS 3.x partagent la même structure de packages interne (`com.refinedmods.refinedstorage.common.*`), donc les mixins sont compatibles ascendant sur toute la ligne RS 2.x sans changement de signature.
- **Chaîne de build** — Migration vers le build MultiLoader-Template (Gradle 9.5.1, Fabric Loom 1.16.3 pour le module Fabric, ModDevGradle 2.0.140 pour les modules common/NeoForge). Le wrapper Gradle est passé de 9.2.1 à 9.5.1 pour satisfaire Loom 1.16.3.

---

## [1.1.0] - 2026-06-02

### Added

- **Wireless Crafting Grid compatibility** (issue #2) — Polymorph recipe switching now works in the [Refined Storage - Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal) Wireless Crafting Grid. The side button already appeared (the wireless menu extends RS2's `AbstractCraftingGridContainerMenu`, so the screen is an `AbstractGridScreen`), but clicking a recipe did nothing because the entire selection bridge was keyed on a `BlockEntity` and the wireless grid has none — it is a transient, player-bound `CraftingGrid` built from a held item. Added a `BlockEntity`-free selection path: `AccessorAbstractCraftingGridContainerMenu` exposes the menu's `craftingGrid`, `SelectRecipePacket` resolves its `RecipeMatrix` from the existing `CONTAINER_TO_MATRIX` registry and drives `updateResult`, and a new per-matrix selection store keeps the choice sticky across input changes (the wireless equivalent of `RsGridRecipeData.selections`). The result reaches the client through the menu's crafting result slot on the next `broadcastChanges` tick — the same path the wired grid uses.

### Fixed

- **Wireless selection mis-routed onto an unrelated wired grid** — `SelectRecipePacket.findBlockEntity` had a global reverse-scan fallback that returned the first registered grid carrying Polymorph data regardless of which menu was open. For a wireless grid (which yields no `BlockEntity`) in a world with any loaded wired grid, the selection was applied to that unrelated wired grid and silently corrupted it. Removed the fallback; resolution is now strictly scoped to the open menu (slot scan, then grid-field accessor).

### Performance

- **No per-open memory leak for wireless grids** — Each opened wireless grid creates a `RecipeMatrix` registered in `CONTAINER_TO_MATRIX`, which strong-references it with no removal hook, so a `WeakHashMap` selection store alone could never reclaim it. Added `MixinAbstractGridContainerMenu`, a `removed(Player)` hook that deterministically unregisters the matrix and its selection on menu close — but only for grids with no `BlockEntity`, so the wired grid's `BlockEntity`-owned mapping is never dropped (guarded by `instanceof BlockEntity`). The per-matrix store is also guarded so it can never shadow a `BlockEntity`-backed selection.

### Compatibility

- **Quartz Arsenal is an optional, soft dependency** — Declared `optional` in `neoforge.mods.toml`. No Quartz Arsenal class is referenced anywhere in the source: the accessor targets the Refined Storage 2 base class `AbstractCraftingGridContainerMenu`, so the fix applies to the wireless grid (which extends it) and to any future `BlockEntity`-free crafting grid, with no compile-time dependency on the addon. The mod loads and runs unchanged when Quartz Arsenal is absent.

### Ajouts

- **Compatibilité Wireless Crafting Grid** (issue #2) — Le changement de recette Polymorph fonctionne désormais dans la Wireless Crafting Grid de [Refined Storage - Quartz Arsenal](https://www.curseforge.com/minecraft/mc-mods/refined-storage-quartz-arsenal). Le bouton latéral apparaissait déjà (le menu wireless étend `AbstractCraftingGridContainerMenu` de RS2, donc l'écran est un `AbstractGridScreen`), mais cliquer sur une recette ne faisait rien car tout le pont de sélection était indexé sur un `BlockEntity`, que la grille wireless ne possède pas — c'est un `CraftingGrid` éphémère, lié au joueur et construit depuis un objet tenu en main. Ajout d'un chemin de sélection sans `BlockEntity` : `AccessorAbstractCraftingGridContainerMenu` expose le `craftingGrid` du menu, `SelectRecipePacket` résout son `RecipeMatrix` depuis le registre existant `CONTAINER_TO_MATRIX` et déclenche `updateResult`, et un nouveau magasin de sélection par matrice garde le choix persistant à travers les changements d'ingrédients (l'équivalent wireless de `RsGridRecipeData.selections`). Le résultat parvient au client via le slot de résultat du menu au tick `broadcastChanges` suivant — le même chemin que la grille filaire.

### Correctifs

- **Sélection wireless dirigée par erreur vers une grille filaire sans rapport** — `SelectRecipePacket.findBlockEntity` avait un fallback de balayage global qui renvoyait la première grille enregistrée portant des données Polymorph, quelle que soit la grille ouverte. Pour une grille wireless (qui ne fournit aucun `BlockEntity`) dans un monde contenant une grille filaire chargée, la sélection était appliquée à cette grille filaire sans rapport et la corrompait silencieusement. Fallback supprimé ; la résolution est désormais strictement limitée au menu ouvert (scan des slots, puis accessor du champ grid).

### Performance

- **Plus de fuite mémoire à chaque ouverture pour les grilles wireless** — Chaque grille wireless ouverte crée un `RecipeMatrix` enregistré dans `CONTAINER_TO_MATRIX`, qui le référence fortement sans hook de suppression ; un magasin `WeakHashMap` seul ne pouvait donc jamais le récupérer. Ajout de `MixinAbstractGridContainerMenu`, un hook `removed(Player)` qui désenregistre de façon déterministe la matrice et sa sélection à la fermeture du menu — mais uniquement pour les grilles sans `BlockEntity`, afin que le mapping de la grille filaire (détenu par le `BlockEntity`) ne soit jamais supprimé (protégé par `instanceof BlockEntity`). Le magasin par matrice est aussi protégé pour ne jamais masquer une sélection adossée à un `BlockEntity`.

### Compatibilité

- **Quartz Arsenal est une dépendance optionnelle et souple** — Déclarée `optional` dans `neoforge.mods.toml`. Aucune classe de Quartz Arsenal n'est référencée dans le code source : l'accessor cible la classe de base de Refined Storage 2 `AbstractCraftingGridContainerMenu`, donc le correctif s'applique à la grille wireless (qui en hérite) et à toute future grille de craft sans `BlockEntity`, sans dépendance à la compilation envers l'addon. Le mod se charge et fonctionne à l'identique en l'absence de Quartz Arsenal.

---

## [1.0.9] - 2026-05-05

### Fixed

- **Root cause of the Create encased fan crash** (issue #1, reported by @djsime1) — Polymorph's `PolymorphApi.registerBlockEntity(Class, IRecipeDataFactory)` records the class as metadata only and stores every factory in a flat list; `createBlockEntityRecipeData(be)` then iterates that list and returns the first factory's non-null result, regardless of the BE's actual class. Our factories `be -> new RsGridRecipeData(be)` always returned non-null, so Polymorph attached `RsGridRecipeData` to every block entity in the world (encased fans, hoppers, etc.). Combined with Polymorph's `RecipeCache` being keyed by input alone — not by `RecipeType` — a SMOKING lookup primed the cache and the next SMELTING query with the same input returned SMOKING recipes, propagating a wrong-typed `RecipeHolder` until Create's `BlastingType.process` blew up on the implicit cast. Factories now guard with `instanceof` so non-RS2 block entities fall through to the next factory and never receive our data. The 1.0.8 `MixinRecipeManagerSafety` is kept as a defense-in-depth backstop.

### Correctifs

- **Cause racine du crash du ventilateur encastré Create** (issue #1, signalée par @djsime1) — `PolymorphApi.registerBlockEntity(Class, IRecipeDataFactory)` enregistre la classe comme simple métadonnée et stocke toutes les factories dans une liste plate ; `createBlockEntityRecipeData(be)` itère cette liste et renvoie le premier non-null, sans vérifier la classe réelle du BE. Nos factories `be -> new RsGridRecipeData(be)` renvoyaient toujours non-null, donc Polymorph attachait `RsGridRecipeData` à **tous** les block entities (ventilateurs Create, entonnoirs, etc.). Comme le `RecipeCache` interne de Polymorph est indexé par input seul — pas par `RecipeType` — une requête SMOKING amorçait le cache, et la requête SMELTING suivante avec le même input renvoyait des recettes SMOKING ; un `RecipeHolder` du mauvais type se propageait jusqu'à `BlastingType.process` de Create qui plantait sur le cast implicite. Les factories sont maintenant protégées par un `instanceof` pour que les block entities non-RS2 passent à la factory suivante et ne reçoivent jamais nos données. Le `MixinRecipeManagerSafety` ajouté en 1.0.8 reste en place comme deuxième ligne de défense.

---

## [1.0.8] - 2026-04-27

### Fixed

- **ClassCastException with Create encased fan + Polymorph 1.1.0** — Reported as a server-side crash in `AllFanProcessingTypes$BlastingType.process` (`SmeltingRecipe cannot be cast to SmokingRecipe`). Polymorph 1.1.0's `MixinRecipeManager` injects on `RecipeManager.getRecipeFor(RecipeType, RecipeInput, Level, RecipeHolder)` and, under specific cross-block-entity tick orderings, can resolve a stored recipe of a different `RecipeType` than the one requested. The caller (Create's blasting/smoking dual lookup) holds the result via Java type erasure and crashes at the first use-site cast. Added `MixinRecipeManagerSafety`, an `@Inject(at = RETURN, cancellable = true)` on the same overload that validates the runtime recipe type matches the requested `RecipeType`; on mismatch it falls back to a fresh `getRecipesFor(...).stream().findFirst()` lookup scoped to the requested type. Polymorph behavior is preserved on all matching-type paths.

### Correctifs

- **ClassCastException avec le ventilateur encastré de Create + Polymorph 1.1.0** — Signalé comme crash serveur dans `AllFanProcessingTypes$BlastingType.process` (`SmeltingRecipe cannot be cast to SmokingRecipe`). Le `MixinRecipeManager` de Polymorph 1.1.0 injecte sur `RecipeManager.getRecipeFor(RecipeType, RecipeInput, Level, RecipeHolder)` et, dans certains ordres de tick croisés entre block entities, peut renvoyer une recette enregistrée d'un `RecipeType` différent de celui demandé. L'appelant (la double recherche blasting/smoking de Create) reçoit le résultat via l'effacement de type Java et plante au premier cast utilisé. Ajout de `MixinRecipeManagerSafety`, un `@Inject(at = RETURN, cancellable = true)` sur la même surcharge qui vérifie que le type runtime de la recette correspond au `RecipeType` demandé ; en cas d'incohérence, il retombe sur une recherche `getRecipesFor(...).stream().findFirst()` scopée au type demandé. Le comportement de Polymorph est préservé sur tous les chemins où les types correspondent.

---

## [1.0.7] - 2026-04-20

### Fixed

- **Pattern Grid preview stuck on previous recipe (RS 2.0.2)** — After selecting a different recipe via the Polymorph popup, the Pattern Grid preview kept showing the previous recipe's output until the pattern was printed once. `MixinRecipeMatrix` only updated the `ResultContainer` via `setResult` but left `currentRecipe` unchanged, so the next `updateResult` short-circuit (`currentRecipe.matches(input)` returns true) re-assembled the old recipe and overwrote the preview. Now also syncs `currentRecipe` via the accessor whenever a Polymorph override is applied.
- **Singleplayer selection not applied server-side** — On singleplayer, `selectRecipe()` used `server.execute` with an `activeBlockEntity` captured from the client-side menu scan. That BE lives on the client level, so the subsequent `isClientSide` guard either skipped `updateResult` or ran it on the wrong level; the selection worked only as a side-effect of later `matrixChanged()` triggers. Unified the SP and MP paths: both now dispatch `SelectRecipePacket` over the local loopback (SP) or network (MP). The handler resolves the server-side BlockEntity via `player.containerMenu`, correctly scoped to the server level, and persists the selection in `RsGridRecipeData.selections`.

### Correctifs

- **Aperçu du Pattern Grid figé sur la recette précédente (RS 2.0.2)** — Après avoir choisi une autre recette via le popup Polymorph, l'aperçu du Pattern Grid gardait le résultat de la recette précédente jusqu'à la prochaine impression de patron. `MixinRecipeMatrix` ne mettait à jour que le `ResultContainer` via `setResult` sans toucher à `currentRecipe` ; le prochain `updateResult` court-circuitait (`currentRecipe.matches(input)` retourne vrai) et ré-assemblait l'ancienne recette, écrasant l'aperçu. Synchronise désormais aussi `currentRecipe` via l'accessor à chaque override Polymorph.
- **Sélection non appliquée côté serveur en solo** — En solo, `selectRecipe()` utilisait `server.execute` avec un `activeBlockEntity` capturé depuis le scan du menu côté client. Ce BE vit sur le niveau client, donc le garde `isClientSide` qui suivait faisait soit sauter `updateResult`, soit le lançait sur le mauvais niveau ; la sélection ne fonctionnait que par effet de bord des déclenchements ultérieurs de `matrixChanged()`. Chemins SP et MP unifiés : les deux expédient maintenant `SelectRecipePacket` via la boucle locale (SP) ou le réseau (MP). Le handler résout le BlockEntity côté serveur via `player.containerMenu`, correctement scopé sur le niveau serveur, et persiste la sélection dans `RsGridRecipeData.selections`.

### Cleanup

- **Nested duplicate clone removed** — An accidental inner `Arcadia-RsPolymorph/` git clone was sitting inside the working tree. It shadowed the main repo with a stale copy (one commit behind) and caused confusion. Deleted.

### Nettoyage

- **Clone imbriqué en double supprimé** — Un clone git interne accidentel `Arcadia-RsPolymorph/` se trouvait dans l'arbre de travail. Il masquait le dépôt principal avec une copie périmée (un commit de retard) et semait la confusion. Supprimé.

---

## [1.0.6] - 2026-04-09

### Fixed

- **Pattern Grid widget not created on dedicated server** — The Polymorph widget was never created for the Pattern Grid because the result slot detection relied on class name matching (`contains("DisabledSlot")`), which fails for anonymous inner classes like `PatternGridContainerMenu$5`. Now uses `instanceof` checks instead.
- **Client-side BlockEntity discovery on dedicated server** — On a dedicated server the menu's Grid field is null (client constructor uses GridData, not the real BE). Added a proximity-based fallback that finds the nearest grid BlockEntity registered in `CONTAINER_TO_BE` from chunk sync.
- **Pattern recipe tagging on dedicated server** — `createCraftingPattern()` reads the selected recipe ID from a static volatile field that was already cleared by the time RS2's create-pattern packet arrives. Now falls back to reading from `RsGridRecipeData.selections` (persisted by `SelectRecipePacket`), ensuring patterns are correctly tagged for autocrafting.

### Correctifs

- **Widget Pattern Grid non créé sur serveur dédié** — Le widget Polymorph n'était jamais créé pour la Grille de Patrons car la détection du slot résultat utilisait la correspondance par nom de classe (`contains("DisabledSlot")`), qui échoue pour les classes anonymes internes comme `PatternGridContainerMenu$5`. Utilise maintenant des vérifications `instanceof`.
- **Découverte du BlockEntity côté client sur serveur dédié** — Sur un serveur dédié, le champ Grid du menu est null (le constructeur client utilise GridData, pas le vrai BE). Ajout d'un fallback par proximité qui trouve le BlockEntity de grille le plus proche enregistré dans `CONTAINER_TO_BE` depuis la synchronisation de chunk.
- **Tagging de recette des patrons sur serveur dédié** — `createCraftingPattern()` lisait l'ID de recette sélectionnée depuis un champ static volatile déjà effacé au moment où le paquet create-pattern de RS2 arrive. Lit maintenant depuis `RsGridRecipeData.selections` (persisté par `SelectRecipePacket`), garantissant que les patrons sont correctement tagués pour l'autocraft.

---

## [1.0.5] - 2026-04-02

### Fixed

- **Pattern Grid on dedicated server** — Recipe selection now works on Pattern Grid in multiplayer. The server-side packet handler could not find the BlockEntity because PatternGrid uses phantom/filter slots; added fallback strategies (menu Grid accessor + reverse container lookup). Also, the selected recipe ID is now propagated to the server so crafting patterns get correctly tagged for autocrafting.

### Performance

- **Per-frame caching** — Container discovery and input hash computation are now cached per render frame, eliminating 2-3 redundant slot scans per frame.
- **ConcurrentHashMap** — Replaced `synchronized(WeakHashMap)` with `ConcurrentHashMap` for the container registry maps, removing global lock contention.
- **Matrix lookup caching** — `RsGridRecipeData.getMatrices()` results are now cached after first successful lookup.
- **Error logging** — Silent exception catch in recipe sync now logs warnings instead of swallowing errors.

### Correctifs

- **Pattern Grid sur serveur dédié** — La sélection de recette fonctionne maintenant sur la Grille de Patrons en multijoueur. Le handler de paquet côté serveur ne trouvait pas le BlockEntity car le PatternGrid utilise des slots fantômes/filtres ; ajout de stratégies de fallback (accessor Grid du menu + recherche inversée des containers). De plus, l'ID de recette sélectionnée est maintenant propagé au serveur pour que les patrons soient correctement tagués pour l'autocraft.

### Performance

- **Cache par frame** — La découverte des containers et le calcul du hash d'entrée sont maintenant cachés par frame de rendu, éliminant 2-3 scans de slots redondants par frame.
- **ConcurrentHashMap** — Remplacement de `synchronized(WeakHashMap)` par `ConcurrentHashMap` pour les maps de registre des containers, supprimant la contention de lock global.
- **Cache du lookup de matrices** — Les résultats de `RsGridRecipeData.getMatrices()` sont maintenant cachés après le premier lookup réussi.
- **Logging d'erreurs** — Le catch silencieux dans la sync des recettes log maintenant des warnings au lieu d'avaler les erreurs.

---

## [1.0.4] - 2026-03-31

### Fixed

- **Pattern Grid on dedicated server** — Further fix for Pattern Grid support on multiplayer servers. Improved container discovery by using the menu's Grid accessor as a fallback, then reverse-looking up registered containers from the block entity. This resolves cases where phantom/filter slots prevented direct slot scanning.

### Correctifs

- **Pattern Grid sur serveur dédié** — Correction supplémentaire du support de la Grille de Patrons sur les serveurs multijoueurs. Amélioration de la découverte des containers via l'accessor Grid du menu en fallback, puis recherche inversée des containers enregistrés depuis le block entity. Cela résout les cas où les slots fantômes/filtres empêchaient le scan direct des slots.

---

## [1.0.3] - 2026-03-30

### Fixed

- **Multiplayer / Dedicated server** — Recipe selection in the Polymorph popup now correctly updates the crafting grid output on dedicated servers. Added a client→server network packet so the server applies the selection.
- **Multiplayer / Dedicated server** — The Polymorph button now works on dedicated servers for both Crafting Grid and Pattern Grid. The widget no longer depends on server-side maps that may be empty on the client.
- **Pattern Grid on dedicated server** — Fixed the Polymorph button not appearing on the Pattern Grid in multiplayer. The widget now discovers the grid's block entity via the menu accessor as a fallback when slot scanning fails (phantom/filter slots).
- **Vanilla crafting table** — Polymorph's recipe selector no longer breaks on the vanilla crafting table after having used an RS2 grid. The rendering override is now limited to RS2 grid screens only.

### Correctifs

- **Multijoueur / Serveur dédié** — La sélection de recette dans le popup Polymorph met désormais correctement à jour la sortie de la grille de craft sur les serveurs dédiés. Ajout d'un paquet réseau client→serveur pour appliquer la sélection.
- **Multijoueur / Serveur dédié** — Le bouton Polymorph fonctionne désormais sur les serveurs dédiés pour la Grille de Craft et la Grille de Patrons. Le widget ne dépend plus de maps côté serveur potentiellement vides côté client.
- **Pattern Grid sur serveur dédié** — Correction du bouton Polymorph absent sur la Grille de Patrons en multijoueur. Le widget découvre désormais le block entity de la grille via l'accessor du menu en fallback quand le scan des slots échoue (slots fantômes/filtres).
- **Table de craft vanilla** — Le sélecteur de recettes Polymorph ne se bloque plus sur la table de craft vanilla après avoir utilisé une grille RS2. L'interception du rendu est désormais limitée aux grilles RS2 uniquement.

---

## [1.0.1] - 2026-03-24

### Fixed

- **Dedicated server crash** — The mod no longer crashes on server startup. All client-only code is now isolated in a separate class so the JVM doesn't try to load GUI types on the server.
- **Recipe selection not applying** — Fixed a case where selecting a recipe had no effect when the open grid's block entity could not be identified.
- **Server stability** — Added defensive error handling to server-side recipe sync to prevent Polymorph API issues from crashing the server.

### Correctifs

- **Crash au démarrage du serveur dédié** — Le mod ne crashe plus au démarrage du serveur. Tout le code client est désormais isolé dans une classe séparée pour éviter que le JVM charge des types GUI côté serveur.
- **Sélection de recette sans effet** — Correction d'un cas où la sélection d'une recette n'avait aucun effet lorsque le block entity de la grille ouverte ne pouvait pas être identifié.
- **Stabilité serveur** — Ajout d'une gestion d'erreur défensive dans la synchronisation des recettes côté serveur pour éviter qu'un problème de l'API Polymorph ne fasse crasher le serveur.

---

## [1.0.0] - 2026-03-20

### Added

- Initial release for Minecraft 1.21.1 (NeoForge 21.1.219+).
- Polymorph recipe selection support for the RS2 Crafting Grid.
- Polymorph recipe selection support for the RS2 Pattern Grid.
- Pattern items are tagged with the selected recipe so autocrafting uses the correct output.
- Multi-matrix support for both Crafting and Smithing table patterns.
- Stale recipe entries no longer appear in the popup when the grid contents change.
- Fixed item duplication / ghost item bug caused by evaluating non-empty recipes on empty matrices.

### Ajouts

- Première version pour Minecraft 1.21.1 (NeoForge 21.1.219+).
- Support de la sélection de recettes Polymorph pour la Grille de Craft RS2.
- Support de la sélection de recettes Polymorph pour la Grille de Patrons RS2.
- Les patrons sont marqués avec la recette sélectionnée pour que l'autocraft utilise le bon résultat.
- Support multi-matrices pour les patrons de table de craft et d'enclume.
- Les recettes obsolètes n'apparaissent plus dans le popup lorsque le contenu de la grille change.
- Correction d'un bug de duplication d'objets / objets fantômes causé par l'évaluation de recettes sur des matrices vides.
