# Page images

Everything the project page (`CURSEFORGE_PAGE.md`) shows lives in `images/`, and everything that
generates it lives here, in `images/tools/`.

| Folder | Holds |
|---|---|
| `images/` | Animated banners, title ribbons, badges, language labels, the divider |
| `images/icons/` | The small icons shown next to the features |
| `images/screenshots/` | Pictures for the CurseForge gallery (uploaded by hand) |
| `images/preview/` | Pictures of the page as CurseForge will show it (not committed) |
| `images/tools/` | The scripts and the files below |

| File | Role |
|---|---|
| `page.toml` | The page itself: links, badges, sections, texts in every language |
| `theme.json` | The colors, the ornament and the shape shared by every image |
| `make_banners.py` | The animated banners, made from footage recorded in game |
| `capture/` | The scripts played by the camera to record that footage |
| `build_page.py` | Writes `CURSEFORGE_PAGE.md` from `page.toml` |
| `titles.py`, `export_icons.py`, `shots.py` | Title ribbons, badges, language labels, divider, feature icons, gallery pictures |
| `check_page.py`, `preview.py` | Check the page and show it the way CurseForge will |
| `kit.py`, `iso.py`, `pixelfont.py`, `icons.py`, `theme.py`, `gifsheet.py`, `heat.py` | The drawing, animation and encoding library behind the others |

Requirements: Python 3.11 or newer with Pillow and numpy, and ffmpeg on the PATH for the banners.

## Rebuilding

```
python images/tools/export_icons.py images/tools/page.toml     # images/icons
python images/tools/titles.py images/tools/page.toml           # images/title-*.gif, lang-*.gif, divider.gif, badge-*.png
python images/tools/make_banners.py                            # images/*.gif (needs the footage)
python images/tools/build_page.py images/tools/page.toml       # CURSEFORGE_PAGE.md
python images/tools/check_page.py CURSEFORGE_PAGE.md
python images/tools/preview.py CURSEFORGE_PAGE.md
```

To change a text, edit `page.toml` and run `build_page.py`. Then paste the new `CURSEFORGE_PAGE.md`
into the description of the project on CurseForge.

## Why the page looks the way it does

CurseForge filters the HTML of a description: it removes `align="center"` and most style
properties, and shows headings as small plain text. It keeps `text-align`, `color`, `font-size`,
`font-weight`, `display`, `margin-left`, `margin-right` and the width of images. So the page is
centered by wrappers that carry `text-align`, section titles are images, and feature names are
colored with `span` tags. `preview.py` imitates that filter and CurseForge's dark stylesheet.

Every image weighs less than 2 MB, the limit of CurseForge for an uploaded image, and banners are
800 pixels wide, the width of its description column.

## Footage

The banners and the gallery pictures were recorded on the 1.21.1 NeoForge target, in a single player
world, with Iris, Sodium and Complementary Unbound in `neoforge-1.21.1/run-capture/` (not committed). The
camera (`capture/CaptureStudio.java`, with a `quickmove` command added for this mod to shift-click
items into the Refined Storage network) is copied into `neoforge-1.21.1/src/main/java` for the time of a
recording with the capture tool, with a temporary `capture` run in `neoforge-1.21.1/build.gradle`, and
never committed. The scripts that were played are in `capture/`, in order:

| Script | Records |
|---|---|
| `01-scout.txt` | Creates the world and finds a flat spot |
| `02-build.txt` | Builds the network (creative controller, storage, Crafting Grid, Pattern Grid) |
| `03-crafting.txt` | Crafting Grid states, the warning tooltip |
| `05-crafting-tour.txt` | The stills of `pick.gif`, then the first Pattern Grid states |
| `06-pattern-hero.txt` | The stills of `pattern.gif` and the daylight view |
| `07-hero-fr.txt` | The dusk view of `hero.gif` and the French gallery pictures |

The recipe conflict shown (two oak planks: sticks, a bowl or a ladder) is staged: vanilla only has
the sticks, and `capture/datapack/rsp_conflicts` adds the two others, as a modpack would. Copy it
into `neoforge-1.21.1/run-capture/saves/rsp_page/datapacks/` after `01-scout.txt` has created the world.
The frames themselves are not in the repository: they stay in `neoforge-1.21.1/run-capture/screenshots/`.

## Credits

Author: vyrriox

---

# Images de la page (Version Française)

Tout ce que montre la page du projet (`CURSEFORGE_PAGE.md`) se trouve dans `images/`, et tout ce
qui le génère se trouve ici, dans `images/tools/`.

| Dossier | Contenu |
|---|---|
| `images/` | Bannières animées, rubans de titre, badges, étiquettes de langue, séparateur |
| `images/icons/` | Les petites icônes affichées à côté des fonctionnalités |
| `images/screenshots/` | Les images pour la galerie CurseForge (à envoyer à la main) |
| `images/preview/` | Des images de la page telle que CurseForge l'affichera (non versionnées) |
| `images/tools/` | Les scripts et les fichiers ci-dessous |

| Fichier | Rôle |
|---|---|
| `page.toml` | La page elle-même : liens, badges, sections, textes dans chaque langue |
| `theme.json` | Les couleurs, l'ornement et la forme communs à toutes les images |
| `make_banners.py` | Les bannières animées, faites à partir de séquences enregistrées en jeu |
| `capture/` | Les scripts joués par la caméra pour enregistrer ces séquences |
| `build_page.py` | Écrit `CURSEFORGE_PAGE.md` à partir de `page.toml` |
| `titles.py`, `export_icons.py`, `shots.py` | Rubans de titre, badges, étiquettes de langue, séparateur, icônes, images de galerie |
| `check_page.py`, `preview.py` | Vérifient la page et la montrent comme CurseForge l'affichera |
| `kit.py`, `iso.py`, `pixelfont.py`, `icons.py`, `theme.py`, `gifsheet.py`, `heat.py` | La bibliothèque de dessin, d'animation et d'encodage utilisée par les autres |

Prérequis : Python 3.11 ou plus récent avec Pillow et numpy, et ffmpeg dans le PATH pour les bannières.

## Reconstruire

```
python images/tools/export_icons.py images/tools/page.toml     # images/icons
python images/tools/titles.py images/tools/page.toml           # images/title-*.gif, lang-*.gif, divider.gif, badge-*.png
python images/tools/make_banners.py                            # images/*.gif (demande les séquences)
python images/tools/build_page.py images/tools/page.toml       # CURSEFORGE_PAGE.md
python images/tools/check_page.py CURSEFORGE_PAGE.md
python images/tools/preview.py CURSEFORGE_PAGE.md
```

Pour changer un texte, modifiez `page.toml` puis lancez `build_page.py`. Collez ensuite le nouveau
`CURSEFORGE_PAGE.md` dans la description du projet sur CurseForge.

## Pourquoi la page est faite ainsi

CurseForge filtre le HTML d'une description : il retire `align="center"` et la plupart des
propriétés de style, et affiche les titres comme du petit texte ordinaire. Il conserve
`text-align`, `color`, `font-size`, `font-weight`, `display`, `margin-left`, `margin-right` et la
largeur des images. La page est donc centrée par des blocs qui portent `text-align`, les titres de
section sont des images, et les noms des fonctionnalités sont colorés avec des balises `span`.
`preview.py` imite ce filtre et la feuille de style sombre de CurseForge.

Chaque image pèse moins de 2 Mo, la limite de CurseForge pour une image envoyée, et les bannières
font 800 pixels de large, la largeur de la colonne de description.

## Séquences

Les bannières et les images de galerie ont été enregistrées sur la cible 1.21.1 NeoForge, dans un
monde solo, avec Iris, Sodium et Complementary Unbound dans `neoforge-1.21.1/run-capture/` (non versionné).
La caméra (`capture/CaptureStudio.java`, avec une commande `quickmove` ajoutée pour ce mod afin de
verser les objets dans le réseau Refined Storage par shift-clic) est copiée dans
`neoforge-1.21.1/src/main/java` le temps d'un enregistrement avec l'outil de capture, avec une run
`capture` temporaire dans `neoforge-1.21.1/build.gradle`, et n'est jamais versionnée. Les scripts joués
sont dans `capture/`, dans l'ordre (voir le tableau de la section anglaise).

Le conflit de recettes montré (deux planches de chêne : bâtons, bol ou échelle) est mis en scène :
le jeu de base n'a que les bâtons, et `capture/datapack/rsp_conflicts` ajoute les deux autres,
comme le ferait un modpack. Copiez-le dans `neoforge-1.21.1/run-capture/saves/rsp_page/datapacks/` après
que `01-scout.txt` a créé le monde. Les images enregistrées ne sont pas dans le dépôt : elles restent
dans `neoforge-1.21.1/run-capture/screenshots/`.

## Credits

Author: vyrriox
