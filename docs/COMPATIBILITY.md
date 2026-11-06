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
| `rspolymorph-forge-1.16.4-<version>.jar` | 1.16.4 | Forge 35 | 1.9.12+ | 8 | |
| `rspolymorph-forge-1.16.3-<version>.jar` | 1.16.3 | Forge 34.1.0+ | 1.9.9+ | 8 | |
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

<!-- RESULTS -->

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
| `rspolymorph-forge-1.16.4-<version>.jar` | 1.16.4 | Forge 35 | 1.9.12+ | 8 | |
| `rspolymorph-forge-1.16.3-<version>.jar` | 1.16.3 | Forge 34.1.0+ | 1.9.9+ | 8 | |
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

Le tableau final est dans la section Test results ci-dessus.
