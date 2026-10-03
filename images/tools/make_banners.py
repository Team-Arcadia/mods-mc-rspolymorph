"""Builds the animated banners of the project page (images/*.gif) from footage recorded in game.

    python images/tools/make_banners.py [names...]

One function per banner. The stills come from the capture scripts in images/tools/capture/
(recorded on the 1.21.1 NeoForge target, frames in neoforge/run-capture/screenshots/).
A still is 1600 x 900 at GUI scale 2; a banner is 800 pixels wide, so a full-width crop keeps one
pixel of the interface as one pixel of the banner. Look at every banner with gifsheet.py before it ships.

Author: vyrriox
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
sys.path.insert(0, HERE)

import kit  # noqa: E402
from theme import Theme  # noqa: E402

SRC = os.path.join(ROOT, "neoforge-1.21.1", "run-capture", "screenshots")
OUT = os.path.dirname(HERE)             # images/, the folder this toolkit sits in
THEME = Theme.load(os.path.join(HERE, "theme.json"))
BUTTON = os.path.join(ROOT, "common", "1.21.1", "src", "main", "resources", "assets", "rspolymorph", "textures", "gui", "sprites",
                      "widget", "side_button", "polymorph.png")

PATTERN = os.path.join(HERE, "textures", "pattern.png")


def src(name):
    return os.path.join(SRC, name)


def out(name):
    return os.path.join(OUT, name)


def steps_overlay(captions, hold, right=268, y=186):
    """One caption per state of a tour, right-aligned against the screen on its free left side. Each one slides
    in with a small overshoot; the first is already in place on frame 0, the frame a visitor sees first."""
    chips = [kit.label(text, THEME, scale=2, icon_name=icon) for text, icon in captions]

    def overlay(canvas, i, n):
        k, f = divmod(i, hold)
        chip = chips[k % len(chips)]
        t = 1.0 if k == 0 else kit.ease_out_back(kit.span(f, 0, 7))
        kit.paste(canvas, chip, right - chip.width - (1 - t) * 30, y, alpha=kit.clamp(t))

    return overlay


# ------------------------------------------------------------------ the banner on top of the page

def hero():
    # Dusk view of the network: the Crafting Grid and the Pattern Grid glow on the right, the name on the left.
    kit.hero_banner(out("hero.gif"), THEME, src("hero2_b.png"), "RS POLYMORPH", logo=BUTTON,
                    crop=(200, 240, 1600, 765))


# ------------------------------------------------------------------ Crafting Grid: pick the recipe

def pick():
    hold = 30
    kit.tour_scene(out("pick.gif"), THEME, [src(f"t{k}.png") for k in range(4)],
                   targets=[(348, 268), (292, 19), (384, 268), (292, 19)],
                   crop=(0, 50, 1600, 850), size=(800, 400), keep=(270, 0, 505, 400), hold=hold,
                   overlay=steps_overlay([("3 RECIPES MATCH", "warning"), ("PICK ONE", "star"),
                                          ("THE GRID CRAFTS IT", "check"), ("CHANGE IT ANY TIME", "refresh")], hold))


# ------------------------------------------------------------------ Pattern Grid: the choice goes into the pattern

def pattern():
    hold = 30
    kit.tour_scene(out("pattern.gif"), THEME, [src(f"q{k}.png") for k in (5, 1, 2, 3, 4)],
                   targets=[(462, 280), (292, 20), (371, 255), (462, 262), (462, 280)],
                   crop=(0, 45, 1600, 845), size=(800, 400), keep=(270, 0, 600, 400), hold=hold,
                   overlay=steps_overlay([("PATTERN GRID TOO", PATTERN), ("PICK ONE", "star"), ("STICKS, NOT A LADDER", "check"),
                                          ("PRINT THE PATTERN", PATTERN), ("AUTOCRAFTING USES IT", "gear")], hold))


BUILDERS = {f.__name__: f for f in (hero, pick, pattern)}


def main():
    names = sys.argv[1:] or list(BUILDERS)
    for name in names:
        BUILDERS[name]()


if __name__ == "__main__":
    main()
