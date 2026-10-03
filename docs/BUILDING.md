# Building

The repository is one Gradle build (MultiLoader template) for every target.

| Folder | Gradle project | Produces |
|--------|----------------|----------|
| `common/1.21.1` | `:common` | Shared code for 1.21.1, no jar of its own |
| `common/26.1.2` | `:common-261` | Shared code for 26.1.2, no jar of its own |
| `neoforge-1.21.1` | `:neoforge` | `neoforge-1.21.1/build/libs/rspolymorph-neoforge-1.21.1-<version>.jar` |
| `fabric-1.21.1` | `:fabric` | `fabric-1.21.1/build/libs/rspolymorph-fabric-1.21.1-<version>.jar` |
| `neoforge-26.1.2` | `:neoforge-261` | `neoforge-26.1.2/build/libs/rspolymorph-neoforge-26.1.2-<version>.jar` |
| `fabric-26.1.2` | not enabled | Waiting for a stable Fabric Loom with Minecraft 26.x support |

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
./gradlew build                                   # every enabled target
./gradlew :neoforge:build :fabric:build           # the 1.21.1 jars only
./gradlew :neoforge-261:build -Dorg.gradle.java.home=<path to a JDK 25>   # the 26.1.2 jar
./gradlew :neoforge:runClient                     # a 1.21.1 NeoForge client in neoforge-1.21.1/run
```

- Java 21 for 1.21.1, Java 25 for 26.1.2. Gradle toolchains fetch what is missing.
- Refined Storage and Quartz Arsenal are compiled against the jars in `libs/`, tracked so that CI
  can build without a private repository.
- The Gradle project names are short on purpose: they name the jars. `settings.gradle` maps each
  project to its folder.

---

# Compilation

Le dépôt est un seul build Gradle (modèle MultiLoader) pour toutes les cibles.

| Dossier | Projet Gradle | Produit |
|---------|---------------|---------|
| `common/1.21.1` | `:common` | Code partagé 1.21.1, pas de jar propre |
| `common/26.1.2` | `:common-261` | Code partagé 26.1.2, pas de jar propre |
| `neoforge-1.21.1` | `:neoforge` | `neoforge-1.21.1/build/libs/rspolymorph-neoforge-1.21.1-<version>.jar` |
| `fabric-1.21.1` | `:fabric` | `fabric-1.21.1/build/libs/rspolymorph-fabric-1.21.1-<version>.jar` |
| `neoforge-26.1.2` | `:neoforge-261` | `neoforge-26.1.2/build/libs/rspolymorph-neoforge-26.1.2-<version>.jar` |
| `fabric-26.1.2` | non activé | En attente d'une version stable de Fabric Loom compatible Minecraft 26.x |

```bash
git clone https://github.com/Team-Arcadia/mods-mc-rspolymorph.git
cd mods-mc-rspolymorph
./gradlew build                                   # toutes les cibles activées
./gradlew :neoforge:build :fabric:build           # les jars 1.21.1 seulement
./gradlew :neoforge-261:build -Dorg.gradle.java.home=<chemin d'un JDK 25>   # le jar 26.1.2
./gradlew :neoforge:runClient                     # un client NeoForge 1.21.1 dans neoforge-1.21.1/run
```

- Java 21 pour la 1.21.1, Java 25 pour la 26.1.2. Les toolchains Gradle téléchargent ce qui manque.
- Refined Storage et Quartz Arsenal sont compilés contre les jars de `libs/`, versionnés pour que la
  CI puisse compiler sans dépôt privé.
- Les noms de projets Gradle sont courts exprès : ils donnent leur nom aux jars. `settings.gradle`
  associe chaque projet à son dossier.
