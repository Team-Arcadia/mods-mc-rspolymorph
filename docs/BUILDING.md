# Building

Each target is a standalone Gradle project, in its own folder, with its own wrapper. The
loader-independent code is shared from `common/<minecraft version>`.

| Folder | Builds | Java |
|--------|--------|------|
| `neoforge-1.21.1` | `build/libs/rspolymorph-neoforge-1.21.1-<version>.jar` | 21 |
| `fabric-1.21.1` | `build/libs/rspolymorph-fabric-1.21.1-<version>.jar` | 21 |
| `neoforge-26.1.2` | `build/libs/rspolymorph-neoforge-26.1.2-<version>.jar` | 25 |
| `fabric-26.1.2` | Not built yet: waits for a stable Fabric Loom with Minecraft 26.x support ([#5](https://github.com/Team-Arcadia/mods-mc-rspolymorph/issues/5)) | 25 |
| `common/1.21.1` | Shared code for 1.21.1, read by both 1.21.1 targets | |
| `common/26.1.2` | The files whose 26.x API differs; the rest of `common/1.21.1` is reused with the 26.x renames | |

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph

cd neoforge-1.21.1 && ./gradlew build        # one target
./gradlew runClient                          # a development client, in neoforge-1.21.1/run
cd ..

tools/collect_jars.sh --build                # every target, jars copied to jars/<version>/
```

- Gradle toolchains download Java 21 or 25 when they are missing: any JDK can start the build.
- Refined Storage comes from the Modrinth maven, pinned by version id per loader. Nothing has to be
  downloaded by hand.
- `jars/` is local and ignored by git: it keeps the jars of every version you built.
- The version is `mod_version` in the `gradle.properties` of each target: a release changes it in
  every target folder.

## In-game selftest

`-Pselftest` compiles an automated test of the real screens and runs it in a game client. A window
opens for about two minutes, a flat world is created, the test plays, the game closes by itself.

```bash
cd neoforge-1.21.1 && ./gradlew runClientSelftest -Pselftest      # any of the four targets
```

- Each check prints `RSP-SELFTEST PASS <name>` or `RSP-SELFTEST FAIL <name>: <why>`, and the run ends
  with `RSP-SELFTEST DONE pass=N fail=M`. The same lines go to `rspolymorph-selftest.txt` in the run
  folder, screenshots to its `screenshots/`: `run/selftest` on NeoForge, `runs/selftest` on Fabric.
- The sources are in `common/<minecraft version>/src/selftest`, with a mixin config of their own. A build
  without `-Pselftest` does not compile them, and a build with it names its jar `...-SELFTEST-...`:
  never release that one.
- `./gradlew runServerSelftest -Pselftest` proves the dedicated server distribution in `run/selftest-server`
  or `runs/selftest-server`, with `rspolymorph-selftest-server.txt`. It goes through the game test launch,
  so it needs no EULA and leaves `eula.txt` alone. It loads the mod, audits the common mixins, force-loads
  what they target and proves that no client class is reachable. Only trust its `DONE` line, never an exit
  code: a server that stops early exits 0 and prints nothing.
- To test next to another mod, drop its jar in the `mods` folder of that run folder. The run keeps the
  same checks.
- On the Refined Storage 1.x line (`forge-1.20.1`, `neoforge-1.20.4`) the sources are in
  `common/1.20.1/src/selftest`, and what differs by Minecraft version is forked in
  `common/<minecraft version>/src/selftest`. The run folders are `run/selftest` and `run/selftest-server`.
- On `forge-1.20.1`, `-PpolymorphJar=<a jar with development names>` runs in `run-polymorph/selftest` a scenario
  of its own: this mod stays out of selection and printed patterns are still tagged.
- On that line `-PauditMixins` works only with `-Pselftest`: it applies every mixin as soon as the game reaches
  the selftest's hooks, in any run, and logs `RSP-AUDIT`. A released jar has no such code.

---

# Compilation

Chaque cible est un projet Gradle autonome, dans son propre dossier, avec son propre wrapper. Le
code indépendant du loader est partagé depuis `common/<version de minecraft>`.

| Dossier | Produit | Java |
|---------|---------|------|
| `neoforge-1.21.1` | `build/libs/rspolymorph-neoforge-1.21.1-<version>.jar` | 21 |
| `fabric-1.21.1` | `build/libs/rspolymorph-fabric-1.21.1-<version>.jar` | 21 |
| `neoforge-26.1.2` | `build/libs/rspolymorph-neoforge-26.1.2-<version>.jar` | 25 |
| `fabric-26.1.2` | Pas encore compilé : attend une version stable de Fabric Loom compatible Minecraft 26.x ([#5](https://github.com/Team-Arcadia/mods-mc-rspolymorph/issues/5)) | 25 |
| `common/1.21.1` | Code partagé 1.21.1, lu par les deux cibles 1.21.1 | |
| `common/26.1.2` | Les fichiers dont l'API 26.x diffère ; le reste de `common/1.21.1` est réutilisé avec les renommages 26.x | |

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph

cd neoforge-1.21.1 && ./gradlew build        # une cible
./gradlew runClient                          # un client de développement, dans neoforge-1.21.1/run
cd ..

tools/collect_jars.sh --build                # toutes les cibles, jars copiés dans jars/<version>/
```

- Les toolchains Gradle téléchargent Java 21 ou 25 s'ils manquent : n'importe quel JDK peut lancer le build.
- Refined Storage vient du maven Modrinth, fixé par identifiant de version pour chaque loader. Rien
  n'est à télécharger à la main.
- `jars/` est local et ignoré par git : il garde les jars de chaque version compilée.
- La version est `mod_version` dans le `gradle.properties` de chaque cible : une release la change
  dans chaque dossier cible.

## Test automatique en jeu

`-Pselftest` compile un test automatique des vrais écrans et le lance dans un client. Une fenêtre
s'ouvre pendant deux minutes environ, un monde plat est créé, le test se joue, le jeu se ferme seul.

```bash
cd neoforge-1.21.1 && ./gradlew runClientSelftest -Pselftest      # n'importe laquelle des quatre cibles
```

- Chaque vérification affiche `RSP-SELFTEST PASS <nom>` ou `RSP-SELFTEST FAIL <nom>: <raison>`, et le
  lancement se termine par `RSP-SELFTEST DONE pass=N fail=M`. Les mêmes lignes vont dans
  `rspolymorph-selftest.txt` du dossier de lancement, les captures dans son `screenshots/` : `run/selftest`
  sur NeoForge, `runs/selftest` sur Fabric.
- Les sources sont dans `common/<version de minecraft>/src/selftest`, avec leur propre configuration de
  mixins. Un build sans `-Pselftest` ne les compile pas, et un build avec nomme son jar `...-SELFTEST-...` :
  ne jamais publier celui-là.
- `./gradlew runServerSelftest -Pselftest` prouve la distribution serveur dédié dans `run/selftest-server` ou
  `runs/selftest-server`, avec `rspolymorph-selftest-server.txt`. Il passe par le lancement des game tests,
  donc sans EULA et sans toucher à `eula.txt`. Il charge le mod, audite les mixins communs, force le
  chargement de leurs cibles et prouve qu'aucune classe client n'est atteignable. Ne se fier qu'à sa ligne
  `DONE`, jamais à un code de sortie : un serveur arrêté tôt sort avec 0 et n'affiche rien.
- Pour tester à côté d'un autre mod, déposer son jar dans le dossier `mods` de ce dossier de lancement. Les
  vérifications restent les mêmes.
- Sur la ligne Refined Storage 1.x (`forge-1.20.1`, `neoforge-1.20.4`) les sources sont dans
  `common/1.20.1/src/selftest`, et ce qui change selon la version de Minecraft est forké dans
  `common/<version de minecraft>/src/selftest`. Les dossiers de lancement sont `run/selftest` et `run/selftest-server`.
- Sur `forge-1.20.1`, `-PpolymorphJar=<un jar aux noms de développement>` joue dans `run-polymorph/selftest` un scénario
  à part : ce mod reste hors de la sélection et les patterns imprimés restent marqués.
- Sur cette ligne, `-PauditMixins` ne marche qu'avec `-Pselftest` : il applique tous les mixins dès que le jeu atteint
  les points d'entrée du test, dans n'importe quel lancement, et écrit `RSP-AUDIT` au log. Un jar publié n'a pas ce code.
