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
