#!/usr/bin/env python3
"""Generates the Creatures of the Nether (docs/superpowers/specs/2026-10-07-nether-creatures-design.md):

- the Ash Wraith's skin and eyes (the Frost Wraith's, gen_wild, in ash-grey with soul-blue eyes);
- the Cinder Hound's hide (charcoal, cracked with glowing seams) and its glow;
- Soul Ash, the Cinder Fang, the Ashen Shroud and the Houndstooth Charm: icons and models; the spawn
  eggs' models;
- their loot, the trophies' recipes and the lands they come to.

Run from the project root:  python3 tools/gen_nether.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

import gen_wild as wild

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

SOUL = (80, 226, 255)
HIDE = (50, 44, 46)
HIDE_DARK = (32, 28, 30)
SEAM = (255, 122, 32)
SEAM_HOT = (255, 196, 80)

ICON_PALETTE = {
    ".": None,
    "a": (96, 94, 100), "A": (140, 138, 146), "s": (60, 170, 210), "S": (120, 236, 255), "k": (44, 40, 46),
    "f": (220, 210, 190), "F": (250, 244, 226), "e": (255, 122, 32), "c": (110, 110, 120), "g": (150, 104, 30), "G": (226, 186, 74),
    "w": (70, 68, 76), "W": (112, 110, 120),
}
SOUL_ASH = [
    "................",
    "................",
    "................",
    ".......S........",
    "......SsS.......",
    "......sSs.......",
    ".......s........",
    "................",
    ".....aAAAa......",
    "...aAAaAAAAa....",
    "..aAaAAAaAAAa...",
    ".aAAAAaAAAaAAa..",
    ".kaaaaaaaaaaak..",
    "..kkkkkkkkkkk...",
    "................",
    "................",
]
CINDER_FANG = [
    "................",
    "................",
    "...ff...........",
    "...fFf..........",
    "....fFf.........",
    "....fFFf........",
    ".....fFFf.......",
    ".....fFFFf......",
    "......fFFFf.....",
    "......fFFFFf....",
    ".......fFFFFf...",
    ".......kkkkk....",
    "........eee.....",
    "................",
    "................",
    "................",
]
ASHEN_SHROUD = [
    "................",
    ".....wwwwww.....",
    "....wWWWWWWw....",
    "...wWWkkkkWWw...",
    "...wWkkkkkkWw...",
    "...wWkSkkSkWw...",
    "...wWkkkkkkWw...",
    "...wWWkkkkWWw...",
    "..wWWWWWWWWWWw..",
    "..wWWwWWWwWWWw..",
    ".wWWWWWWWWWWWWw.",
    ".wWwWWWwWWWwWWw.",
    ".w.wWw.wWw.wWw..",
    "...w...w...w....",
    "................",
    "................",
]
HOUNDSTOOTH = [
    "......cccc......",
    ".....c....c.....",
    "....c......c....",
    ".....c....c.....",
    "......gGGg......",
    ".......ff.......",
    "......fFFf......",
    "......fFFf......",
    "......fFFf......",
    ".......fFf......",
    ".......fF.......",
    "........e.......",
    "................",
    "................",
    "................",
    "................",
]


def icon(rows):
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, key in enumerate(row):
            color = ICON_PALETTE[key]
            if color is not None:
                image.putpixel((x, y), color + (255,))
    return image


def ash_wraith():
    """The Frost Wraith's skin and eyes, in ash and soul fire (gen_wild draws them with its own colours: lent ours)."""
    saved = (wild.CLOTH, wild.CLOTH_FOLD, wild.WRAITH_EYE)
    wild.CLOTH, wild.CLOTH_FOLD, wild.WRAITH_EYE = (104, 100, 108), (66, 62, 70), SOUL
    try:
        return wild.wraith(), wild.wraith_eyes()
    finally:
        wild.CLOTH, wild.CLOTH_FOLD, wild.WRAITH_EYE = saved


def box_faces(u, v, w, h, d):
    """A model box's six faces in its texture: (x0, y0, x1, y1)."""
    return [(u + d, v, u + d + w, v + d), (u + d + w, v, u + d + 2 * w, v + d), (u, v + d, u + d, v + d + h),
            (u + d, v + d, u + d + w, v + d + h), (u + d + w, v + d, u + 2 * d + w, v + d + h),
            (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)]


# The hound's boxes, as client/CinderHoundModel lays them out: (u, v, width, height, depth).
HOUND_BOXES = [(0, 0, 6, 6, 14), (40, 0, 2, 2, 10), (0, 20, 6, 6, 5), (22, 20, 3, 3, 4), (36, 20, 2, 2, 1), (42, 12, 2, 10, 2),
               (50, 12, 2, 8, 2)]


def hound(seed):
    rng = np.random.default_rng(seed)
    img = np.zeros((32, 64, 4), dtype=np.uint8)
    glow = np.zeros((32, 64, 4), dtype=np.uint8)
    glow[..., 3] = 255
    for (u, v, w, h, d) in HOUND_BOXES:
        for (x0, y0, x1, y1) in box_faces(u, v, w, h, d):
            for y in range(y0, y1):
                for x in range(x0, x1):
                    base = HIDE_DARK if rng.random() < 0.3 else HIDE
                    jitter = int(rng.integers(-6, 7))
                    img[y, x] = (*[max(0, min(255, c + jitter)) for c in base], 255)
            # A seam of cinders across the face, now and then.
            if x1 - x0 >= 3 and y1 - y0 >= 3 and rng.random() < 0.55:
                cx, cy = int(rng.integers(x0, x1)), int(rng.integers(y0, y1))
                for _ in range(int(rng.integers(2, 2 + max(x1 - x0, y1 - y0) // 2))):
                    color = SEAM_HOT if rng.random() < 0.3 else SEAM
                    img[cy, cx] = (*color, 255)
                    glow[cy, cx] = (*color, 255)
                    cx = max(x0, min(x1 - 1, cx + int(rng.integers(-1, 2))))
                    cy = max(y0, min(y1 - 1, cy + int(rng.integers(0, 2))))
    # The back's ridge glows along its top.
    for (x0, y0, x1, y1) in box_faces(40, 0, 2, 2, 10)[:1]:
        img[y0:y1, x0:x1] = (*SEAM, 255)
        glow[y0:y1, x0:x1] = (*SEAM, 255)
    # Eyes like coals on the head's front, the mouth's glow on the snout's.
    hx0, hy0, _, _ = box_faces(0, 20, 6, 6, 5)[3]
    for ex in (hx0 + 1, hx0 + 4):
        img[hy0 + 2, ex] = (*SEAM_HOT, 255)
        glow[hy0 + 2, ex] = (*SEAM_HOT, 255)
    sx0, sy0, sx1, sy1 = box_faces(22, 20, 3, 3, 4)[3]
    img[sy1 - 1, sx0:sx1] = (*SEAM, 255)
    glow[sy1 - 1, sx0:sx1] = (*SEAM, 255)
    return Image.fromarray(img, "RGBA"), Image.fromarray(glow, "RGBA")


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def main():
    entity = ASSETS / "textures/entity/wild"
    skin, eyes = ash_wraith()
    skin.save(entity / "ash_wraith.png")
    eyes.save(entity / "ash_wraith_eyes.png")
    hide, glow = hound(5600)
    hide.save(entity / "cinder_hound.png")
    glow.save(entity / "cinder_hound_glow.png")
    models = ASSETS / "models/item"
    for name, rows in (("soul_ash", SOUL_ASH), ("cinder_fang", CINDER_FANG), ("ashen_shroud", ASHEN_SHROUD),
                       ("houndstooth_charm", HOUNDSTOOTH)):
        icon(rows).save(ASSETS / f"textures/item/{name}.png")
        write_json(models / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    for egg in ("ash_wraith_spawn_egg", "cinder_hound_spawn_egg"):
        write_json(models / f"{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})

    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    write_json(DATA / "loot_table/entities/ash_wraith.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "soul_ash", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}, looting]}],
         "conditions": [{"condition": "minecraft:killed_by_player"}]}]})
    write_json(DATA / "loot_table/entities/cinder_hound.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": NS + "cinder_fang", "functions": [looting]}],
         "conditions": [{"condition": "minecraft:killed_by_player"},
                        {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
                         "unenchanted_chance": 0.33, "enchanted_chance": {"type": "minecraft:linear", "base": 0.43, "per_level_above_first": 0.1}}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]}]})
    write_json(DATA / "recipe/ashen_shroud.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" A ", "APA", " A "],
        "key": {"A": {"item": NS + "soul_ash"}, "P": {"item": "minecraft:phantom_membrane"}},
        "result": {"id": NS + "ashen_shroud", "count": 1}})
    write_json(DATA / "recipe/houndstooth_charm.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" C ", "FGF", " F "],
        "key": {"C": {"item": "minecraft:chain"}, "F": {"item": NS + "cinder_fang"}, "G": {"item": "minecraft:gold_ingot"}},
        "result": {"id": NS + "houndstooth_charm", "count": 1}})
    write_json(DATA / "tags/worldgen/biome/wild/ash_wraith.json", {"values": ["minecraft:soul_sand_valley"]})
    write_json(DATA / "tags/worldgen/biome/wild/cinder_hound.json", {"values": [
        "minecraft:nether_wastes", "minecraft:crimson_forest", "minecraft:basalt_deltas"]})
    print("nether creatures' art and data written")


if __name__ == "__main__":
    main()
