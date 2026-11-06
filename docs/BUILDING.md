# Building

Each target is a standalone Gradle project, in its own folder, with its own wrapper. The
loader-independent code is shared from `common/<minecraft version>`.

| Folders | Builds with | Run Gradle on | Compiles for |
|---------|-------------|---------------|--------------|
| `neoforge-26.1.2`, `fabric-26.1.2` | ModDevGradle on Gradle 9.5, Loom 1.18 on Gradle 9.7 | Java 25 | Java 25 |
| `neoforge-1.21.1`, `fabric-1.21.1` | ModDevGradle, Loom 1.16, Gradle 9.5 | Java 21 | Java 21 |
| `neoforge-1.20.4`, `forge-1.20.1`, `forge-1.19.2`, `forge-1.18.2`, `forge-1.18.1` | ModDevGradle (legacy Forge variant on Forge), Gradle 9.2 | Java 21 | Java 17 |
| `forge-1.16.5`, `forge-1.16.4`, `forge-1.16.3`, `forge-1.16.2`, `forge-1.16.1`, `forge-1.15.2`, `forge-1.15.1`, `forge-1.14.4` | ForgeGradle 6, Gradle 8.14 | Java 21 | Java 8 |
| `forge-1.12.2` | RetroFuturaGradle 1.4.9, Gradle 8.14 | Java 21, not a Java 8 `JAVA_HOME` | Java 8 |
| `forge-1.12` | ForgeGradle 2.3, Gradle 4.10 | Java 8 only | Java 8 |
| `forge-1.11.2`, `forge-1.11`, `forge-1.10.2`, `forge-1.10` | ForgeGradle 2.2, Gradle 3.5 | Java 8 only | Java 8 |

Every target writes `build/libs/rspolymorph-<folder>-<version>.jar`.

### Shared code

`common/<minecraft version>` holds the code, and each target assembles its sources from layers:

| Target | Layers, the first one wins |
|--------|----------------------------|
| `neoforge-1.21.1`, `fabric-1.21.1` | `common/1.21.1` |
| `neoforge-26.1.2`, `fabric-26.1.2` | `common/26.1.2`, then `common/1.21.1` with the 26.x renames |
| `forge-1.20.1` | `common/1.20.1` |
| `neoforge-1.20.4` | `common/1.20.4`, then `common/1.20.1` |
| `forge-1.19.2`, `forge-1.18.2`, `forge-1.18.1` | `common/1.19.2`, then `common/1.20.1` |
| `forge-1.16.5`, `forge-1.16.4`, `forge-1.16.3`, `forge-1.16.2` | `common/1.16.5` |
| `forge-1.16.1` | `common/1.16.1`, then `common/1.16.5` |
| `forge-1.15.2` | `common/1.15.2`, then `common/1.16.5` |
| `forge-1.15.1` | `common/1.15.1`, `common/1.15.2`, then `common/1.16.5` |
| `forge-1.14.4` | `common/1.14.4`, `common/1.15.1`, `common/1.15.2`, then `common/1.16.5` |
| `forge-1.12.2` | `common/1.12.2` |
| `forge-1.12` | `common/1.12`, then `common/1.12.2` |
| `forge-1.11.2`, `forge-1.11` | `common/1.11.2`, `common/1.12`, then `common/1.12.2` |
| `forge-1.10.2` | `common/1.10.2`, `common/1.11.2`, `common/1.12`, then `common/1.12.2` |
| `forge-1.10` | `common/1.10`, `common/1.10.2`, `common/1.11.2`, `common/1.12`, `common/1.12.2` |

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph

cd neoforge-1.21.1 && ./gradlew build        # one target
./gradlew runClient                          # a development client, in neoforge-1.21.1/run
cd ..

tools/collect_jars.sh --build                # every target, jars copied to jars/<version>/
```

- Start Gradle on the Java of the table. Toolchains download the compile JDK when it is missing,
  except on ForgeGradle 2.x, which compiles on the Java that runs it.
- `tools/collect_jars.sh --build` runs the ForgeGradle 2.x targets on `JAVA8_HOME` when it is set.
- Refined Storage comes from the Modrinth maven, pinned by version id per loader. Nothing has to be
  downloaded by hand.
- `jars/` is local and ignored by git: it keeps the jars of every version you built.
- The version is `mod_version` in the `gradle.properties` of each target: a release changes it in
  every target folder.

## In-game selftest

`-Pselftest` compiles an automated test of the real screens and runs it in a game client. A window
opens for about two minutes, a flat world is created, the test plays, the game closes by itself.

```bash
cd neoforge-1.21.1 && ./gradlew runClientSelftest -Pselftest      # any target
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
- `-Prs_jar=<path>` with `-Pselftest` runs the selftest against another Refined Storage jar, to check a
  version floor.
- On `forge-1.12`, `-Ptarget_mc=1.12.1` makes the development runs and `runObfClient` use Minecraft 1.12.1.
  The jar is still built on 1.12 and is the same for both. `-Pselftest -Pobf_run` runs the built jar with
  production names on the ForgeGradle 2.x targets.
- `-PpolymorphJar` also exists on `forge-1.16.5`, with a Polymorph+ jar.
- On that line `-PauditMixins` works only with `-Pselftest`: it applies every mixin as soon as the game reaches
  the selftest's hooks, in any run, and logs `RSP-AUDIT`. A released jar has no such code.

---

# Compilation

Chaque cible est un projet Gradle autonome, dans son propre dossier, avec son propre wrapper. Le
code indépendant du loader est partagé depuis `common/<version de minecraft>`.

| Dossiers | Compilé avec | Gradle tourne sur | Compile pour |
|----------|--------------|-------------------|--------------|
| `neoforge-26.1.2`, `fabric-26.1.2` | ModDevGradle sur Gradle 9.5, Loom 1.18 sur Gradle 9.7 | Java 25 | Java 25 |
| `neoforge-1.21.1`, `fabric-1.21.1` | ModDevGradle, Loom 1.16, Gradle 9.5 | Java 21 | Java 21 |
| `neoforge-1.20.4`, `forge-1.20.1`, `forge-1.19.2`, `forge-1.18.2`, `forge-1.18.1` | ModDevGradle (variante legacy Forge sur Forge), Gradle 9.2 | Java 21 | Java 17 |
| `forge-1.16.5`, `forge-1.16.4`, `forge-1.16.3`, `forge-1.16.2`, `forge-1.16.1`, `forge-1.15.2`, `forge-1.15.1`, `forge-1.14.4` | ForgeGradle 6, Gradle 8.14 | Java 21 | Java 8 |
| `forge-1.12.2` | RetroFuturaGradle 1.4.9, Gradle 8.14 | Java 21, pas un `JAVA_HOME` Java 8 | Java 8 |
| `forge-1.12` | ForgeGradle 2.3, Gradle 4.10 | Java 8 uniquement | Java 8 |
| `forge-1.11.2`, `forge-1.11`, `forge-1.10.2`, `forge-1.10` | ForgeGradle 2.2, Gradle 3.5 | Java 8 uniquement | Java 8 |

Chaque cible produit `build/libs/rspolymorph-<dossier>-<version>.jar`. L'ordre des couches de code
partagé par cible est dans le tableau « Shared code » ci-dessus : la première couche l'emporte.

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph

cd neoforge-1.21.1 && ./gradlew build        # une cible
./gradlew runClient                          # un client de développement, dans neoforge-1.21.1/run
cd ..

tools/collect_jars.sh --build                # toutes les cibles, jars copiés dans jars/<version>/
```

- Lancer Gradle sur le Java du tableau. Les toolchains téléchargent le JDK de compilation s'il manque,
  sauf avec ForgeGradle 2.x, qui compile sur le Java qui le lance.
- `tools/collect_jars.sh --build` lance les cibles ForgeGradle 2.x sur `JAVA8_HOME` s'il est défini.
- Refined Storage vient du maven Modrinth, fixé par identifiant de version pour chaque loader. Rien
  n'est à télécharger à la main.
- `jars/` est local et ignoré par git : il garde les jars de chaque version compilée.
- La version est `mod_version` dans le `gradle.properties` de chaque cible : une release la change
  dans chaque dossier cible.

## Test automatique en jeu

`-Pselftest` compile un test automatique des vrais écrans et le lance dans un client. Une fenêtre
s'ouvre pendant deux minutes environ, un monde plat est créé, le test se joue, le jeu se ferme seul.

```bash
cd neoforge-1.21.1 && ./gradlew runClientSelftest -Pselftest      # n'importe quelle cible
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
- `-Prs_jar=<chemin>` avec `-Pselftest` lance le test contre un autre jar de Refined Storage, pour vérifier
  une version minimale.
- Sur `forge-1.12`, `-Ptarget_mc=1.12.1` fait utiliser Minecraft 1.12.1 aux lancements de développement et à
  `runObfClient`. Le jar reste compilé sur 1.12 et sert aux deux. `-Pselftest -Pobf_run` lance le jar compilé,
  avec les noms de production, sur les cibles ForgeGradle 2.x.
- `-PpolymorphJar` existe aussi sur `forge-1.16.5`, avec un jar Polymorph+.
- Sur cette ligne, `-PauditMixins` ne marche qu'avec `-Pselftest` : il applique tous les mixins dès que le jeu atteint
  les points d'entrée du test, dans n'importe quel lancement, et écrit `RSP-AUDIT` au log. Un jar publié n'a pas ce code.
