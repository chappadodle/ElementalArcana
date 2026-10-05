#!/usr/bin/env python3
"""Generates the cantrips' art and data (docs/superpowers/specs/2026-10-04-cantrips-design.md):

- the seven cantrips' spell icons (Mage Light, Prospect, Recall, Mend, Water Breathing,
  Featherfall, Night Eye), drawn as 16x16 pixel art in the Arcane school's pale violet;
- the Cantrip Scroll's icon and item model;
- the Mage Light's orb (texture, full-bright model, blockstate);
- the loot modifiers that hide scrolls in libraries (one chest in two) and other old chests (one
  in four), and their entry in NeoForge's global list.

Run from the project root:  python3 tools/gen_cantrips.py
"""
import json
import math
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data"
NS = "elementalarcana:"

VIOLET = (201, 168, 255)
VIOLET_DARK = (120, 84, 196)
VIOLET_DEEP = (78, 52, 140)
WHITE = (255, 255, 250)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


class Icon:
    def __init__(self):
        self.img = np.zeros((16, 16, 4), dtype=np.uint8)

    def put(self, x, y, color):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < 16 and 0 <= y < 16:
            self.img[y, x] = (*color, 255)

    def line(self, x0, y0, x1, y1, color):
        steps = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for i in range(steps + 1):
            t = i / max(1, steps)
            self.put(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, color)

    def disc(self, cx, cy, r, color):
        for y in range(16):
            for x in range(16):
                if math.hypot(x - cx, y - cy) <= r:
                    self.img[y, x] = (*color, 255)

    def ring(self, cx, cy, r, color, width=0.7):
        for y in range(16):
            for x in range(16):
                if abs(math.hypot(x - cx, y - cy) - r) <= width:
                    self.img[y, x] = (*color, 255)

    def rows(self, rows, colors):
        assert len(rows) == 16 and all(len(row) == 16 for row in rows), rows
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch in colors:
                    self.img[y, x] = (*colors[ch], 255)

    def image(self):
        return Image.fromarray(self.img, "RGBA")


def mage_light():
    icon = Icon()
    for angle in range(0, 360, 45):
        length = 7 if angle % 90 == 0 else 5
        dx, dy = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        icon.line(7.5 + dx * 3, 7.5 + dy * 3, 7.5 + dx * length, 7.5 + dy * length, VIOLET)
    icon.disc(7.5, 7.5, 3.4, VIOLET_DARK)
    icon.disc(7.5, 7.5, 2.6, VIOLET)
    icon.disc(7.5, 7.5, 1.5, WHITE)
    return icon.image()


def prospect():
    icon = Icon()
    stone, dark = (128, 128, 132), (92, 92, 98)
    for y in range(3, 13):
        for x in range(3, 13):
            icon.put(x, y, dark if (x * 3 + y * 5) % 7 == 0 else stone)
    for x, y, color in ((5, 5, (94, 240, 230)), (6, 5, (94, 240, 230)), (5, 6, (60, 190, 184)),
                        (9, 8, (252, 238, 75)), (10, 8, (252, 238, 75)), (10, 9, (200, 180, 40)),
                        (5, 10, (255, 48, 48)), (6, 10, (200, 30, 30)), (10, 4, (255, 48, 48))):
        icon.put(x, y, color)
    # The outline Prospect draws: violet corner brackets round the block.
    for cx, cy, sx, sy in ((1, 1, 1, 1), (14, 1, -1, 1), (1, 14, 1, -1), (14, 14, -1, -1)):
        for i in range(3):
            icon.put(cx + sx * i, cy, VIOLET)
            icon.put(cx, cy + sy * i, VIOLET)
    return icon.image()


def recall():
    icon = Icon()
    # A spiral of motes winding in to a bright heart: the way home.
    for i in range(140):
        t = i / 140 * 4.2 * math.pi
        r = 0.6 + t * 0.48
        x, y = 7.5 + math.cos(t) * r, 7.5 + math.sin(t) * r
        icon.put(x, y, VIOLET if r > 4 else VIOLET_DARK if r > 2.5 else VIOLET)
    icon.disc(7.5, 7.5, 1.2, WHITE)
    return icon.image()


def mend():
    icon = Icon()
    handle, head, shine = (124, 86, 48), (150, 150, 158), (210, 210, 218)
    icon.line(3, 13, 10, 6, handle)
    icon.line(4, 13, 11, 6, (96, 64, 34))
    # The pick's head: an arc across the top of the handle.
    for i, (x, y) in enumerate(((7, 3), (8, 3), (9, 3), (10, 4), (11, 5), (12, 6), (12, 7), (12, 8), (6, 3), (5, 4))):
        icon.put(x, y, shine if i < 3 else head)
    icon.put(9, 4, head)
    icon.put(11, 6, head)
    # A spark of mending.
    green = (126, 230, 120)
    for d in range(-2, 3):
        icon.put(3 + d, 4, green)
        icon.put(3, 4 + d, green)
    icon.put(3, 4, WHITE)
    return icon.image()


def water_breathing():
    icon = Icon()
    blue, light, deep = (120, 196, 255), (220, 244, 255), (64, 132, 220)
    for cx, cy, r in ((5.5, 10, 3.3), (11.5, 4.5, 2.0), (12.5, 12, 1.2)):
        icon.ring(cx, cy, r + 0.55, deep, 0.3)
        icon.ring(cx, cy, r, blue, 0.55)
        icon.put(cx - r * 0.5, cy - r * 0.5, light)
    icon.put(4, 8, WHITE)
    return icon.image()


def featherfall():
    icon = Icon()
    vane, edge, quill = (244, 244, 250), (190, 190, 210), (176, 152, 112)
    # A feather lying along the diagonal from the bottom-left to the top-right: its vane widest
    # high up, the bare quill below; u runs along it, v across it.
    x0, y0, x1, y1 = 2.0, 14.0, 13.5, 2.0
    length = math.hypot(x1 - x0, y1 - y0)
    ux, uy = (x1 - x0) / length, (y1 - y0) / length
    for y in range(16):
        for x in range(16):
            u = (x - x0) * ux + (y - y0) * uy
            v = -(x - x0) * uy + (y - y0) * ux
            if u < 0 or u > length:
                continue
            width = 0 if u < 4 else 2.6 * math.sin(math.pi * min(1.0, (u - 4) / (length - 3.5)))
            if abs(v) <= 0.5:
                icon.put(x, y, quill if u < 5 else vane)
            elif abs(v) <= width:
                icon.put(x, y, edge if abs(v) > width - 0.8 or int(u) % 3 == 0 else vane)
    # It drifts: two short strokes behind it.
    for x, y in ((1, 9), (2, 9), (11, 13), (12, 13), (13, 13)):
        icon.put(x, y, VIOLET)
    return icon.image()


def night_eye():
    icon = Icon()
    outline, white, iris, iris_light = (54, 46, 84), (236, 236, 244), (96, 210, 120), (170, 250, 170)
    icon.rows([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...oowwwwwwoo...",
        "..owwwwiiwwwwo..",
        ".owwwwiILiwwwwo.",
        "owwwwiIPPIiwwwwo",
        "owwwwiIPPIiwwwwo",
        ".owwwwiIIiwwwwo.",
        "..owwwwiiwwwwo..",
        "...oowwwwwwoo...",
        ".....oooooo.....",
        "................",
        "................",
        "................",
    ], {"o": outline, "w": white, "i": iris, "I": (60, 170, 90), "L": iris_light, "P": (16, 18, 24)})
    return icon.image()


def scroll():
    icon = Icon()
    icon.rows([
        "................",
        "................",
        ".dDDDDDDDDDDDDd.",
        ".DdddddddddddDD.",
        "..pppppppppppp..",
        "..pllllllllllp..",
        "..pppppppppppp..",
        "..plllllllllpp..",
        "..pppppppppppp..",
        "..pllllllvvllp..",
        "..ppppppvVvppp..",
        ".dDDDDDDDvDDDDd.",
        ".DdddddddddddDD.",
        "................",
        "................",
        "................",
    ], {"d": (150, 112, 70), "D": (186, 148, 96), "p": (236, 220, 180), "l": (176, 150, 116),
        "v": VIOLET_DARK, "V": VIOLET})
    return icon.image()


def mage_light_orb():
    img = np.zeros((16, 16, 4), dtype=np.uint8)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            color = WHITE if r < 1.6 else (238, 226, 255) if r < 2.6 else (214, 192, 255) if r < 3.4 else VIOLET
            img[y, x] = (*color, 255)
    return Image.fromarray(img, "RGBA")


ICONS = {"mage_light": mage_light, "prospect": prospect, "recall": recall, "mend": mend,
         "water_breathing": water_breathing, "featherfall": featherfall, "night_eye": night_eye}
LIBRARIES = ["crypt_library", "mage_tower_library", "sanctum"]
OLD_CHESTS = ["ruin", "arcanist_cottage", "mage_tower_laboratory", "drake_hoard_fire", "drake_hoard_ice",
              "drake_hoard_lightning", "drake_hoard_wind"]


def modifier(tables, chance):
    return {"type": f"{NS}cantrip_scrolls", "chance": chance, "conditions": [
        {"condition": "minecraft:any_of", "terms": [
            {"condition": "neoforge:loot_table_id", "loot_table_id": f"{NS}chests/{table}"} for table in tables]}]}


def main():
    spells = ASSETS / "textures/spell"
    for name, draw in ICONS.items():
        draw().save(spells / f"{name}.png")
    scroll().save(ASSETS / "textures/item/cantrip_scroll.png")
    write_json(ASSETS / "models/item/cantrip_scroll.json",
               {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/cantrip_scroll"}})
    mage_light_orb().save(ASSETS / "textures/block/mage_light.png")
    faces = {side: {"texture": "#orb"} for side in ("north", "east", "south", "west", "up", "down")}
    write_json(ASSETS / "models/block/mage_light.json", {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
        "textures": {"particle": f"{NS}block/mage_light", "orb": f"{NS}block/mage_light"},
        "elements": [{"from": [5, 5, 5], "to": [11, 11, 11], "neoforge_data": {"block_light": 15, "sky_light": 15}, "faces": faces}]})
    write_json(ASSETS / "blockstates/mage_light.json", {"variants": {"": {"model": f"{NS}block/mage_light"}}})
    mods = DATA / "elementalarcana/loot_modifiers"
    write_json(mods / "cantrip_scrolls_libraries.json", modifier(LIBRARIES, 0.5))
    write_json(mods / "cantrip_scrolls_chests.json", modifier(OLD_CHESTS, 0.25))
    global_list = DATA / "neoforge/loot_modifiers/global_loot_modifiers.json"
    entries = json.loads(global_list.read_text())["entries"] if global_list.exists() else []
    for name in ("cantrip_scrolls_libraries", "cantrip_scrolls_chests"):
        if f"{NS}{name}" not in entries:
            entries.append(f"{NS}{name}")
    write_json(global_list, {"replace": False, "entries": entries})
    print("cantrip art and data written")


if __name__ == "__main__":
    main()
