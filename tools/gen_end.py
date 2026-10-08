#!/usr/bin/env python3
"""Generates the Far Isles (docs/superpowers/specs/2026-10-08-the-far-isles-design.md):

- the Stargazer's robe and its glow (its face, and the stars in its robe);
- Stardust, the Voidwalker's Charm and the Astral Chart: icons and models; the spawn egg's model;
- the Star Lens's four faces (a constellation each, in its colour) and its models and states; the
  Astral Orrery's model (vanilla copper and amethyst; sea lantern once awake) and states;
- their loot (the Stargazer's, the observatory vault's), the charm's recipe, the charm tag;
- the observatory's worldgen (structure, set, biomes), the compass's tag and the Stargazer's lands.

Run from the project root:  python3 tools/gen_end.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

ROBE = (36, 24, 56)
ROBE_DARK = (22, 14, 36)
TRIM = (150, 122, 214)
STAR = (220, 218, 255)
FACE = (232, 226, 255)
FACE_EDGE = (176, 160, 236)
EYE = (36, 18, 70)

# The constellations, in FarIslesRules order, with the colours of ObservatoryPiece's floor tiles.
CONSTELLATIONS = {
    "flame": ((255, 142, 40), [(7, 2), (5, 5), (9, 5), (4, 9), (7, 8), (10, 9)]),
    "wave": ((96, 196, 255), [(2, 7), (4, 5), (6, 7), (8, 5), (10, 7), (12, 5)]),
    "gale": ((240, 240, 255), [(7, 7), (9, 6), (9, 9), (5, 10), (4, 5), (8, 3)]),
    "stone": ((126, 224, 92), [(4, 4), (10, 4), (4, 10), (10, 10), (7, 7)]),
}


def box_faces(u, v, w, h, d):
    """A model box's six faces in its texture: (x0, y0, x1, y1): top, bottom, right, front, left, back."""
    return [(u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d), (u, v + d, u + d, v + d + h),
            (u + d, v + d, u + d + w, v + d + h), (u + d + w, v + d, u + 2 * d + w, v + d + h),
            (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)]


# The Stargazer's boxes, as client/StargazerModel lays them out: (u, v, width, height, depth).
HEAD = (0, 0, 8, 8, 8)
BODY = (0, 16, 9, 26, 5)
HEM = (28, 16, 11, 12, 7)
ARM = (28, 35, 3, 22, 3)


def stargazer(seed):
    rng = np.random.default_rng(seed)
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    glow = np.zeros((64, 64, 4), dtype=np.uint8)
    for box in (HEAD, BODY, HEM, ARM):
        for (x0, y0, x1, y1) in box_faces(*box):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    base = ROBE_DARK if rng.random() < 0.35 else ROBE
                    jitter = int(rng.integers(-4, 5))
                    img[y, x] = (*[max(0, min(255, c + jitter)) for c in base], 255)
                    if box is not HEAD and rng.random() < 0.035:
                        img[y, x] = (*STAR, 255)
                        glow[y, x] = (*STAR, 255)
    # The hem's lowest rows and the cuffs: a trim of pale violet.
    for (x0, y0, x1, y1) in box_faces(*HEM)[2:]:
        img[y1 - 1, x0:x1] = (*TRIM, 255)
    for (x0, y0, x1, y1) in box_faces(*ARM)[2:]:
        img[y1 - 2:y1, x0:x1] = (*TRIM, 255)
    # The face: starlight inside the hood's rim, two dark eyes.
    fx0, fy0, fx1, fy1 = box_faces(*HEAD)[3]
    for y in range(fy0 + 1, fy1):
        for x in range(fx0 + 1, fx1 - 1):
            edge = y == fy0 + 1 or x in (fx0 + 1, fx1 - 2)
            color = FACE_EDGE if edge else FACE
            img[y, x] = (*color, 255)
            glow[y, x] = (*color, 255)
    for ex in (fx0 + 2, fx0 + 5):
        for ey in (fy0 + 3, fy0 + 4):
            img[ey, ex] = (*EYE, 255)
            glow[ey, ex] = (0, 0, 0, 0)
    return Image.fromarray(img, "RGBA"), Image.fromarray(glow, "RGBA")


def icon(rows, palette):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            color = palette[key]
            if color is not None:
                image.putpixel((x, y), color + (255,))
    return image


ICONS = {
    ".": None, "w": (246, 244, 255), "l": (196, 184, 250), "v": (132, 104, 214), "d": (64, 40, 110), "k": (24, 14, 40),
    "p": (40, 120, 110), "P": (90, 200, 180), "c": (120, 110, 130), "g": (226, 186, 74), "b": (236, 222, 186),
    "B": (204, 184, 140), "s": (120, 96, 60),
}
STARDUST = [
    "................",
    "................",
    ".........w......",
    "........lwl.....",
    ".........w......",
    "....w...........",
    "...lwl....l.....",
    "....w....lwl....",
    "..........l.....",
    "......v.........",
    "....vlvlv.......",
    "...vlvwvlvl.....",
    "..dvlvlvlvlvd...",
    "..ddvdvdvdvdd...",
    "................",
    "................",
]
VOIDWALKER = [
    "......cccc......",
    ".....c....c.....",
    "....c......c....",
    ".....c....c.....",
    "......dkkd......",
    ".....dkvvkd.....",
    "....dkvlPvkd....",
    "....kvlPpPvk....",
    "....kvPpPpvk....",
    "....dkvPpvkd....",
    ".....dkvvkd.....",
    "......dkkd......",
    ".......ww.......",
    "........w.......",
    "................",
    "................",
]
ASTRAL_CHART = [
    "................",
    "..ssbbbbbbbbss..",
    "..sbBbbbbbbBbs..",
    "..sbbbbwbbbbbs..",
    "..sbbbbbbbbbbs..",
    "..sbwbbbbbvbbs..",
    "..sbbbbbbbbbbs..",
    "..sbbbbvbbbbbs..",
    "..sbbbbbbbbwbs..",
    "..sbbwbbbbbbbs..",
    "..sbbbbbbbbbbs..",
    "..sbbbbbbvbbbs..",
    "..sbBbbbbbbBbs..",
    "..ssbbbbbbbbss..",
    "................",
    "................",
]


def lens(name):
    """A Star Lens's face: a purpur frame round a dark glass, its constellation's stars in its colour."""
    color, stars = CONSTELLATIONS[name]
    img = Image.new("RGBA", (16, 16))
    rng = np.random.default_rng(len(name))
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.2 or x in (0, 15) or y in (0, 15):
                tone = int(rng.integers(-8, 9))
                img.putpixel((x, y), (168 + tone, 120 + tone, 168 + tone, 255))
            elif d > 6.2:
                img.putpixel((x, y), (196, 150, 98, 255))
            else:
                tone = int(rng.integers(0, 6))
                img.putpixel((x, y), (20 + tone, 14 + tone, 38 + tone, 255))
    dim = tuple(int(c * 0.45) for c in color)
    for i, (x, y) in enumerate(stars):
        img.putpixel((x + 1, y + 1), color + (255,))
        if i + 1 < len(stars):
            # A faint line on to the next star.
            nx, ny = stars[i + 1]
            for t in (0.33, 0.66):
                lx, ly = round(x + (nx - x) * t) + 1, round(y + (ny - y) * t) + 1
                if img.getpixel((lx, ly))[:3] != color:
                    img.putpixel((lx, ly), dim + (255,))
    return img


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main():
    entity = ASSETS / "textures/entity/end"
    entity.mkdir(parents=True, exist_ok=True)
    skin, glow = stargazer(7100)
    skin.save(entity / "stargazer.png")
    glow.save(entity / "stargazer_glow.png")

    models = ASSETS / "models"
    for name, rows in (("stardust", STARDUST), ("voidwalker_charm", VOIDWALKER), ("astral_chart", ASTRAL_CHART)):
        icon(rows, ICONS).save(ASSETS / f"textures/item/{name}.png")
        write_json(models / f"item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    write_json(models / "item/stargazer_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

    # The Star Lens: four faces, a model each, turned by its facing.
    block_textures = ASSETS / "textures/block"
    variants = {}
    for index, name in enumerate(CONSTELLATIONS):
        lens(name).save(block_textures / f"star_lens_{name}.png")
        write_json(models / f"block/star_lens_{name}.json", {"parent": "minecraft:block/orientable", "textures": {
            "front": f"{NS}block/star_lens_{name}", "side": "minecraft:block/purpur_block", "top": "minecraft:block/purpur_pillar_top"}})
        for facing, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            variant = {"model": f"{NS}block/star_lens_{name}"}
            if rotation:
                variant["y"] = rotation
            variants[f"constellation={index},facing={facing}"] = variant
    write_json(ASSETS / "blockstates/star_lens.json", {"variants": variants})
    write_json(models / "item/star_lens.json", {"parent": f"{NS}block/star_lens_flame"})

    # The Astral Orrery: a copper stand under a sphere of amethyst (a sea lantern's light, awake).
    def orrery(sphere):
        return {"parent": "minecraft:block/block", "textures": {"particle": "minecraft:block/cut_copper",
                "copper": "minecraft:block/cut_copper", "sphere": sphere}, "elements": [
            {"from": [2, 0, 2], "to": [14, 3, 14], "faces": {f: {"texture": "#copper"} for f in ("down", "up", "north", "south", "west", "east")}},
            {"from": [6, 3, 6], "to": [10, 8, 10], "faces": {f: {"texture": "#copper"} for f in ("north", "south", "west", "east")}},
            {"from": [3, 8, 3], "to": [13, 16, 13], "faces": {f: {"texture": "#sphere"} for f in ("down", "up", "north", "south", "west", "east")}},
        ]}
    write_json(models / "block/astral_orrery.json", orrery("minecraft:block/amethyst_block"))
    write_json(models / "block/astral_orrery_awake.json", orrery("minecraft:block/sea_lantern"))
    write_json(ASSETS / "blockstates/astral_orrery.json", {"variants": {
        "awake=false": {"model": f"{NS}block/astral_orrery"}, "awake=true": {"model": f"{NS}block/astral_orrery_awake"}}})
    write_json(models / "item/astral_orrery.json", {"parent": f"{NS}block/astral_orrery"})

    # Loot.
    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    write_json(DATA / "loot_table/entities/stargazer.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "stardust", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}, looting]}],
         "conditions": [{"condition": "minecraft:killed_by_player"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "astral_chart"}],
         "conditions": [{"condition": "minecraft:killed_by_player"},
                        {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                         "unenchanted_chance": 0.02, "enchanted_chance": {"type": "minecraft:linear", "base": 0.03, "per_level_above_first": 0.01}}]}]})

    def count(low, high):
        return [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}}]
    write_json(DATA / "loot_table/chests/observatory.json", {"type": "minecraft:chest", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "tome_of_insight", "functions": count(2, 2)}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "wishing_star"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "astral_chart"}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "stardust", "functions": count(3, 6)}]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 3}, "entries": [
            {"type": "minecraft:item", "name": "minecraft:ender_pearl", "weight": 3, "functions": count(2, 6)},
            {"type": "minecraft:item", "name": "minecraft:amethyst_shard", "weight": 3, "functions": count(4, 10)},
            {"type": "minecraft:item", "name": "minecraft:chorus_fruit", "weight": 2, "functions": count(3, 8)},
            {"type": "minecraft:item", "name": "minecraft:experience_bottle", "weight": 2, "functions": count(3, 8)},
            {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1, "functions": count(1, 3)}]}]})

    write_json(DATA / "recipe/voidwalker_charm.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["S S", " P ", "SCS"],
        "key": {"S": {"item": NS + "stardust"}, "P": {"item": "minecraft:ender_pearl"}, "C": {"item": "minecraft:chorus_fruit"}},
        "result": {"id": NS + "voidwalker_charm", "count": 1}})
    charms = DATA / "tags/item/charms.json"
    tag = json.loads(charms.read_text())
    if NS + "voidwalker_charm" not in tag["values"]:
        tag["values"].append(NS + "voidwalker_charm")
    write_json(charms, tag)

    # Worldgen: the observatories on the outer islands' highlands, kept clear of the End cities.
    write_json(DATA / "worldgen/structure/observatory.json", {
        "type": NS + "observatory", "biomes": "#elementalarcana:has_structure/observatories", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/observatories.json", {
        "structures": [{"structure": NS + "observatory", "weight": 1}],
        "placement": {"type": NS + "spread_away", "spacing": 24, "separation": 8, "salt": 488213,
                      "keep_away": {"sets": ["minecraft:end_cities"], "chunk_count": 4}}})
    write_json(DATA / "tags/worldgen/biome/has_structure/observatories.json", {"values": ["minecraft:end_highlands", "minecraft:end_midlands"]})
    write_json(DATA / "tags/worldgen/structure/seekable/observatories.json", {"values": [NS + "observatory"]})
    write_json(DATA / "tags/worldgen/biome/wild/stargazer.json", {"values": [
        "minecraft:end_highlands", "minecraft:end_midlands", "minecraft:end_barrens", "minecraft:small_end_islands"]})
    print("the far isles' art and data written")


if __name__ == "__main__":
    main()
