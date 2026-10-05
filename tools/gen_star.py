#!/usr/bin/env python3
"""Generates Starfall's art and data (docs/superpowers/specs/2026-10-05-starfall-design.md):

- the Fallen Star (a full-bright crystal cluster model), Starstone, the Starlit Lantern (a star
  fragment caged in iron and glass), their blockstates and item models;
- the Star Fragment and Wishing Star icons;
- loot tables (the star gives fragments and Radiance Essence to a pickaxe; Starstone one fragment),
  recipes, and the pickaxe tag.

Run from the project root:  python3 tools/gen_star.py
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

GOLD = (255, 214, 120)
GOLD_DARK = (214, 160, 70)
WHITE = (255, 252, 236)
BLUE_WHITE = (226, 236, 255)
IRON = (120, 120, 128)
IRON_DARK = (78, 78, 86)


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


def star_crystal():
    """The Fallen Star's faces: white-gold crystal with bright seams and blue-white glints."""
    rng = np.random.default_rng(7100)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            color = WHITE if r < 2.5 else GOLD if r < 5.5 else GOLD_DARK
            if (x + y) % 5 == 0 or (x - y) % 7 == 0:
                color = WHITE
            if rng.random() < 0.06:
                color = BLUE_WHITE
            img[y, x] = (*color, 255)
    return Image.fromarray(img, "RGBA")


def starstone():
    rng = np.random.default_rng(7101)
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            roll = rng.random()
            color = (34, 32, 42) if roll < 0.5 else (26, 24, 32) if roll < 0.85 else (48, 44, 58)
            if rng.random() < 0.05:
                color = GOLD
            img[y, x] = (*color, 255)
    return Image.fromarray(img, "RGBA")


def lantern_cage():
    """The lantern's sides: an iron frame round glass, the star inside showing through."""
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            edge = x in (4, 11) or y in (4, 15) or (y in (1, 2, 3) and x in (6, 7, 8, 9))
            if edge and 4 <= x <= 11 and (y >= 4 or 6 <= x <= 9):
                img[y, x] = (*IRON, 255)
            elif 5 <= x <= 10 and 5 <= y <= 14:
                r = math.hypot(x - 7.5, y - 10)
                img[y, x] = (*(WHITE if r < 1.8 else GOLD if r < 3 else (250, 236, 190)), 255)
    for x in (6, 7, 8, 9):
        img[0, x] = (*IRON_DARK, 255)
    return Image.fromarray(img, "RGBA")


FRAGMENT = [
    "................",
    "................",
    ".......w........",
    ".......w........",
    "......wGw.......",
    "..www.GWG.www...",
    "...wwGWWWGww....",
    "....GWWHWWG.....",
    "...wwGWWWGww....",
    "..www.GWG.www...",
    "......wGw.......",
    ".......w........",
    ".......w........",
    "................",
    "................",
    "................",
]
WISH = [
    "................",
    ".......w........",
    "......wGw.......",
    "......GWG.......",
    "..wGGGWHWGGGw...",
    "...GWWWWWWWG....",
    "....GWWWWWG.....",
    ".....GWWWG......",
    "....GWWGWWG.....",
    "...GWG...GWG....",
    "..GG.......GG...",
    "................",
    "...b.....b...b..",
    "..bb.b..bb.b....",
    "................",
    "................",
]


def crystal_model():
    elements = []
    for lo, hi in (([5, 0, 5], [11, 10, 11]), ([3, 0, 6], [6, 6, 9]), ([10, 0, 4], [13, 7, 7]), ([7, 0, 10], [10, 5, 13])):
        faces = {side: {"texture": "#star"} for side in ("north", "east", "south", "west", "up", "down")}
        elements.append({"from": lo, "to": hi, "neoforge_data": {"block_light": 15, "sky_light": 15}, "faces": faces})
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
            "textures": {"particle": f"{NS}block/fallen_star", "star": f"{NS}block/fallen_star"}, "elements": elements}


def lantern_model():
    side = {s: {"texture": "#cage"} for s in ("north", "east", "south", "west")}
    cap = {"up": {"texture": "#iron"}, "down": {"texture": "#iron"}}
    glow = {s: {"texture": "#star"} for s in ("north", "east", "south", "west", "up", "down")}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
            "textures": {"particle": f"{NS}block/starlit_lantern", "cage": f"{NS}block/starlit_lantern",
                         "iron": "minecraft:block/iron_block", "star": f"{NS}block/fallen_star"},
            "elements": [
                {"from": [4, 0, 4], "to": [12, 12, 12], "faces": {**side, **cap}},
                {"from": [6.5, 2, 6.5], "to": [9.5, 7, 9.5], "neoforge_data": {"block_light": 15, "sky_light": 15}, "faces": glow},
                {"from": [6, 12, 6], "to": [10, 14, 10], "faces": {s: {"texture": "#iron"} for s in ("north", "east", "south", "west", "up")}},
            ]}


def block_loot(name, entries):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": entries,
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


def count(name, low, high):
    return {"type": "minecraft:item", "name": name,
            "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]}


def main():
    blocks = ASSETS / "textures/block"
    star_crystal().save(blocks / "fallen_star.png")
    starstone().save(blocks / "starstone.png")
    lantern_cage().save(blocks / "starlit_lantern.png")
    items = ASSETS / "textures/item"
    rows(FRAGMENT, {"w": (255, 236, 170), "G": GOLD, "W": WHITE, "H": BLUE_WHITE}).save(items / "star_fragment.png")
    rows(WISH, {"w": (255, 236, 170), "G": GOLD, "W": WHITE, "H": BLUE_WHITE, "b": (200, 220, 255)}).save(items / "wishing_star.png")
    models = ASSETS / "models"
    write_json(models / "block/fallen_star.json", crystal_model())
    write_json(models / "block/starstone.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}block/starstone"}})
    write_json(models / "block/starlit_lantern.json", lantern_model())
    for name in ("fallen_star", "starstone", "starlit_lantern"):
        write_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}block/{name}"}}})
        write_json(models / f"item/{name}.json", {"parent": f"{NS}block/{name}"})
    for name in ("star_fragment", "wishing_star"):
        write_json(models / f"item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    tables = DATA / "loot_table/blocks"
    write_json(tables / "fallen_star.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [count(f"{NS}star_fragment", 2, 4)], "conditions": [{"condition": "minecraft:survives_explosion"}]},
        {"rolls": 1, "entries": [count(f"{NS}radiance_essence", 1, 2)], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write_json(tables / "starstone.json", block_loot("starstone", [{"type": "minecraft:item", "name": f"{NS}star_fragment"}]))
    write_json(tables / "starlit_lantern.json", block_loot("starlit_lantern", [{"type": "minecraft:item", "name": f"{NS}starlit_lantern"}]))
    recipes = DATA / "recipe"
    write_json(recipes / "starlit_lantern.json", {
        "type": "minecraft:crafting_shaped", "category": "building", "pattern": ["FIF", "FGF"],
        "key": {"I": {"item": "minecraft:iron_ingot"}, "F": {"item": f"{NS}star_fragment"}, "G": {"item": "minecraft:glass_pane"}},
        "result": {"id": f"{NS}starlit_lantern", "count": 1}})
    write_json(recipes / "wishing_star.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}star_fragment"}, {"item": f"{NS}star_fragment"}, {"item": f"{NS}radiance_essence"}],
        "result": {"id": f"{NS}wishing_star", "count": 1}})
    pickaxe = DATA.parent / "minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads(pickaxe.read_text()) if pickaxe.exists() else {"replace": False, "values": []}
    for name in ("fallen_star", "starstone", "starlit_lantern"):
        if f"{NS}{name}" not in tag["values"]:
            tag["values"].append(f"{NS}{name}")
    write_json(pickaxe, tag)
    print("star art and data written")


if __name__ == "__main__":
    main()
