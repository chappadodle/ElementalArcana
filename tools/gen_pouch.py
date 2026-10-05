#!/usr/bin/env python3
"""Generates the Charm Pouch's art and data (docs/superpowers/specs/2026-10-05-charm-pouch-design.md):

- the pouch's icons (empty, and full with a glint of charms at its neck) and its item model, which
  picks the full one by the elementalarcana:filled property;
- the screen's texture: a vanilla-style panel (six pixels taller than a dispenser's), the nine slots
  on a stitched leather patch;
- the recipe, and the elementalarcana:charms tag (what the pouch takes: the Drakescale Charms, the
  Trophies of the Wild, the relics).

Run from the project root:  python3 tools/gen_pouch.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

DRAKE_ELEMENTS = ("fire", "water", "wind", "ice", "lightning")
WILD_CHARMS = ("heartwood_talisman", "wraithsilk_veil", "salamander_charm", "plume_of_the_gale", "crawlers_prism",
               "bog_pearl_charm")
RELICS = ("ember_heart", "tidecallers_pearl", "rimeheart_locket", "feather_of_the_gale", "stoneheart_idol",
          "prism_of_the_deep", "storm_sigil", "sunstone", "revenants_phylactery")

LEATHER = {
    "o": (58, 33, 18),     # outline
    "d": (104, 60, 32),    # shade
    "L": (138, 83, 45),    # leather
    "l": (172, 112, 64),   # light
    "h": (198, 140, 86),   # highlight
    "S": (226, 214, 184),  # string
    "s": (170, 154, 124),  # string, shaded
    "G": (252, 210, 76),   # gold
    "g": (190, 140, 40),   # gold, shaded
    "t": (214, 170, 96),   # gold-thread stitching
}
GLINTS = {"r": (255, 96, 56), "c": (110, 214, 255), "v": (110, 230, 120), "w": (255, 255, 240)}

POUCH = [
    "................",
    "................",
    ".....oo.oo.oo...",
    ".....ohoLlodo...",
    "......oLLLdo....",
    ".....sSSSSSSG...",
    ".....olLLLdog...",
    "....olhLLLLdo...",
    "...olhlLLLLLdo..",
    "..olhlLLLtLLLdo.",
    "..ollLLLtGtLLdo.",
    "..olLLLLLtLLLdo.",
    "..oLLLLLLLLLddo.",
    "...odLLLLLLddo..",
    "....oddddddoo...",
    ".....oooooo.....",
]
FULL = [
    "........w.......",
    ".......wcw......",
    ".....oorwovo....",
    ".....ohooLodo...",
    "......oLLLdo....",
    ".....sSSSSSSG...",
    ".....olLLLdog...",
    "....olhLLLLdo...",
    "...olhlLLLLLdo..",
    "..olhlLLLtLLLdo.",
    "..ollLLLtGtLLdo.",
    "..olLLLLLtLLLdo.",
    "..oLLLLLLLLLddo.",
    "...odLLLLLLddo..",
    "....oddddddoo...",
    ".....oooooo.....",
]


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def rows(grid, colors, size=16):
    img = np.zeros((size, size, 4), dtype=np.uint8)
    assert len(grid) == size and all(len(r) == size for r in grid), grid
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in colors:
                img[y, x] = (*colors[ch], 255)
    return Image.fromarray(img, "RGBA")


# The screen: vanilla's container palette.
PANEL = (198, 198, 198)
BLACK = (0, 0, 0)
WHITE = (255, 255, 255)
SHADOW = (85, 85, 85)
SLOT_DARK = (55, 55, 55)
SLOT_FILL = (139, 139, 139)


def panel(img, x0, y0, w, h):
    """Vanilla's raised panel: a black rim with cut corners, white light top-left, grey shadow bottom-right."""
    x1, y1 = x0 + w - 1, y0 + h - 1
    img[y0:y1 + 1, x0:x1 + 1] = (*PANEL, 255)
    img[y0, x0 + 3:x1 - 2] = (*BLACK, 255)
    img[y1, x0 + 3:x1 - 2] = (*BLACK, 255)
    img[y0 + 3:y1 - 2, x0] = (*BLACK, 255)
    img[y0 + 3:y1 - 2, x1] = (*BLACK, 255)
    for cx, cy, dx, dy in ((x0, y0, 1, 1), (x1, y0, -1, 1), (x0, y1, 1, -1), (x1, y1, -1, -1)):
        img[cy, cx] = (0, 0, 0, 0)
        img[cy, cx + dx] = (0, 0, 0, 0)
        img[cy + dy, cx] = (0, 0, 0, 0)
        img[cy, cx + 2 * dx] = (*BLACK, 255)
        img[cy + dy, cx + dx] = (*BLACK, 255)
        img[cy + 2 * dy, cx] = (*BLACK, 255)
    img[y0 + 1:y0 + 3, x0 + 3:x1 - 2] = (*WHITE, 255)
    img[y0 + 3:y1 - 2, x0 + 1:x0 + 3] = (*WHITE, 255)
    img[y0 + 2, x0 + 2] = (*WHITE, 255)
    img[y1 - 2:y1, x0 + 3:x1 - 2] = (*SHADOW, 255)
    img[y0 + 3:y1 - 2, x1 - 2:x1] = (*SHADOW, 255)
    img[y1 - 2, x1 - 2] = (*SHADOW, 255)


def slot(img, x, y):
    """An 18 by 18 sunken slot, its top-left corner at (x, y)."""
    img[y:y + 18, x:x + 18] = (*SLOT_FILL, 255)
    img[y, x:x + 17] = (*SLOT_DARK, 255)
    img[y:y + 17, x] = (*SLOT_DARK, 255)
    img[y + 17, x + 1:x + 18] = (*WHITE, 255)
    img[y + 1:y + 18, x + 17] = (*WHITE, 255)


def leather_patch(img, x0, y0, w, h):
    """The leather the nine slots sit on: grained hide, a darker rim, gold-thread stitches, brass studs."""
    rng = np.random.default_rng(3907)
    x1, y1 = x0 + w - 1, y0 + h - 1
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            if (x in (x0, x1) and y in (y0, y1)):
                continue
            edge = x in (x0, x1) or y in (y0, y1)
            base = LEATHER["o"] if edge else LEATHER["L"]
            if not edge:
                roll = rng.random()
                base = LEATHER["l"] if roll < 0.12 else LEATHER["d"] if roll < 0.24 else base
                if x in (x0 + 1, ) or y in (y0 + 1, ):
                    base = LEATHER["h"] if rng.random() < 0.5 else LEATHER["l"]
                if x == x1 - 1 or y == y1 - 1:
                    base = LEATHER["d"]
            img[y, x] = (*base, 255)
    for x in range(x0 + 4, x1 - 3):
        if (x - x0) % 3 != 0:
            img[y0 + 2, x] = (*LEATHER["t"], 255)
            img[y1 - 2, x] = (*LEATHER["t"], 255)
    for y in range(y0 + 4, y1 - 3):
        if (y - y0) % 3 != 0:
            img[y, x0 + 2] = (*LEATHER["t"], 255)
            img[y, x1 - 2] = (*LEATHER["t"], 255)
    for cx, cy in ((x0 + 2, y0 + 2), (x1 - 2, y0 + 2), (x0 + 2, y1 - 2), (x1 - 2, y1 - 2)):
        img[cy, cx] = (*LEATHER["G"], 255)


def screen():
    img = np.zeros((256, 256, 4), dtype=np.uint8)
    panel(img, 0, 0, 176, 172)
    leather_patch(img, 57, 17, 62, 62)
    for row in range(3):
        for col in range(3):
            slot(img, 61 + col * 18, 21 + row * 18)
    for row in range(3):
        for col in range(9):
            slot(img, 7 + col * 18, 89 + row * 18)
    for col in range(9):
        slot(img, 7 + col * 18, 147)
    return Image.fromarray(img, "RGBA")


def main():
    items = ASSETS / "textures/item"
    rows(POUCH, LEATHER).save(items / "charm_pouch.png")
    rows(FULL, {**LEATHER, **GLINTS}).save(items / "charm_pouch_filled.png")
    (ASSETS / "textures/gui").mkdir(parents=True, exist_ok=True)
    screen().save(ASSETS / "textures/gui/charm_pouch.png")
    models = ASSETS / "models/item"
    write_json(models / "charm_pouch.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/charm_pouch"},
        "overrides": [{"predicate": {f"{NS}filled": 1}, "model": f"{NS}item/charm_pouch_filled"}]})
    write_json(models / "charm_pouch_filled.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/charm_pouch_filled"}})
    write_json(DATA / "recipe/charm_pouch.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" S ", "LGL", "LLL"],
        "key": {"S": {"item": "minecraft:string"}, "L": {"item": "minecraft:leather"}, "G": {"item": "minecraft:gold_ingot"}},
        "result": {"id": f"{NS}charm_pouch", "count": 1}})
    charms = ([f"{NS}{e}_drakescale_charm" for e in DRAKE_ELEMENTS] + [NS + name for name in WILD_CHARMS]
              + [NS + name for name in RELICS])
    write_json(DATA / "tags/item/charms.json", {"replace": False, "values": charms})
    print("pouch art and data written")


if __name__ == "__main__":
    main()
