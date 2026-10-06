#!/usr/bin/env python3
"""Generates Arcane Flora (docs/superpowers/specs/2026-10-06-arcane-flora-design.md): the eight
herbs' art (16x16 pixel sprites, drawn as crosses like flowers; the Moonlily flat like a lily pad),
the Kinship effect icons, every herb's blockstate, models, loot and ground, the flower pots, and
where they grow: a random_patch for each, placed on the surface, on water, on cave floors or the
Nether's layers, and added to their biomes by NeoForge biome modifiers.

Run from the project root:  python3 tools/gen_flora.py
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

from gen_world import effect_icon

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data"
NS = "elementalarcana"

HERBS = ["emberbloom", "moonlily", "skyplume", "deepcap", "frostcap", "prismleaf", "stormthistle", "sunpetal"]
ELEMENTS = {"emberbloom": "fire", "moonlily": "water", "skyplume": "wind", "deepcap": "earth", "frostcap": "ice",
            "prismleaf": "crystal", "stormthistle": "lightning", "sunpetal": "radiance"}
COLORS = {"fire": (255, 122, 31), "water": (63, 156, 255), "wind": (143, 227, 192), "earth": (181, 137, 90),
          "ice": (158, 230, 255), "crystal": (208, 140, 255), "lightning": (255, 225, 77), "radiance": (255, 241, 184)}

# ---- the art: a character grid each, '.' clear, every other character a colour of its palette ----

STEM = {"g": 0x4C8A30, "G": 0x2E5E22, "l": 0x6AAE3C, "L": 0x8ACC50}

SPRITES = {
    # A flower of flame-tongued petals, red out to orange to a white-hot heart.
    "emberbloom": ({**STEM, "D": 0x8A1C0C, "r": 0xC8341A, "o": 0xF0701C, "y": 0xFFB830, "w": 0xFFF0A0}, [
        "................",
        ".......o........",
        "......oy....o...",
        "...o..oy...oy...",
        "...oy.ryo..ry...",
        "...ryorywooyr...",
        "....rywwwyyr....",
        "....ryywwyor....",
        ".....rryyorD....",
        "......DrrrD.....",
        ".......gG.......",
        "..ll...gG.......",
        "...lLl.gG..ll...",
        ".....llgGlLl....",
        ".......gGl......",
        ".......gG.......",
    ]),
    # Three white plumes on thin stems, nodding.
    "skyplume": ({"w": 0xF6FFFB, "c": 0xD2F2E6, "d": 0xA6D8C6, "s": 0x7A9A64, "S": 0x58784A}, [
        ".....w.....w....",
        "....wcw...wcw...",
        "....wcd..wccw...",
        "...wccd..wcd....",
        "....wcd.wcd.....",
        "....wd..wd..w...",
        ".....s..s..wcw..",
        ".....s..s..wcd..",
        ".....s..s..cd...",
        "......s.s..s....",
        "......s.s.s.....",
        "......sSsS......",
        ".......sS.......",
        ".......sS.......",
        ".......sS.......",
        ".......sS.......",
    ]),
    # Two brown cave mushrooms with glowing amber spots.
    "deepcap": ({"k": 0x3A2414, "b": 0x7A4E2A, "B": 0x9A6638, "a": 0xFFB547, "A": 0xFFE08A, "u": 0x5A3A20,
                 "s": 0xD8C8A8, "S": 0xA89878}, [
        "................",
        "................",
        "................",
        "....kkkkkk......",
        "...kBBaBBbk.....",
        "..kBAbBBbabk....",
        "..kbbbbaBbbk....",
        "..kkuuuuuukk....",
        ".....sS.........",
        ".....sS..kkkk...",
        ".....sS.kBaBbk..",
        ".....sS.kbbbAk..",
        ".....sS.kkuukk..",
        ".....sS...sS....",
        ".....sS...sS....",
        ".....sS...sS....",
    ]),
    # Pale blue mushrooms, their caps rimed white.
    "frostcap": ({"k": 0x4A7AA8, "b": 0x8EC8EE, "B": 0xBFE6FF, "w": 0xF4FBFF, "u": 0x6A9AC0,
                  "s": 0xE8F2F8, "S": 0xB8CCD8}, [
        "................",
        "................",
        ".......w........",
        "......wBw.......",
        ".....wBBbk......",
        "....wBBbbbk.....",
        "...wBbbbbbbk....",
        "...kuuuuuuuk....",
        ".......sS.......",
        "..w....sS.......",
        ".wBk...sS.......",
        "wBbbk..sS.......",
        "kuuuk..sS.......",
        "..sS...sS.......",
        "..sS...sS.......",
        "..sS...sS.......",
    ]),
    # A sprig of violet crystal leaves, glinting white.
    "prismleaf": ({"k": 0x5A2A8A, "v": 0x9A5CD8, "l": 0xC890FF, "w": 0xF4E4FF, "s": 0x5E7A4E, "S": 0x40583A}, [
        "................",
        ".......w........",
        "......wlk.......",
        "......lvk.......",
        "...w...vs..w....",
        "..wlk..s..wlk...",
        "..lvk..s..lvk...",
        "...vks.s.s.vk...",
        "....s.ss.s......",
        "..w..ss.s...w...",
        ".wlk..sss..wlk..",
        ".lvk..sS...lvk..",
        "..vkss.sS.s.k...",
        "....ss.sSs......",
        "......ssS.......",
        ".......sS.......",
    ]),
    # A thistle: a slate head on spiky leaves, sparks for its tuft.
    "stormthistle": ({"y": 0xFFE14D, "w": 0xFFFBD0, "b": 0x4A5A7A, "B": 0x6A7AA0, "g": 0x5E7A54, "G": 0x3E5638}, [
        "................",
        "....y..w..y.....",
        ".....ywyyy......",
        "...y.yywyy.y....",
        "....yywwwyy.....",
        ".....BbBbB......",
        "....bBbBbBb.....",
        "...b.bBbBb.b....",
        ".....bbbbb......",
        ".......gG.......",
        "..g....gG...g...",
        "..gg...gG..gG...",
        "...gG..gG.gG....",
        "....gGggGgG.....",
        ".......gG.......",
        ".......gG.......",
    ]),
    # A golden flower, open to the sun.
    "sunpetal": ({**STEM, "y": 0xFFD84A, "Y": 0xFFF2A0, "o": 0xE8A020, "c": 0xB86A10, "C": 0xFF9A20}, [
        "................",
        "................",
        "......Y.Y.......",
        "....Y.yYy.Y.....",
        ".....yYyYy......",
        "...YyyCCCyyY....",
        "....yYCccCYy....",
        "...YyyCCCyyY....",
        ".....yYyYy......",
        "....Y.yoy.Y.....",
        "......Yg.Y......",
        ".......gG.......",
        "..ll...gG..ll...",
        "...lLl.gG.lL....",
        ".....llgGl......",
        ".......gG.......",
    ]),
    # The same, shut for the night: a green bud, gold at its tip.
    "sunpetal_closed": ({**STEM, "y": 0xFFD84A, "o": 0xE8A020}, [
        "................",
        "................",
        "................",
        "................",
        ".......y........",
        "......yoy.......",
        "......lyl.......",
        "......lol.......",
        ".......l........",
        ".......gG.......",
        ".......gG.......",
        ".......gG.......",
        "..ll...gG..ll...",
        "...lLl.gG.lL....",
        ".....llgGl......",
        ".......gG.......",
    ]),
}

# The Moonlily's flower, drawn over its pad: open (white petals round a glowing heart), or a shut bud.
MOONLILY_OPEN = ({"w": 0xF2FAFF, "p": 0xC8E0FF, "b": 0x8CC4F4, "g": 0x9FE8FF}, [
    ".....w.....",
    "....wpw....",
    "..w.wpw.w..",
    "..wpbpbpw..",
    "...pbgbp...",
    "wwppggbppww",
    "...pbgbp...",
    "..wpbpbpw..",
    "..w.wpw.w..",
    "....wpw....",
    ".....w.....",
])
MOONLILY_BUD = ({"w": 0xE4ECFF, "p": 0xB0C0E8, "k": 0x7080B0}, [
    "...",
    ".w.",
    "wpw",
    "kpk",
    ".k.",
])


def render(palette, grid):
    height, width = len(grid), len(grid[0])
    assert all(len(row) == width for row in grid), "rows must be even"
    image = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, char in enumerate(row):
            if char != ".":
                color = palette[char]
                image.putpixel((x, y), (color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 255))
    return image


def moonlily_pad():
    """A round lily pad seen from above, blue-green, a notch cut to its heart, veins running out."""
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    rng = np.random.default_rng(4407)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            if r > 7.2:
                continue
            angle = math.degrees(math.atan2(dy, dx)) % 360
            if 32 <= angle <= 58 and r > 1.2:
                continue  # the notch
            shade = 0.85 + 0.15 * rng.random()
            base = np.array([42, 107, 102]) * shade
            if r > 6.2:
                base = np.array([30, 78, 80])  # the rim
            elif abs(round(angle / 45) * 45 - angle) < 4 and r > 2:
                base = np.array([79, 163, 148])  # a vein
            img[y, x] = [*np.clip(base, 0, 255).astype(np.uint8), 255]
    return Image.fromarray(img, "RGBA")


def moonlily(open_):
    pad = moonlily_pad()
    palette, grid = MOONLILY_OPEN if open_ else MOONLILY_BUD
    flower = render(palette, grid)
    corner = (8 - flower.width // 2 - (flower.width % 2 == 0), 8 - flower.height // 2 - (flower.height % 2 == 0))
    pad.alpha_composite(flower, corner)
    return pad


def textures():
    """Every texture by its path under textures/."""
    out = {f"block/{name}": render(*sprite) for name, sprite in SPRITES.items()}
    out["block/moonlily"] = moonlily(False)
    out["block/moonlily_open"] = moonlily(True)
    return out


# Kinship's effect icons: the herb's shape in the element's colour.
RUNES = {
    "fire": [
        "....W.....",
        "..W.WW....",
        "..WWcW.W..",
        ".WccccWW..",
        ".WcWWccW..",
        ".WcWWWcW..",
        "..WcccW...",
        "...WWW....",
        "....W.....",
        "..........",
    ],
    "water": [
        "..........",
        "...WWWW...",
        "..WccccW..",
        ".WccWcccW.",
        ".WcWWWccW.",
        ".WccWc.WW.",
        "..Wcc.W...",
        "...WWW....",
        "..........",
        "..........",
    ],
    "wind": [
        "..W....W..",
        ".WcW..WcW.",
        ".WcW..WcW.",
        "..W....W..",
        "..W....W..",
        "...W..W...",
        "....WW....",
        "....W.....",
        "....W.....",
        "..........",
    ],
    "earth": [
        "..........",
        "..WWWWWW..",
        ".WccWcccW.",
        ".WcccccWW.",
        "..WWWWWW..",
        "....WW....",
        "....WW....",
        "....WW....",
        "..........",
        "..........",
    ],
    "ice": [
        "....W.....",
        "...WcW....",
        "..WcccW...",
        ".WcccccW..",
        ".WWWWWWW..",
        "....W.....",
        "....W.....",
        "....W.....",
        "..........",
        "..........",
    ],
    "crystal": [
        "....W.....",
        "...WcW....",
        "...WcW.W..",
        ".W..W.WcW.",
        "WcW.W..W..",
        ".W..W.....",
        "....W.....",
        "....W.....",
        "..........",
        "..........",
    ],
    "lightning": [
        "..W.W.W...",
        "...WWW....",
        ".WWcccWW..",
        "..WcccW...",
        "...WWW....",
        "....W.....",
        "....W.....",
        "....W.....",
        "..........",
        "..........",
    ],
    "radiance": [
        "....W.....",
        ".W..W..W..",
        "..WWWWW...",
        "..WcccW...",
        "WWWcccWWW.",
        "..WcccW...",
        "..WWWWW...",
        ".W..W..W..",
        "....W.....",
        "..........",
    ],
}

# ---- where they grow ----

SOILS = {
    "emberbloom": ["#minecraft:sand", "#minecraft:terracotta", "#minecraft:dirt", "minecraft:netherrack", "#minecraft:nylium",
                   "minecraft:soul_soil", "minecraft:basalt", "minecraft:blackstone"],
    "moonlily": [],  # it floats on water instead (MoonlilyBlock)
    "skyplume": ["#minecraft:dirt", "minecraft:gravel", "#minecraft:base_stone_overworld"],
    "deepcap": ["#minecraft:dirt", "minecraft:gravel", "#minecraft:base_stone_overworld", "minecraft:calcite"],
    "frostcap": ["#minecraft:dirt", "minecraft:snow_block", "minecraft:packed_ice"],
    "prismleaf": ["#minecraft:base_stone_overworld", "minecraft:calcite", "minecraft:dripstone_block", "minecraft:gravel",
                  "minecraft:amethyst_block"],
    "stormthistle": ["#minecraft:dirt", "minecraft:gravel"],
    "sunpetal": ["#minecraft:dirt"],
}

# Each patch: its herb, the biome tag's members, and how it's placed.
OVERWORLD_NOT_DEEP_DARK = {"type": "neoforge:and", "values": [
    "#minecraft:is_overworld", {"type": "neoforge:not", "value": "minecraft:deep_dark"}]}
PATCHES = {
    "emberbloom": ("emberbloom", [{"id": "#c:is_desert", "required": False}, {"id": "#c:is_badlands", "required": False},
                                  {"id": "#c:is_savanna", "required": False}], "surface", 8),
    "emberbloom_nether": ("emberbloom", ["minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:basalt_deltas"],
                          "nether", 3),
    "moonlily": ("moonlily", ["minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:river"], "water", 4),
    "skyplume": ("skyplume", ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
                              "minecraft:meadow"], "surface", 7),
    "deepcap": ("deepcap", OVERWORLD_NOT_DEEP_DARK, "cave", 8),
    "frostcap": ("frostcap", [{"id": "#c:is_snowy", "required": False}], "snow", 7),
    "prismleaf": ("prismleaf", ["minecraft:stony_peaks", "minecraft:stony_shore", "minecraft:windswept_gravelly_hills"],
                  "surface", 6),
    "prismleaf_cave": ("prismleaf", ["minecraft:dripstone_caves"], "cave", 6),
    "stormthistle": ("stormthistle", ["minecraft:windswept_savanna", "minecraft:savanna_plateau", "minecraft:windswept_hills",
                                      "minecraft:windswept_forest"], "surface", 8),
    "sunpetal": ("sunpetal", ["minecraft:meadow", "minecraft:sunflower_plains", "minecraft:flower_forest",
                              "minecraft:cherry_grove"], "surface", 5),
}


def configured(herb, kind):
    """A random_patch of the herb: a dozen and a half tries in a small square, each placing it where
    the spot is free (air, or a snow layer for the Frostcap) and the herb can stand (simple_block
    checks that)."""
    state = {"Name": f"{NS}:{herb}"}
    if herb == "sunpetal":
        state["Properties"] = {"open": "true"}
    elif herb == "moonlily":
        state["Properties"] = {"open": "false"}
    free = ({"type": "minecraft:matching_blocks", "blocks": ["minecraft:air", "minecraft:snow"]} if kind == "snow"
            else {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"})
    return {"type": "minecraft:random_patch", "config": {
        "tries": 10 if kind == "water" else 18, "xz_spread": 5 if kind == "water" else 3, "y_spread": 2,
        "feature": {
            "feature": {"type": "minecraft:simple_block", "config": {
                "to_place": {"type": "minecraft:simple_state_provider", "state": state}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": free}]}}}


def placement(kind, rarity):
    """Where a patch starts: a chunk in {rarity} at the surface (or the water's), or, underground,
    on a cave floor found by scanning down from a few spots, or on each of the Nether's floors."""
    if kind in ("surface", "snow"):
        return [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]
    if kind == "water":
        return [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]
    if kind == "cave":
        return [{"type": "minecraft:count", "count": rarity}, {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                              "min_inclusive": {"above_bottom": 8},
                                                              "max_inclusive": {"absolute": 56}}},
                {"type": "minecraft:environment_scan", "direction_of_search": "down", "max_steps": 12,
                 "target_condition": {"type": "minecraft:solid"},
                 "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}},
                {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1},
                {"type": "minecraft:biome"}]
    if kind == "nether":
        return [{"type": "minecraft:count_on_every_layer", "count": 1}, {"type": "minecraft:rarity_filter", "chance": rarity},
                {"type": "minecraft:biome"}]
    raise ValueError(kind)


# ---- writing ----

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def block_models(herb):
    models = ASSETS / "models"
    tex = f"{NS}:block/{herb}"
    if herb == "moonlily":
        for name in ("moonlily", "moonlily_open"):
            write_json(models / f"block/{name}.json", {
                "ambientocclusion": False, "render_type": "minecraft:cutout",
                "textures": {"particle": f"{NS}:block/{name}", "texture": f"{NS}:block/{name}"},
                "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "faces": {
                    "down": {"uv": [16, 16, 0, 0], "texture": "#texture"},
                    "up": {"uv": [16, 0, 0, 16], "texture": "#texture"}}}]})
        variants = {}
        for open_, model in (("true", "moonlily_open"), ("false", "moonlily")):
            variants[f"open={open_}"] = [{"model": f"{NS}:block/{model}", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
        write_json(ASSETS / "blockstates/moonlily.json", {"variants": variants})
        write_json(models / "item/moonlily.json", {"parent": "minecraft:item/generated",
                                                   "textures": {"layer0": f"{NS}:block/moonlily_open"}})
        return
    names = [herb, f"{herb}_closed"] if herb == "sunpetal" else [herb]
    for name in names:
        write_json(models / f"block/{name}.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                   "textures": {"cross": f"{NS}:block/{name}"}})
    if herb == "sunpetal":
        write_json(ASSETS / "blockstates/sunpetal.json", {"variants": {
            "open=true": {"model": f"{NS}:block/sunpetal"}, "open=false": {"model": f"{NS}:block/sunpetal_closed"}}})
    else:
        write_json(ASSETS / f"blockstates/{herb}.json", {"variants": {"": {"model": f"{NS}:block/{herb}"}}})
    write_json(models / f"item/{herb}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    # Potted.
    write_json(models / f"block/potted_{herb}.json", {"parent": "minecraft:block/flower_pot_cross",
                                                      "render_type": "minecraft:cutout", "textures": {"plant": tex}})
    write_json(ASSETS / f"blockstates/potted_{herb}.json", {"variants": {"": {"model": f"{NS}:block/potted_{herb}"}}})


def loot(herb):
    tables = DATA / NS / "loot_table/blocks"
    survives = [{"condition": "minecraft:survives_explosion"}]
    write_json(tables / f"{herb}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{herb}", "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{herb}"}], "conditions": survives}]})
    if herb != "moonlily":
        write_json(tables / f"potted_{herb}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/potted_{herb}",
                                                    "pools": [
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:flower_pot"}], "conditions": survives},
            {"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{herb}"}], "conditions": survives}]})


def main():
    for path, image in textures().items():
        save(image, ASSETS / f"textures/{path}.png")
    for element, rune in RUNES.items():
        save(effect_icon(COLORS[element], rune, 2600 + len(element)), ASSETS / f"textures/mob_effect/{element}_kinship.png")
    for herb in HERBS:
        block_models(herb)
        loot(herb)
        if SOILS[herb]:
            write_json(DATA / NS / f"tags/block/herb_soil/{herb}.json", {"values": SOILS[herb]})
    write_json(DATA / NS / "tags/item/herbs.json", {"values": [f"{NS}:{herb}" for herb in HERBS]})
    write_json(DATA / "minecraft/tags/block/flower_pots.json",
               {"values": [f"{NS}:potted_{herb}" for herb in HERBS if herb != "moonlily"]})
    write_json(DATA / "neoforge/data_maps/item/compostables.json",
               {"values": {f"{NS}:{herb}": {"chance": 0.65} for herb in HERBS}})
    for patch, (herb, biomes, kind, rarity) in PATCHES.items():
        write_json(DATA / NS / f"worldgen/configured_feature/{patch}_patch.json", configured(herb, kind))
        write_json(DATA / NS / f"worldgen/placed_feature/{patch}_patch.json",
                   {"feature": f"{NS}:{patch}_patch", "placement": placement(kind, rarity)})
        if isinstance(biomes, list):
            write_json(DATA / NS / f"tags/worldgen/biome/flora/{patch}.json", {"values": biomes})
            biomes = f"#{NS}:flora/{patch}"
        write_json(DATA / NS / f"neoforge/biome_modifier/{patch}.json", {
            "type": "neoforge:add_features", "biomes": biomes, "features": f"{NS}:{patch}_patch", "step": "vegetal_decoration"})
    print("flora written")


if __name__ == "__main__":
    main()
