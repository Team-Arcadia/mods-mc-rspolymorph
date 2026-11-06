# Compatibility

| Jar | Minecraft | Loader | Refined Storage | Java | Notes |
|-----|-----------|--------|-----------------|------|-------|
| `rspolymorph-neoforge-26.1.2-<version>.jar` | 26.1.2 | NeoForge 26.1.2.x | 3.2.0+ (built on 3.2.1) | 25 | |
| `rspolymorph-fabric-26.1.2-<version>.jar` | 26.1.2 | Fabric Loader 0.19.5+ and Fabric API | 3.2.0+ (built on 3.2.1) | 25 | |
| `rspolymorph-neoforge-1.21.1-<version>.jar` | 1.21.1 | NeoForge 21.1.219+ | 2.0.8+ (built on 2.0.9) | 21 | |
| `rspolymorph-fabric-1.21.1-<version>.jar` | 1.21.1 | Fabric Loader 0.16.10+ and Fabric API | 2.0.8+ (built on 2.0.9) | 21 | |
| `rspolymorph-neoforge-1.20.4-<version>.jar` | 1.20.4 | NeoForge 20.4.x | 1.13.0-beta.4+ | 17 | |
| `rspolymorph-forge-1.20.1-<version>.jar` | 1.20.1 | Forge 47+ or NeoForge 47.1 | 1.12.4+ | 17 | One jar for both loaders |
| `rspolymorph-forge-1.19.2-<version>.jar` | 1.19.2 | Forge 43+ | 1.11.7+ | 17 | |
| `rspolymorph-forge-1.18.2-<version>.jar` | 1.18.2 | Forge 40+ | 1.10.6+ | 17 | |
| `rspolymorph-forge-1.18.1-<version>.jar` | 1.18.1 | Forge 39+ | 1.10.1+ | 17 | |
| `rspolymorph-forge-1.16.5-<version>.jar` | 1.16.5 | Forge 36+ | 1.9.18+ | 8 | |
| `rspolymorph-forge-1.16.4-<version>.jar` | 1.16.4 | Forge 35 | 1.9.12+ | 8 || Java 8u312 or older: this Forge's own launcher crashes on newer Java 8 (seen on 8u322 and 8u504) |
| `rspolymorph-forge-1.16.3-<version>.jar` | 1.16.3 | Forge 34.1.0+ | 1.9.9+ | 8 || Java 8u312 or older: this Forge's own launcher crashes on newer Java 8 (seen on 8u504) |
| `rspolymorph-forge-1.16.2-<version>.jar` | 1.16.2 | Forge 33.0.20+ | 1.9.5+ | 8 | |
| `rspolymorph-forge-1.16.1-<version>.jar` | 1.16.1 | Forge 32.0.72+ | 1.9.2-beta.2+ | 8 | |
| `rspolymorph-forge-1.15.2-<version>.jar` | 1.15.2 | Forge 31.2.44+ | 1.8.8+ | 8 | |
| `rspolymorph-forge-1.15.1-<version>.jar` | 1.15.1 | Forge 30.0.43 to 30.0.47 | 1.8.0-beta+ | 8 | Forge JS coremod. Refined Storage 1.8 itself crashes on newer Forge |
| `rspolymorph-forge-1.14.4-<version>.jar` | 1.14.4 | Forge 28.2.30+ | 1.7.3-beta+ | 8 | Forge JS coremod |
| `rspolymorph-forge-1.12.2-<version>.jar` | 1.12.2 | Forge | 1.6.16+ | 8 | FML coremod, nothing extra to install |
| `rspolymorph-forge-1.12-<version>.jar` | 1.12 and 1.12.1 | Forge | 1.5.28+ | 8 | One jar for both versions. FML coremod, nothing extra to install |
| `rspolymorph-forge-1.11.2-<version>.jar` | 1.11.2 | Forge | 1.4.20+ | 8 | FML coremod, nothing extra to install |
| `rspolymorph-forge-1.11-<version>.jar` | 1.11 | Forge | 1.3.5+ | 8 | FML coremod, nothing extra to install |
| `rspolymorph-forge-1.10.2-<version>.jar` | 1.10.2 | Forge | 1.2.26+ | 8 | FML coremod; Refined Storage 1.2.26 itself requires MCMultiPart |
| `rspolymorph-forge-1.10-<version>.jar` | 1.10 | Forge | 0.8.10-beta+ | 8 | FML coremod; see the note on Refined Storage 0.x |
| `rspolymorph-forge-1.9.4-<version>.jar` | 1.9.4 | Forge | 0.7.19-beta+ | 8 | FML coremod; see the note on Refined Storage 0.x |
| `rspolymorph-forge-1.9-<version>.jar` | 1.9 | Forge | 0.6.15-alpha+ | 8 | FML coremod; see the note on Refined Storage 0.x |

Refined Storage 0.x (1.10, 1.9.4, 1.9): its patterns store outputs and have no recipe, so a printed
pattern keeps the selected output instead of being tagged with a recipe id. The selection on the
Crafting Grid works as everywhere else.

- **Polymorph is not needed**: recipe selection is built in.
- **Install on both sides**: the client draws the button and the popup, the server applies the
  choice and resolves patterns during autocrafting. Since 1.3.0 the network protocol differs from
  older versions: update client and server together.
- **Optional**: Refined Storage - Quartz Arsenal 1.0.7 or later, for its Wireless Crafting Grid.
- **Minecraft 26.2 and 26.3 are absent** because the maintainer of Refined Storage declined those
  ports (refinedmods/refinedstorage2 issue
  [#1385](https://github.com/refinedmods/refinedstorage2/issues/1385) and pull request
  [#1423](https://github.com/refinedmods/refinedstorage2/pull/1423)). This mod cannot exist there
  without Refined Storage.

## Polymorph family

- **Polymorph and Polymorph+** work next to the mod.
- On **1.16.5, 1.18.2, 1.19.2 and 1.20.1**, Polymorph+ has its own Refined Storage integration. RS
  Polymorph leaves the crafting grid selection to it and keeps tagging patterns.
- **Polymorphic AE2, Occultism, Extended Crafting and Tinkers** touch no Refined Storage class.

## Known issues

- **Occultism 1.262.0 on 26.1.2** throws `NoClassDefFoundError` on leaving a world when JEI is not
  installed. This mod does not cause it: Occultism registers the listener in its JEI plugin class.

## Test results

The in-game selftest (`-Pselftest`, see `docs/BUILDING.md`) opens real grid screens in a world: it
selects recipes, crafts, shift-crafts, reloads the world, prints patterns and sends forged packets.
Counts are checks passed; **no check failed on any target**. SKIP lines, printed for what a version of
Refined Storage does not have, are not counted.

- **Development**: the development environment of the target.
- **Real**: the selftest jar in a real install of the loader, with the published Refined Storage jar.
  Its mod classes are identical to the released jar's.
- **Reobf**: the reobfuscated selftest jar in a mods folder, on the game's own names, without a loader
  installer.
- **Server**: dedicated server distribution, mods loaded and no client class reachable. A real dedicated
  server reaching `Done`, and a client playing on it, are listed under the table.

| Target | Tested with | Development | Real | Reobf | Server |
|--------|-------------|-------------|------|-------|--------|
| neoforge-26.1.2 | NeoForge 26.1.2, RS 3.2.1 | 68 | 68 (NeoForge 26.1.2.126) | | 11 |
| fabric-26.1.2 | Fabric Loader 0.19.5, RS 3.2.1 | 68 | | | 13 |
| neoforge-1.21.1 | NeoForge 21.1, RS 2.0.9 | 67 | 67 (NeoForge 21.1.265) | | 13 |
| fabric-1.21.1 | Fabric Loader 0.16.10, RS 2.0.9 | 67 | 67 (Fabric Loader 0.20.1) | | 13 |
| neoforge-1.20.4 | NeoForge 20.4.253, RS 1.13.0-beta.4 | 64 | 64 | | 10 |
| forge-1.20.1 | Forge 47.4.10, RS 1.12.4 | 64 | 64 | | 10 |
| forge-1.20.1 | NeoForge 47.1.106, RS 1.12.4 | | 64 | | |
| forge-1.19.2 | Forge 43.5.2, RS 1.11.7 | 64 | 64 | | 10 |
| forge-1.18.2 | Forge 40.3.12, RS 1.10.6 | 64 | 64 | | 10 |
| forge-1.18.1 | Forge 39.1.2, RS 1.10.1 | 64 | 64 | | 10 |
| forge-1.16.5 | Forge 36.2.42, RS 1.9.18 | 64 | 64 | | 11 |
| forge-1.16.4 | Forge 35.1.37, RS 1.9.12 | 64 | 64 (Java 8u312) | | 11 |
| forge-1.16.3 | Forge 34.1.42, RS 1.9.9 | 64 | 64 (Java 8u312) | | 11 |
| forge-1.16.2 | Forge 33.0.61, RS 1.9.5 | 64 | 64 | | 11 |
| forge-1.16.1 | Forge 32.0.108, RS 1.9.2-beta.2 | 64 | 64 | | 11 |
| forge-1.15.2 | Forge 31.2.57, RS 1.8.8 | 64 | 64 | | 11 |
| forge-1.15.1 | Forge 30.0.43, RS 1.8 | 68 | 68 | | 14 |
| forge-1.14.4 | Forge 28.2.30, RS 1.7.3-beta | 68 | 68 | | 14 |
| forge-1.12.2 | Forge 14.23.5.2860, RS 1.6.16 | 75 | 75 | 75 | 9 |
| forge-1.12 | Forge 14.21.1.2443, RS 1.5.28 | 76 | | 76 | 9 |
| forge-1.12 on 1.12.1 | Forge 14.22.1.2485, RS 1.5.28 | | | 76 | 9 |
| forge-1.11.2 | Forge 13.20.1.2588, RS 1.4.20 | 74 | | 74 | 9 |
| forge-1.11 | Forge 13.19.1.2199, RS 1.3.5 | 74 | | 74 | 9 |
| forge-1.10.2 | Forge 12.18.3.2511, RS 1.2.26 | 74 | | 74 | 9 |
| forge-1.10 | Forge 12.18.0.2000, RS 0.8.10-beta | 72 | | 72 | 9 |
| forge-1.9.4 | Forge 12.17.0.1909, RS 0.7.19-beta | 72 | | 72 | 9 |
| forge-1.9 | Forge 12.16.1.1887, RS 0.6.15-alpha | 70 | | 70 | 9 |

**With the Polymorph family**, no mixin conflict and no failed check:

| Minecraft | Installed next to the mod |
|-----------|---------------------------|
| 26.1.2, NeoForge | Polymorph+ 1.4.2, Polymorphic AE2, Extended Crafting and Occultism with their base mods (development, NeoForge 26.1.2.126 as Occultism requires) |
| 26.1.2, Fabric | Polymorph+ 1.4.2 (development) |
| 1.21.1, NeoForge | Polymorph 1.2.0, then Polymorph+ 1.4.2, each with Polymorphic AE2, Extended Crafting and Occultism and their base mods (development) |
| 1.21.1, Fabric | Polymorph 1.2.0, then Polymorph+ 1.4.2 (development) |
| 1.20.1, Forge | Polymorph 0.49.11, then Polymorph+ 1.4.2, each with Polymorphic Extended Crafting, Occultism and Tinkers and their base mods (real install) |
| 1.19.2 and 1.18.2, Forge | Polymorph+ 1.4.2 (development) |
| 1.16.5, Forge | Polymorph+ 1.4.2 with Polymorphic Extended Crafting, Occultism and Tinkers and their base mods (real install) |
| 1.12.2, Forge | Polymorph+ 1.4.2 and MixinBooter (reobfuscated jar) |

**Real dedicated server, one client playing on it:** fabric-26.1.2, forge-1.20.1 and forge-1.12.2.
A real dedicated server also reached `Done` on the other 1.21.1 and 26.1.2 targets.

**Next Refined Storage:** the two 26.1.2 targets pass against its `develop` branch (commit d33a173) and
the two 1.21.1 targets against `release/2.0.10` (commit 0f89121), client selftest only.

**Not tested:**

- Two real clients on one grid: the send list is checked with simulated players on the Refined Storage
  1.x line, and a single client otherwise.
- A complete autocrafting task: patterns are resolved through Refined Storage's normal path, but no task
  was run to its end.
- The screens of the add-ons themselves (AE2 terminals, Extended Crafting tables, Occultism, Tinkers):
  these runs prove that the mods load together and that the grids of this mod work next to them.
- Polymorphic Tinkers on 26.1.2: Tinkers' Construct and Mantle have no 26.1.2 build to run it with.
- The Quartz Arsenal Wireless Crafting Grid.
- Real installs of forge-1.12, 1.11.2, 1.11, 1.10.2, 1.10, 1.9.4 and 1.9: reobfuscated jar only.

---

# Compatibilité

| Jar | Minecraft | Loader | Refined Storage | Java | Remarques |
|-----|-----------|--------|-----------------|------|-------|
| `rspolymorph-neoforge-26.1.2-<version>.jar` | 26.1.2 | NeoForge 26.1.2.x | 3.2.0+ (compilé sur 3.2.1) | 25 | |
| `rspolymorph-fabric-26.1.2-<version>.jar` | 26.1.2 | Fabric Loader 0.19.5+ et Fabric API | 3.2.0+ (compilé sur 3.2.1) | 25 | |
| `rspolymorph-neoforge-1.21.1-<version>.jar` | 1.21.1 | NeoForge 21.1.219+ | 2.0.8+ (compilé sur 2.0.9) | 21 | |
| `rspolymorph-fabric-1.21.1-<version>.jar` | 1.21.1 | Fabric Loader 0.16.10+ et Fabric API | 2.0.8+ (compilé sur 2.0.9) | 21 | |
| `rspolymorph-neoforge-1.20.4-<version>.jar` | 1.20.4 | NeoForge 20.4.x | 1.13.0-beta.4+ | 17 | |
| `rspolymorph-forge-1.20.1-<version>.jar` | 1.20.1 | Forge 47+ ou NeoForge 47.1 | 1.12.4+ | 17 | Un seul jar pour les deux loaders |
| `rspolymorph-forge-1.19.2-<version>.jar` | 1.19.2 | Forge 43+ | 1.11.7+ | 17 | |
| `rspolymorph-forge-1.18.2-<version>.jar` | 1.18.2 | Forge 40+ | 1.10.6+ | 17 | |
| `rspolymorph-forge-1.18.1-<version>.jar` | 1.18.1 | Forge 39+ | 1.10.1+ | 17 | |
| `rspolymorph-forge-1.16.5-<version>.jar` | 1.16.5 | Forge 36+ | 1.9.18+ | 8 | |
| `rspolymorph-forge-1.16.4-<version>.jar` | 1.16.4 | Forge 35 | 1.9.12+ | 8 || Java 8u312 ou plus ancien : le lanceur de ce Forge plante sur les Java 8 plus récents (vu sur 8u322 et 8u504) |
| `rspolymorph-forge-1.16.3-<version>.jar` | 1.16.3 | Forge 34.1.0+ | 1.9.9+ | 8 || Java 8u312 ou plus ancien : le lanceur de ce Forge plante sur les Java 8 plus récents (vu sur 8u504) |
| `rspolymorph-forge-1.16.2-<version>.jar` | 1.16.2 | Forge 33.0.20+ | 1.9.5+ | 8 | |
| `rspolymorph-forge-1.16.1-<version>.jar` | 1.16.1 | Forge 32.0.72+ | 1.9.2-beta.2+ | 8 | |
| `rspolymorph-forge-1.15.2-<version>.jar` | 1.15.2 | Forge 31.2.44+ | 1.8.8+ | 8 | |
| `rspolymorph-forge-1.15.1-<version>.jar` | 1.15.1 | Forge 30.0.43 à 30.0.47 | 1.8.0-beta+ | 8 | Coremod JS Forge. Refined Storage 1.8 plante lui-même sur les Forge plus récents |
| `rspolymorph-forge-1.14.4-<version>.jar` | 1.14.4 | Forge 28.2.30+ | 1.7.3-beta+ | 8 | Coremod JS Forge |
| `rspolymorph-forge-1.12.2-<version>.jar` | 1.12.2 | Forge | 1.6.16+ | 8 | Coremod FML, rien de plus à installer |
| `rspolymorph-forge-1.12-<version>.jar` | 1.12 et 1.12.1 | Forge | 1.5.28+ | 8 | Un seul jar pour les deux versions. Coremod FML, rien de plus à installer |
| `rspolymorph-forge-1.11.2-<version>.jar` | 1.11.2 | Forge | 1.4.20+ | 8 | Coremod FML, rien de plus à installer |
| `rspolymorph-forge-1.11-<version>.jar` | 1.11 | Forge | 1.3.5+ | 8 | Coremod FML, rien de plus à installer |
| `rspolymorph-forge-1.10.2-<version>.jar` | 1.10.2 | Forge | 1.2.26+ | 8 | Coremod FML ; Refined Storage 1.2.26 demande lui-même MCMultiPart |
| `rspolymorph-forge-1.10-<version>.jar` | 1.10 | Forge | 0.8.10-beta+ | 8 | Coremod FML ; voir la note sur Refined Storage 0.x |
| `rspolymorph-forge-1.9.4-<version>.jar` | 1.9.4 | Forge | 0.7.19-beta+ | 8 | Coremod FML ; voir la note sur Refined Storage 0.x |
| `rspolymorph-forge-1.9-<version>.jar` | 1.9 | Forge | 0.6.15-alpha+ | 8 | Coremod FML ; voir la note sur Refined Storage 0.x |

Refined Storage 0.x (1.10, 1.9.4, 1.9) : ses patrons stockent des sorties et n'ont pas de recette :
un patron imprimé garde donc la sortie choisie au lieu d'être marqué avec un identifiant de recette.
La sélection sur la grille de craft fonctionne comme partout ailleurs.

- **Polymorph n'est pas nécessaire** : la sélection de recette est intégrée.
- **À installer des deux côtés** : le client affiche le bouton et le popup, le serveur applique le
  choix et résout les patrons pendant l'autocraft. Depuis la 1.3.0 le protocole réseau diffère des
  versions précédentes : mettre à jour le client et le serveur ensemble.
- **Optionnel** : Refined Storage - Quartz Arsenal 1.0.7 ou plus, pour sa grille de craft sans fil.
- **Minecraft 26.2 et 26.3 sont absents** car le mainteneur de Refined Storage a refusé ces portages
  (refinedmods/refinedstorage2, issue
  [#1385](https://github.com/refinedmods/refinedstorage2/issues/1385) et pull request
  [#1423](https://github.com/refinedmods/refinedstorage2/pull/1423)). Ce mod ne peut pas exister
  là-bas sans Refined Storage.

## Famille Polymorph

- **Polymorph et Polymorph+** fonctionnent à côté du mod.
- En **1.16.5, 1.18.2, 1.19.2 et 1.20.1**, Polymorph+ a sa propre intégration Refined Storage. RS
  Polymorph lui laisse la sélection de la grille de craft et continue de marquer les patrons.
- **Polymorphic AE2, Occultism, Extended Crafting et Tinkers** ne touchent aucune classe de Refined
  Storage.

## Problèmes connus

- **Occultism 1.262.0 en 26.1.2** lève `NoClassDefFoundError` en quittant un monde quand JEI n'est
  pas installé. Ce mod n'en est pas la cause : Occultism enregistre l'écouteur dans sa classe de
  plugin JEI.

## Résultats de test

Le test automatique en jeu (`-Pselftest`, voir `docs/BUILDING.md`) ouvre de vrais écrans de grille dans
un monde : il sélectionne des recettes, crafte, shift-crafte, recharge le monde, imprime des patrons et
envoie des paquets forgés. **Aucune vérification n'a échoué, sur aucune cible.** Le tableau détaillé, par
cible et par mode d'exécution, est dans la section anglaise Test results ci-dessus.

- **Vraie installation** : toutes les cibles de 1.14.4 à 1.20.4, 1.12.2, neoforge-26.1.2, neoforge-1.21.1 et fabric-1.21.1.
- **Jar reobfusqué dans un dossier mods, sans installeur** : forge-1.12 (aussi sur 1.12.1), 1.11.2, 1.11,
  1.10.2, 1.10, 1.9.4 et 1.9.
- **Vrai serveur dédié avec un client connecté** : fabric-26.1.2, forge-1.20.1 et forge-1.12.2.
- **Famille Polymorph** : Polymorph et Polymorph+ de 1.12.2 à 26.1.2, avec Polymorphic AE2, Extended
  Crafting, Occultism et Tinkers là où leurs mods de base existent, sans conflit de mixins ni échec.

**Non testé :** deux vrais clients sur la même grille, une tâche d'autocraft complète, les écrans des
add-ons eux-mêmes, Polymorphic Tinkers en 26.1.2 (Tinkers' Construct et Mantle n'y ont pas de version), et
la grille sans fil de Quartz Arsenal.
