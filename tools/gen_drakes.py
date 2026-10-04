#!/usr/bin/env python3
"""Generates the Elemental Drakes' art and data (docs/superpowers/specs/2026-10-04-drakes-design.md):

- each drake's hide (256x128, laid out for client/DrakeModel) and its glowing eyes;
- the scales' and charms' icons, and the item models (with the spawn eggs');
- the hoards' loot tables, the charms' recipes, the tags (drakes, innate elements, the tide drake's
  breathing, the drakes' lands and the nests' biomes) and the nests' worldgen.

Run from the project root:  python3 tools/gen_drakes.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"
ELEMENTS = ["fire", "ice", "lightning", "water", "wind"]

PALETTES = {
    "fire": dict(scale=(130, 32, 22), dark=(90, 20, 14), belly=(232, 142, 52), membrane=(214, 84, 34), vein=(150, 46, 18),
                 bone=(70, 26, 18), horn=(236, 222, 190), eye=(255, 220, 80)),
    "ice": dict(scale=(150, 196, 230), dark=(100, 150, 196), belly=(236, 246, 255), membrane=(170, 222, 250), vein=(110, 170, 220),
                bone=(80, 110, 150), horn=(250, 252, 255), eye=(170, 240, 255)),
    "lightning": dict(scale=(58, 58, 78), dark=(36, 36, 50), belly=(236, 214, 90), membrane=(214, 190, 70), vein=(150, 130, 36),
                      bone=(30, 30, 40), horn=(240, 230, 160), eye=(255, 250, 150)),
    "water": dict(scale=(34, 110, 140), dark=(20, 76, 100), belly=(140, 214, 222), membrane=(70, 160, 210), vein=(36, 100, 150),
                  bone=(20, 60, 80), horn=(220, 240, 240), eye=(160, 255, 240)),
    "wind": dict(scale=(112, 176, 152), dark=(76, 130, 110), belly=(226, 246, 238), membrane=(180, 236, 216), vein=(120, 186, 166),
                 bone=(60, 110, 95), horn=(250, 250, 240), eye=(220, 255, 240)),
}
NAMES = {"fire": "Fire", "ice": "Frost", "lightning": "Storm", "water": "Tide", "wind": "Gale"}
RELICS = {"fire": "ember_heart", "ice": "rimeheart_locket", "lightning": "storm_sigil", "wind": "feather_of_the_gale"}
LANDS = {
    "fire": ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:savanna_plateau",
             "minecraft:windswept_savanna"],
    "ice": ["minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:grove", "minecraft:ice_spikes"],
    "lightning": ["minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:stony_peaks"],
    "water": ["minecraft:ocean", "minecraft:deep_ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean",
              "minecraft:deep_lukewarm_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean", "minecraft:beach",
              "minecraft:stony_shore"],
    "wind": ["minecraft:meadow", "minecraft:cherry_grove", "minecraft:windswept_gravelly_hills"],
}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def box_faces(u0, v0, w, h, d):
    """The texture rectangles (x0, y0, x1, y1) of a model box at (u0, v0) of size w x h x d."""
    return {
        "top": (u0 + d, v0, u0 + d + w, v0 + d),
        "bottom": (u0 + d + w, v0, u0 + d + 2 * w, v0 + d),
        "right": (u0, v0 + d, u0 + d, v0 + d + h),
        "front": (u0 + d, v0 + d, u0 + d + w, v0 + d + h),
        "left": (u0 + d + w, v0 + d, u0 + 2 * d + w, v0 + d + h),
        "back": (u0 + 2 * d + w, v0 + d, u0 + 2 * d + 2 * w, v0 + d + h),
    }


class Painter:
    def __init__(self, palette, seed):
        self.img = np.zeros((128, 256, 4))
        self.p = palette
        self.rng = np.random.default_rng(seed)

    def scales(self, rect, base, dark, belly_rows=0, belly=None):
        """Scales: 2x2 cells shaded light and dark in a staggered pattern; the bottom rows fade to the belly."""
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                cell = ((x // 2) + (y // 2) * 3 + (y // 2 % 2)) % 3
                color = np.array(base if cell else dark, dtype=float)
                if belly is not None and y >= y1 - belly_rows:
                    color = np.array(belly, dtype=float)
                color += (self.rng.random() - 0.5) * 14
                self.img[y, x, :3] = np.clip(color, 0, 255)
                self.img[y, x, 3] = 255

    def fill(self, rect, color, noise=10):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.img[y, x, :3] = np.clip(np.array(color, dtype=float) + (self.rng.random() - 0.5) * noise, 0, 255)
                self.img[y, x, 3] = 255

    def creature_box(self, u0, v0, w, h, d, belly_rows=2):
        p = self.p
        faces = box_faces(u0, v0, w, h, d)
        for name, rect in faces.items():
            if name == "bottom":
                self.fill(rect, p["belly"])
            elif name == "top":
                self.scales(rect, p["scale"], p["dark"])
            else:
                self.scales(rect, p["scale"], p["dark"], belly_rows, p["belly"])

    def membrane(self, u0, v0, w, h, d):
        """A wing membrane: the element's colour, darker veins running back from the bone."""
        p = self.p
        for name, rect in box_faces(u0, v0, w, h, d).items():
            x0, y0, x1, y1 = rect
            self.fill(rect, p["membrane"], 8)
            if name in ("top", "bottom"):
                for x in range(x0 + 3, x1, 7):
                    self.img[y0:y1, x, :3] = p["vein"]

    def image(self):
        return Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), "RGBA")


def hide(element):
    p = PALETTES[element]
    paint = Painter(p, 4000 + ELEMENTS.index(element))
    paint.creature_box(0, 0, 14, 12, 32, belly_rows=3)
    for rect in box_faces(96, 50, 2, 3, 24).values():
        paint.fill(rect, p["dark"])
    paint.creature_box(0, 48, 8, 8, 14)
    paint.creature_box(48, 48, 10, 8, 12)
    for rect in box_faces(200, 40, 2, 6, 2).values():
        paint.fill(rect, p["horn"], 6)
    for name, rect in box_faces(48, 72, 8, 3, 10).items():
        paint.fill(rect, p["belly"] if name == "bottom" else p["dark"])
    # Teeth along the jaw's top edge and the head's snout.
    x0, y0, x1, y1 = box_faces(48, 72, 8, 3, 10)["front"]
    for x in range(x0, x1, 2):
        paint.img[y0, x, :3] = (240, 236, 220)
    paint.creature_box(96, 0, 10, 8, 16)
    paint.creature_box(96, 26, 6, 6, 16)
    paint.creature_box(150, 0, 4, 4, 16)
    paint.membrane(150, 22, 10, 1, 8)
    for rect in box_faces(0, 90, 32, 3, 4).values():
        paint.fill(rect, p["bone"])
    paint.membrane(0, 98, 32, 1, 22)
    for rect in box_faces(110, 90, 30, 2, 3).values():
        paint.fill(rect, p["bone"])
    paint.membrane(110, 96, 30, 1, 18)
    paint.creature_box(192, 0, 4, 6, 5)
    paint.creature_box(192, 12, 3, 4, 3, belly_rows=0)
    for rect in box_faces(192, 20, 4, 2, 5).values():
        paint.fill(rect, p["bone"])
    return paint.image()


def eyes(element):
    """The eyes alone, for the glowing layer: on the head's front and both its sides."""
    img = np.zeros((128, 256, 4), dtype=np.uint8)
    color = (*PALETTES[element]["eye"], 255)
    faces = box_faces(48, 48, 10, 8, 12)
    x0, y0, x1, y1 = faces["front"]
    img[y0 + 2, x0 + 1:x0 + 3] = color
    img[y0 + 2, x1 - 3:x1 - 1] = color
    for name in ("right", "left"):
        x0, y0, x1, y1 = faces[name]
        edge = x0 + 1 if name == "left" else x1 - 3
        img[y0 + 2, edge:edge + 2] = color
    return Image.fromarray(img, "RGBA")


SCALE_SHAPE = [
    "................",
    "................",
    ".....dddddd.....",
    "....dsssssLd....",
    "...dssssssLsd...",
    "...dsssssLssd...",
    "...dssssLsssd...",
    "...dsssLssssd...",
    "....dssLsssd....",
    "....dsLssssd....",
    ".....dLsssd.....",
    ".....dssssd.....",
    "......dssd......",
    ".......dd.......",
    "................",
    "................",
]


def scale_icon(element):
    """A drake's scale: rounded above, pointed below, a shine across it."""
    p = PALETTES[element]
    colors = {"d": p["dark"], "s": p["scale"], "L": p["belly"]}
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y, row in enumerate(SCALE_SHAPE):
        for x, ch in enumerate(row):
            if ch in colors:
                img[y, x] = (*colors[ch], 255)
    return Image.fromarray(img, "RGBA")


def charm_icon(element):
    p = PALETTES[element]
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    y, x = np.mgrid[0:16, 0:16] + 0.5
    r = np.hypot(x - 8, y - 8.5)
    img[(r < 6.5) & (r >= 5.0)] = (214, 168, 58, 255)
    img[(r < 5.0)] = (*p["scale"], 255)
    img[(r < 3.2)] = (*p["belly"], 255)
    img[(r < 6.5) & (r >= 6.0)] = (150, 108, 34, 255)
    img[0:2, 7:9] = (214, 168, 58, 255)
    return Image.fromarray(img, "RGBA")


def entry(name, weight, low=None, high=None, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = list(functions or [])
    if low is not None:
        fns.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}})
    if fns:
        e["functions"] = fns
    return e


def main():
    for element in ELEMENTS:
        entity = ASSETS / "textures/entity/drake"
        entity.mkdir(parents=True, exist_ok=True)
        hide(element).save(entity / f"{element}.png")
        eyes(element).save(entity / f"{element}_eyes.png")
        items = ASSETS / "textures/item"
        scale_icon(element).save(items / f"{element}_drake_scale.png")
        charm_icon(element).save(items / f"{element}_drakescale_charm.png")
        models = ASSETS / "models/item"
        for name in (f"{element}_drake_scale", f"{element}_drakescale_charm"):
            write_json(models / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
        write_json(models / f"{element}_drake_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
        write_json(DATA / f"recipe/{element}_drakescale_charm.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": [" S ", "SGS", " S "],
            "key": {"S": {"item": f"{NS}{element}_drake_scale"}, "G": {"item": "minecraft:gold_ingot"}},
            "result": {"id": f"{NS}{element}_drakescale_charm", "count": 1}})
        write_json(DATA / f"tags/worldgen/biome/drakes/{element}.json", {"values": LANDS[element]})
        if element in RELICS:
            write_json(DATA / f"loot_table/chests/drake_hoard_{element}.json", {"type": "minecraft:chest", "pools": [
                {"rolls": {"type": "minecraft:uniform", "min": 4, "max": 7}, "entries": [
                    entry("minecraft:gold_ingot", 8, 2, 6),
                    entry("minecraft:raw_gold", 4, 2, 5),
                    entry("minecraft:emerald", 6, 2, 6),
                    entry(f"{NS}{element}_drake_scale", 6, 1, 3),
                    entry(f"{NS}{element}_essence", 6, 2, 4),
                    entry("minecraft:experience_bottle", 4, 2, 4),
                    entry("minecraft:diamond", 2, 1, 2),
                    entry("minecraft:book", 3, functions=[{"function": "minecraft:enchant_randomly"}]),
                ]},
                {"rolls": 1, "entries": [entry(f"{NS}{RELICS[element]}", 15), {"type": "minecraft:empty", "weight": 85}]},
            ]})
    nest_lands = [biome for element in ("fire", "ice", "lightning", "wind") for biome in LANDS[element]]
    write_json(DATA / "tags/worldgen/biome/has_structure/drake_nests.json", {"values": nest_lands})
    write_json(DATA / "tags/entity_type/drakes.json", {"values": [f"{NS}{e}_drake" for e in ELEMENTS]})
    write_json(DATA / "worldgen/structure/drake_nest.json", {
        "type": f"{NS}drake_nest", "biomes": f"#{NS}has_structure/drake_nests", "step": "surface_structures",
        "spawn_overrides": {}, "terrain_adaptation": "none"})
    write_json(DATA / "worldgen/structure_set/drake_nests.json", {
        "structures": [{"structure": f"{NS}drake_nest", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 64, "separation": 24, "salt": 773311}})
    # Each drake is born to its element; the tide drake breathes under water.
    for element in ELEMENTS:
        path = DATA / f"tags/entity_type/innate/{element}.json"
        tag = json.loads(path.read_text()) if path.exists() else {"values": []}
        if f"{NS}{element}_drake" not in tag["values"]:
            tag["values"].append(f"{NS}{element}_drake")
        write_json(path, tag)
    breathing = DATA.parent / "minecraft/tags/entity_type/can_breathe_under_water.json"
    tag = json.loads(breathing.read_text())
    if f"{NS}water_drake" not in tag["values"]:
        tag["values"].append(f"{NS}water_drake")
    write_json(breathing, tag)
    print("drake art and data written")


if __name__ == "__main__":
    main()
