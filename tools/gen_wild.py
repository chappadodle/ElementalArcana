#!/usr/bin/env python3
"""Generates the Creatures of the Wild's art and data (docs/superpowers/specs/2026-10-04-wild-creatures-design.md):

- the Thornwood Treant's bark and leaves for each of its woods (128x64, laid out for client/TreantModel)
  and its glowing eyes;
- the Frost Wraith's translucent robes (64x64, client/FrostWraithModel) and its eyes;
- the Ember Salamander's scales (64x32, client/SalamanderModel) and their glow;
- the icons of Heartwood, Wraith Silk, the Salamander Scale and the Rooted effect, and the item models;
- the loot tables, the biome tags of their lands, and the entity tags (innate elements, can_attune,
  wild_creatures). (Their advancement, Into the Wild, is in tools/gen_advancements.py.)
- Creatures of the Wild II (docs/superpowers/specs/2026-10-05-wild-creatures-2-design.md): the Gale
  Harpy's feathers (64x64), the Crystal Crawler's plates and crystals (64x32, and their glow), the
  Bog Lurker's hide (128x64), their materials' icons, loot, lands and tags;
- Trophies of the Wild (docs/superpowers/specs/2026-10-04-wild-trophies-design.md): the three charms'
  icons and recipes, and the Cooling Crust's textures (its crust and four stages of glowing cracks),
  models and blockstate.

Run from the project root:  python3 tools/gen_wild.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

WOODS = {
    "oak": dict(bark=(109, 85, 50), groove=(70, 54, 31), ridge=(138, 108, 66), leaves=(72, 128, 38), leaf_dark=(46, 94, 24),
                leaf_light=(108, 162, 58), ring=(176, 140, 86)),
    "birch": dict(bark=(214, 212, 204), groove=(44, 42, 38), ridge=(236, 236, 230), leaves=(102, 150, 62), leaf_dark=(74, 116, 44),
                  leaf_light=(136, 180, 84), ring=(206, 186, 128)),
    "spruce": dict(bark=(80, 58, 36), groove=(52, 37, 22), ridge=(104, 77, 48), leaves=(56, 94, 60), leaf_dark=(36, 66, 40),
                   leaf_light=(78, 118, 78), ring=(150, 112, 70)),
    "dark_oak": dict(bark=(62, 44, 26), groove=(38, 27, 15), ridge=(86, 63, 39), leaves=(46, 98, 26), leaf_dark=(30, 70, 16),
                     leaf_light=(68, 126, 38), ring=(120, 88, 54)),
}
TREANT_EYE = (150, 255, 110)

TREANT_LANDS = ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                "minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"]
WRAITH_LANDS = ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:snowy_slopes",
                "minecraft:grove", "minecraft:frozen_river", "minecraft:frozen_peaks", "minecraft:jagged_peaks"]
HARPY_LANDS = ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks",
               "minecraft:jagged_peaks", "minecraft:meadow"]
LURKER_LANDS = ["minecraft:swamp", "minecraft:mangrove_swamp"]
SALAMANDER_LANDS = ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands", "minecraft:desert",
                    "minecraft:nether_wastes", "minecraft:basalt_deltas"]


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


class Canvas:
    def __init__(self, width, height, seed):
        self.img = np.zeros((height, width, 4))
        self.rng = np.random.default_rng(seed)

    def put(self, x, y, color, alpha=255):
        self.img[y, x, :3] = np.clip(np.array(color, dtype=float), 0, 255)
        self.img[y, x, 3] = alpha

    def fill(self, rect, color, noise=10, alpha=255):
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                self.put(x, y, np.array(color, dtype=float) + (self.rng.random() - 0.5) * noise, alpha)

    def image(self):
        return Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), "RGBA")


# --- The treant ---------------------------------------------------------------------------------

def bark(c, rect, wood, vertical=True):
    """Bark: ridges and dark grooves running along the limb (birch: white with black dashes)."""
    p = WOODS[wood]
    x0, y0, x1, y1 = rect
    offsets = c.rng.integers(0, 4, size=max(1, x1 - x0))
    for y in range(y0, y1):
        for x in range(x0, x1):
            if wood == "birch":
                color = np.array(p["bark"], dtype=float) + (c.rng.random() - 0.5) * 12
                if (y * 7 + (x // 3) * 5) % 9 == 0 and c.rng.random() < 0.8:
                    color = np.array(p["groove"], dtype=float)
            else:
                lane = (x - x0 + (offsets[(y - y0) // 5 % len(offsets)] if not vertical else 0)) % 4
                wobble = offsets[(x - x0) % len(offsets)]
                if lane == 0 or (y + wobble) % 7 == 0 and c.rng.random() < 0.4:
                    color = p["groove"]
                elif lane == 2:
                    color = p["ridge"]
                else:
                    color = p["bark"]
                color = np.array(color, dtype=float) + (c.rng.random() - 0.5) * 10
            c.put(x, y, color)


def log_end(c, rect, wood):
    """A cut end: rings of pale wood round a dark heart, a rim of bark."""
    p = WOODS[wood]
    x0, y0, x1, y1 = rect
    cx, cy = (x0 + x1 - 1) / 2, (y0 + y1 - 1) / 2
    for y in range(y0, y1):
        for x in range(x0, x1):
            edge = min(x - x0, x1 - 1 - x, y - y0, y1 - 1 - y)
            if edge == 0:
                color = p["bark"]
            else:
                r = max(abs(x - cx), abs(y - cy))
                color = p["ring"] if int(r) % 2 == 0 else tuple(v * 0.82 for v in p["ring"])
            c.put(x, y, np.array(color, dtype=float) + (c.rng.random() - 0.5) * 8)


def leaves(c, rect, wood, holes=0.08, shade=1.0):
    p = WOODS[wood]
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            roll = c.rng.random()
            if roll < holes:
                c.put(x, y, (0, 0, 0), 0)
                continue
            color = p["leaf_dark"] if roll < 0.35 else p["leaf_light"] if roll > 0.85 else p["leaves"]
            c.put(x, y, np.array(color, dtype=float) * shade + (c.rng.random() - 0.5) * 10)


def treant(wood):
    p = WOODS[wood]
    c = Canvas(128, 64, 5100 + list(WOODS).index(wood))
    # The crown: holes along its sides and top; its underside darker.
    for name, rect in box_faces(0, 0, 18, 14, 18).items():
        leaves(c, rect, wood, holes=0.0 if name == "bottom" else 0.07, shade=0.7 if name == "bottom" else 1.0)
    # The trunk, its face on the front: two knotholes and a crooked mouth.
    faces = box_faces(72, 0, 12, 20, 10)
    for name, rect in faces.items():
        if name in ("top", "bottom"):
            log_end(c, rect, wood)
        else:
            bark(c, rect, wood)
    x0, y0, x1, y1 = faces["front"]
    hollow = (24, 16, 10) if wood != "birch" else (40, 34, 28)
    for ex in (x0 + 2, x1 - 5):
        for dx in range(3):
            for dy in range(2):
                c.put(ex + dx, y0 + 4 + dy, hollow)
        c.put(ex + 1, y0 + 3, p["groove"])
    mouth = [(2, 0), (3, 1), (4, 1), (5, 0), (6, 1), (7, 1), (8, 0), (9, 1)]
    for dx, dy in mouth:
        c.put(x0 + dx, y0 + 10 + dy, hollow)
        c.put(x0 + dx, y0 + 11 + dy, hollow)
    # The arms and their twigs (a leaf or two at the twig's end).
    for name, rect in box_faces(0, 32, 4, 20, 4).items():
        log_end(c, rect, wood) if name in ("top", "bottom") else bark(c, rect, wood)
    for name, rect in box_faces(40, 32, 2, 6, 2).items():
        bark(c, rect, wood)
    for name in ("front", "back", "left", "right"):
        x0, y0, x1, y1 = box_faces(40, 32, 2, 6, 2)[name]
        for x in range(x0, x1):
            c.put(x, y1 - 1, p["leaves"])
    # The legs: bark, roots spreading over their feet.
    for name, rect in box_faces(16, 32, 6, 14, 6).items():
        if name == "top":
            log_end(c, rect, wood)
            continue
        bark(c, rect, wood)
        x0, y0, x1, y1 = rect
        if name == "bottom":
            c.fill(rect, (74, 52, 30), 12)
        else:
            for x in range(x0, x1):
                for y in range(y1 - 3, y1):
                    if (x + y) % 3 != 0:
                        c.put(x, y, (96, 70, 42) if wood != "birch" else (90, 80, 70))
    return c.image()


def treant_eyes():
    img = np.zeros((64, 128, 4), dtype=np.uint8)
    x0, y0, x1, y1 = box_faces(72, 0, 12, 20, 10)["front"]
    for ex in (x0 + 2, x1 - 5):
        img[y0 + 4, ex + 1] = (*TREANT_EYE, 255)
        img[y0 + 5, ex:ex + 3] = (*TREANT_EYE, 255)
        img[y0 + 4, ex] = (90, 200, 70, 255)
        img[y0 + 4, ex + 2] = (90, 200, 70, 255)
    return Image.fromarray(img, "RGBA")


# --- The wraith ---------------------------------------------------------------------------------

CLOTH = (206, 228, 242)
CLOTH_FOLD = (150, 186, 214)
WRAITH_EYE = (120, 220, 255)


def cloth(c, rect, alpha, folds=True, fade=False, ragged=False):
    """Pale frost-cloth: folds running down it, frost glints; {fade}: thinning out toward the bottom;
    {ragged}: its lower edge torn, every column a different length."""
    x0, y0, x1, y1 = rect
    lengths = c.rng.integers(max(1, (y1 - y0) // 2), y1 - y0 + 1, size=x1 - x0)
    for y in range(y0, y1):
        for x in range(x0, x1):
            if ragged and y - y0 >= lengths[x - x0]:
                c.put(x, y, (0, 0, 0), 0)
                continue
            color = CLOTH_FOLD if folds and (x - x0) % 3 == 1 and c.rng.random() < 0.75 else CLOTH
            color = np.array(color, dtype=float) + (c.rng.random() - 0.5) * 10
            if c.rng.random() < 0.04:
                color = np.array((250, 254, 255), dtype=float)
            a = alpha
            if fade:
                a = int(alpha * (1 - 0.75 * (y - y0) / max(1, y1 - y0)))
            c.put(x, y, color, a)


def wraith():
    c = Canvas(64, 64, 5200)
    # The head under the hood: shadow, the eyes only lit in the glow layer.
    for name, rect in box_faces(0, 0, 8, 8, 8).items():
        c.fill(rect, (22, 30, 46), 6, 245)
    x0, y0, x1, y1 = box_faces(0, 0, 8, 8, 8)["front"]
    for ex in (x0 + 1, x1 - 3):
        c.put(ex, y0 + 4, (70, 120, 160), 255)
        c.put(ex + 1, y0 + 4, (70, 120, 160), 255)
    # The hood: frost-cloth over the head, open at the face.
    for name, rect in box_faces(32, 0, 8, 8, 8).items():
        cloth(c, rect, 215, folds=name != "top")
    x0, y0, x1, y1 = box_faces(32, 0, 8, 8, 8)["front"]
    for y in range(y0 + 2, y1):
        for x in range(x0 + 1, x1 - 1):
            c.put(x, y, (0, 0, 0), 0)
    # Body and robe.
    for name, rect in box_faces(0, 16, 8, 10, 4).items():
        cloth(c, rect, 205)
    for name, rect in box_faces(24, 16, 10, 8, 6).items():
        if name == "bottom":
            c.fill(rect, (0, 0, 0), 0, 0)
        else:
            cloth(c, rect, 195)
    # The tatters fade and tear away below; no top or bottom.
    for name, rect in box_faces(0, 32, 10, 10, 6).items():
        if name in ("top", "bottom"):
            c.fill(rect, (0, 0, 0), 0, 0)
        else:
            cloth(c, rect, 175, fade=True, ragged=True)
    # Sleeves, torn at the ends, nothing inside them.
    for name, rect in box_faces(32, 32, 3, 12, 3).items():
        if name == "bottom":
            c.fill(rect, (18, 26, 40), 4, 220)
        elif name == "top":
            cloth(c, rect, 205, folds=False)
        else:
            x0, y0, x1, y1 = rect
            cloth(c, (x0, y0, x1, y1 - 3), 205)
            cloth(c, (x0, y1 - 3, x1, y1), 205, folds=False, ragged=True)
    return c.image()


def wraith_eyes():
    img = np.zeros((64, 64, 4), dtype=np.uint8)
    x0, y0, x1, y1 = box_faces(0, 0, 8, 8, 8)["front"]
    for ex in (x0 + 1, x1 - 3):
        img[y0 + 4, ex:ex + 2] = (*WRAITH_EYE, 255)
    return Image.fromarray(img, "RGBA")


# --- The salamander -----------------------------------------------------------------------------

SCALE = (112, 38, 20)
SCALE_DARK = (72, 22, 12)
BELLY = (196, 104, 46)
EMBER = (255, 150, 40)
EMBER_HOT = (255, 214, 96)
MOUTH = (255, 96, 32)
SAL_EYE = (255, 226, 90)


def scales(c, rect, belly_rows=0, spots=None):
    """Scales in a staggered 2x2 pattern; the lower rows the belly's colour; {spots}: glowing spots."""
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            cell = ((x // 2) + (y // 2) * 3 + (y // 2 % 2)) % 3
            color = np.array(SCALE if cell else SCALE_DARK, dtype=float)
            if y >= y1 - belly_rows:
                color = np.array(BELLY, dtype=float)
            c.put(x, y, color + (c.rng.random() - 0.5) * 14)
    if spots:
        for x, y in spots:
            c.put(x, y, EMBER)


def back_spots(rect, step=3, row=None):
    """A row of spots down the middle of a face (its back), every {step} pixels."""
    x0, y0, x1, y1 = rect
    mid = (x0 + x1) // 2 if row is None else row
    return [(mid - 1 + (i % 2) * 1, y) for i, y in enumerate(range(y0 + 1, y1 - 1, step))]


def salamander():
    c = Canvas(64, 32, 5300)
    glow = []
    body = box_faces(0, 0, 6, 4, 14)
    for name, rect in body.items():
        if name == "bottom":
            c.fill(rect, BELLY, 12)
        elif name == "top":
            spots = back_spots(rect, 3)
            scales(c, rect, spots=spots)
            glow += spots
        else:
            x0, y0, x1, y1 = rect
            spots = [(x, y0 + 1) for x in range(x0 + 1, x1 - 1, 4)] if name in ("left", "right") else []
            scales(c, rect, belly_rows=1, spots=spots)
            glow += spots
    head = box_faces(40, 0, 5, 3, 5)
    for name, rect in head.items():
        if name == "bottom":
            c.fill(rect, MOUTH, 16)
            x0, y0, x1, y1 = rect
            glow += [(x, y) for x in range(x0, x1) for y in range(y0, y1)]
        else:
            scales(c, rect)
    for name in ("left", "right"):
        x0, y0, x1, y1 = head[name]
        eye = (x0 + 1, y0) if name == "right" else (x1 - 2, y0)
        c.put(*eye, SAL_EYE)
        glow.append(eye)
    x0, y0, x1, y1 = head["front"]
    c.put(x0 + 1, y0 + 2, SCALE_DARK)
    c.put(x1 - 2, y0 + 2, SCALE_DARK)
    jaw = box_faces(40, 8, 5, 1, 4)
    for name, rect in jaw.items():
        if name == "top":
            c.fill(rect, MOUTH, 16)
            x0, y0, x1, y1 = rect
            glow += [(x, y) for x in range(x0, x1) for y in range(y0, y1)]
            for x in range(x0, x1, 2):
                c.put(x, y1 - 1, (250, 240, 220))
        elif name == "bottom":
            c.fill(rect, BELLY, 12)
        else:
            scales(c, rect)
    for u0, w, h, d in ((0, 4, 3, 8), (24, 2, 2, 8)):
        for name, rect in box_faces(u0, 18, w, h, d).items():
            if name == "bottom":
                c.fill(rect, BELLY, 12)
            elif name == "top":
                spots = back_spots(rect, 3)
                scales(c, rect, spots=spots)
                glow += spots
            else:
                scales(c, rect, belly_rows=1 if h > 2 else 0)
    # The tail's tip burns.
    x0, y0, x1, y1 = box_faces(24, 18, 2, 2, 8)["back"]
    for x in range(x0, x1):
        for y in range(y0, y1):
            c.put(x, y, EMBER_HOT)
            glow.append((x, y))
    for name, rect in box_faces(44, 18, 2, 5, 2).items():
        scales(c, rect)
        x0, y0, x1, y1 = rect
        if name not in ("top", "bottom"):
            for x in range(x0, x1):
                c.put(x, y1 - 1, SCALE_DARK)
    glow_img = np.zeros((32, 64, 4), dtype=np.uint8)
    for x, y in glow:
        glow_img[y, x, :3] = c.img[y, x, :3]
        glow_img[y, x, 3] = 255
    return c.image(), Image.fromarray(glow_img, "RGBA")


# --- Creatures of the Wild II ----------------------------------------------------------------------

FEATHER = (112, 126, 152)
FEATHER_DARK = (78, 88, 112)
FEATHER_LIGHT = (156, 170, 194)


def feathers(c, rect, tips=False, rows_per=3):
    """Feathers in rows: each column a vane, a light edge, darker toward its tip; {tips}: the lowest
    row torn into points."""
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            row = (y - y0) % rows_per
            color = FEATHER_LIGHT if row == 0 else FEATHER if (x + (y - y0) // rows_per) % 3 else FEATHER_DARK
            if tips and y == y1 - 1 and (x - x0) % 2:
                c.put(x, y, (0, 0, 0), 0)
                continue
            if tips and y >= y1 - 2:
                color = FEATHER_DARK
            c.put(x, y, np.array(color, dtype=float) + (c.rng.random() - 0.5) * 8)


def harpy():
    c = Canvas(64, 64, 5500)
    skin, eye, brow = (210, 186, 156), (248, 214, 70), (60, 52, 62)
    head = box_faces(0, 0, 6, 6, 6)
    for name, rect in head.items():
        feathers(c, rect)
    x0, y0, x1, y1 = head["front"]
    c.fill((x0, y0 + 2, x1, y1), skin, 8)
    for ex in (x0 + 1, x1 - 2):
        c.put(ex, y0 + 3, eye)
        c.put(ex, y0 + 2, brow)
    c.put(x0 + 2, y0 + 5, (150, 96, 80))
    c.put(x0 + 3, y0 + 5, (150, 96, 80))
    for name, rect in box_faces(24, 0, 2, 5, 6).items():
        feathers(c, rect, rows_per=2)
    for name, rect in box_faces(0, 12, 6, 9, 4).items():
        feathers(c, rect)
    x0, y0, x1, y1 = box_faces(0, 12, 6, 9, 4)["front"]
    c.fill((x0 + 1, y0 + 1, x1 - 1, y1 - 2), (196, 204, 216), 10)
    for name, rect in box_faces(40, 0, 2, 9, 2).items():
        feathers(c, rect)
    for name, rect in box_faces(0, 26, 1, 12, 6).items():
        feathers(c, rect, tips=name in ("right", "left"))
    leg, scale_dark, talon = (206, 176, 96), (164, 136, 70), (52, 44, 40)
    for name, rect in box_faces(20, 12, 2, 7, 2).items():
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                c.put(x, y, scale_dark if (y - y0) % 2 else leg)
    for name, rect in box_faces(28, 12, 3, 1, 4).items():
        c.fill(rect, talon, 6)
    for name, rect in box_faces(20, 26, 5, 1, 5).items():
        feathers(c, rect, rows_per=2)
    return c.image()


CRAWL_PLATE = (54, 48, 64)
CRAWL_DARK = (36, 32, 44)
CRAWL_LIGHT = (76, 68, 90)
CRYSTAL = (196, 140, 255)
CRYSTAL_LIGHT = (238, 212, 255)
CRYSTAL_DARK = (132, 84, 206)
CRAWL_EYE = (226, 170, 255)


def plates(c, rect):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            color = CRAWL_DARK if (y - y0) % 3 == 2 else CRAWL_LIGHT if (x + y) % 7 == 0 else CRAWL_PLATE
            c.put(x, y, np.array(color, dtype=float) + (c.rng.random() - 0.5) * 8)


def crawler():
    c = Canvas(64, 32, 5600)
    glow = []
    for rect in box_faces(0, 0, 8, 5, 10).values():
        plates(c, rect)
    head = box_faces(36, 0, 6, 4, 5)
    for rect in head.values():
        plates(c, rect)
    x0, y0, x1, y1 = head["front"]
    for ex, ey in ((x0 + 1, y0 + 1), (x1 - 2, y0 + 1), (x0 + 2, y0 + 2), (x1 - 3, y0 + 2)):
        c.put(ex, ey, CRAWL_EYE)
        glow.append((ex, ey))
    for name, rect in box_faces(36, 10, 12, 2, 2).items():
        x0, y0, x1, y1 = rect
        plates(c, rect)
        if name in ("front", "back", "top", "bottom"):
            for y in range(y0, y1):
                c.put(x0 + (x1 - x0) // 2, y, CRAWL_LIGHT)
    for name, rect in box_faces(0, 16, 2, 8, 2).items():
        x0, y0, x1, y1 = rect
        for y in range(y0, y1):
            for x in range(x0, x1):
                color = CRYSTAL_LIGHT if x == x0 else CRYSTAL_DARK if x == x1 - 1 else CRYSTAL
                if y == y0 and name not in ("top", "bottom"):
                    color = CRYSTAL_LIGHT
                c.put(x, y, color)
                glow.append((x, y))
    for rect in box_faces(8, 16, 1, 1, 3).values():
        c.fill(rect, (28, 24, 32), 4)
    glow_img = np.zeros((32, 64, 4), dtype=np.uint8)
    for x, y in glow:
        glow_img[y, x, :3] = c.img[y, x, :3]
        glow_img[y, x, 3] = 255
    return c.image(), Image.fromarray(glow_img, "RGBA")


BOG = (74, 90, 48)
BOG_DARK = (50, 62, 32)
BOG_LIGHT = (106, 120, 66)
BOG_BELLY = (176, 160, 104)


def hide_mottled(c, rect, belly_rows=0, ridges=False):
    x0, y0, x1, y1 = rect
    for y in range(y0, y1):
        for x in range(x0, x1):
            roll = c.rng.random()
            color = BOG_DARK if roll < 0.28 else BOG_LIGHT if roll > 0.86 else BOG
            if y >= y1 - belly_rows:
                color = BOG_BELLY
            if ridges and (x - x0) % 3 == 1 and (y - y0) % 4 < 2:
                color = BOG_DARK
            c.put(x, y, np.array(color, dtype=float) + (c.rng.random() - 0.5) * 8)


def lurker():
    c = Canvas(128, 64, 5700)
    for name, rect in box_faces(0, 0, 12, 5, 16).items():
        if name == "bottom":
            c.fill(rect, BOG_BELLY, 10)
        else:
            hide_mottled(c, rect, belly_rows=1 if name != "top" else 0, ridges=name == "top")
    for name, rect in box_faces(56, 0, 10, 4, 8).items():
        if name == "bottom":
            c.fill(rect, (196, 112, 104), 10)
        else:
            hide_mottled(c, rect)
    x0, y0, x1, y1 = box_faces(56, 0, 10, 4, 8)["front"]
    for x in range(x0, x1):
        c.put(x, y1 - 1, BOG_DARK)
    for name, rect in box_faces(56, 12, 10, 2, 8).items():
        if name == "top":
            c.fill(rect, (196, 112, 104), 10)
            x0, y0, x1, y1 = rect
            for x in range(x0, x1, 2):
                c.put(x, y0, (236, 232, 214))
        elif name == "bottom":
            c.fill(rect, BOG_BELLY, 10)
        else:
            hide_mottled(c, rect)
    for name, rect in box_faces(92, 0, 2, 2, 2).items():
        x0, y0, x1, y1 = rect
        c.fill(rect, (214, 196, 70), 6)
        if name == "front":
            c.put(x0, y0, (24, 20, 16))
            c.put(x0, y0 + 1, (24, 20, 16))
    for name, rect in box_faces(92, 4, 3, 4, 3).items():
        hide_mottled(c, rect)
        if name not in ("top", "bottom"):
            x0, y0, x1, y1 = rect
            for x in range(x0, x1):
                c.put(x, y1 - 1, (40, 46, 26))
    for name, rect in box_faces(0, 21, 6, 4, 10).items():
        hide_mottled(c, rect, belly_rows=1 if name in ("left", "right") else 0, ridges=name == "top")
    for name, rect in box_faces(32, 21, 4, 3, 8).items():
        hide_mottled(c, rect, ridges=name == "top")
    return c.image()


# --- Icons --------------------------------------------------------------------------------------

def icon(rows, colors):
    img = np.zeros((len(rows), len(rows[0]), 4), dtype=np.uint8)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                img[y, x] = (*colors[ch], 255) if len(colors[ch]) == 3 else colors[ch]
    return Image.fromarray(img, "RGBA")


HEARTWOOD = [
    "................",
    "................",
    "....bbbbbbb.....",
    "...bwwwwwwwb....",
    "..bwrrrrrrrwb...",
    "..bwrwwwwwrwb...",
    "..bwrwaaawrwb...",
    "..bwrwaHawrwb...",
    "..bwrwaaawrwb...",
    "..bwrwwwwwrwb...",
    "..bwrrrrrrrwb...",
    "...bwwwwwwwb....",
    "....bbbbbbb.....",
    "................",
    "................",
    "................",
]
WRAITH_SILK = [
    "................",
    "................",
    "...ssssssss.....",
    "..sLLLLLLLLs....",
    "..sLfLLLLfLss...",
    "..sLLLLLLLLLLs..",
    "...sLLfLLLLLLs..",
    "...sLLLLLLfLLs..",
    "....sLLLLLLLs...",
    "....sLLLfLLLs...",
    "...sLLLLLLLLs...",
    "...sLfLLLLs.s...",
    "..sLLLs.sLs.....",
    "..sLs....s......",
    "...s............",
    "................",
]
SAL_SCALE = [
    "................",
    "................",
    ".....dddddd.....",
    "....dsssssd.....",
    "...dsseeessd....",
    "...dseEEEesd....",
    "..dsseEhEEessd..",
    "..dsseEEEEessd..",
    "...dsseEEessd...",
    "....dsseessd....",
    ".....dsssd......",
    "......dsd.......",
    ".......d........",
    "................",
    "................",
    "................",
]
ROOTED = [
    "..................",
    "..................",
    "....r........r....",
    "....rr......rr....",
    ".....r.....rr.....",
    ".....rr....r......",
    "......r...rr......",
    ".r....rr..r....r..",
    ".rr....r.rr...rr..",
    "..rr...rrr...rr...",
    "...rr..rrr..rr....",
    "....rrrrrrrrr.....",
    ".....rrrrrrr......",
    "..ddddddddddddd...",
    ".dDdddDddddDdddd..",
    "ddddDdddddddDdddd.",
    "..................",
    "..................",
]


TALISMAN = [
    "................",
    ".......ss.......",
    "......s..s......",
    "......s..s......",
    ".......ss.......",
    ".....bbbbbb.....",
    "....bwwwwwwb....",
    "...bwwwLLwwwb...",
    "...bwwLggLwwb...",
    "...bwLggggLwb...",
    "...bwwLggLwwb...",
    "...bwwwgLwwwb...",
    "....bwwgwwwb....",
    ".....bbbbbb.....",
    "................",
    "................",
]
VEIL = [
    "................",
    "......cCc.......",
    ".....sLcLs......",
    "....sLLLLLs.....",
    "....sLLfLLLs....",
    "...sLLLLLLLs....",
    "...sLfLLLLLLs...",
    "...sLLLLLfLLs...",
    "..sLLLLLLLLLs...",
    "..sLLLfLLLLLLs..",
    "..sLLLLLLLLLLs..",
    "..sLsLLsLLsLs...",
    "...s.Ls.Ls.s....",
    "......s..s......",
    "................",
    "................",
]
SAL_CHARM = [
    "................",
    "......ggg.......",
    ".....g...g......",
    ".....g...g......",
    "......ggg.......",
    ".......c........",
    "......dsd.......",
    ".....dseesd.....",
    "....dseEEesd....",
    "....dseEhEesd...",
    "....dseEEEesd...",
    ".....dseEesd....",
    "......dseesd....",
    ".......dssd.....",
    "........dd......",
    "................",
]


def crust():
    """The Cooling Crust: dark cooled rock, and its cracks for each of its four stages, wider and
    brighter as it ages (the cracks are drawn glowing, on their own layer)."""
    rng = np.random.default_rng(5400)
    base = np.zeros((16, 16, 4))
    for y in range(16):
        for x in range(16):
            roll = rng.random()
            color = (58, 42, 38) if roll < 0.45 else (42, 31, 29) if roll < 0.8 else (78, 56, 46)
            base[y, x, :3] = np.array(color, dtype=float) + (rng.random() - 0.5) * 8
            base[y, x, 3] = 255
    # Crack paths: each runs mostly one way across the tile, jittering; each stage shows more of them,
    # brighter (they wrap at the edges, so neighbouring crusts join up).
    paths = []
    for _ in range(8):
        x, y = (int(v) for v in rng.integers(0, 16, size=2))
        heading = [(1, 0), (0, 1), (1, 1), (1, -1)][int(rng.integers(0, 4))]
        path = []
        for _ in range(int(rng.integers(6, 12))):
            path.append((x % 16, y % 16))
            if rng.random() < 0.35:
                x, y = x + heading[1], y + heading[0]
            else:
                x, y = x + heading[0], y + heading[1]
        paths.append(path)
    stages = []
    for age in range(4):
        img = np.zeros((16, 16, 4), dtype=np.uint8)
        hot = (255, 110 + 35 * age, 25 + 20 * age)
        for path in paths[:2 + 2 * age]:
            for x, y in path:
                img[y, x] = (*hot, 255)
        if age == 3:
            for _ in range(4):
                x, y = paths[int(rng.integers(0, len(paths)))][int(rng.integers(0, 5))]
                img[y, x] = (255, 236, 140, 255)
        stages.append(Image.fromarray(img, "RGBA"))
    return Image.fromarray(base.astype(np.uint8), "RGBA"), stages


def crust_model(age):
    faces = {side: {"texture": "#crust", "cullface": side} for side in ("north", "east", "south", "west", "up", "down")}
    glow = {side: {"texture": "#cracks", "cullface": side} for side in ("north", "east", "south", "west", "up", "down")}
    return {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
            "textures": {"particle": f"{NS}block/lava_crust", "crust": f"{NS}block/lava_crust",
                         "cracks": f"{NS}block/lava_crust_cracks_{age}"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces},
                         {"from": [0, 0, 0], "to": [16, 16, 16], "neoforge_data": {"block_light": 15, "sky_light": 15},
                          "faces": glow}]}


def charm_recipe(result, top, material, essence):
    return {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": [" T ", "MEM"],
            "key": {"T": {"item": top}, "M": {"item": f"{NS}{material}"}, "E": {"item": f"{NS}{essence}_essence"}},
            "result": {"id": f"{NS}{result}", "count": 1}}


def trophies():
    items = ASSETS / "textures/item"
    icon(TALISMAN, {"s": (226, 222, 206), "b": (92, 66, 38), "w": (176, 138, 82), "g": (96, 170, 60),
                    "L": (146, 212, 96)}).save(items / "heartwood_talisman.png")
    icon(VEIL, {"c": (110, 196, 250), "C": (232, 250, 255), "s": (120, 160, 196), "L": (214, 234, 248),
                "f": (255, 255, 255)}).save(items / "wraithsilk_veil.png")
    icon(SAL_CHARM, {"g": (214, 168, 58), "c": (150, 108, 34), "d": SCALE_DARK, "s": SCALE, "e": EMBER, "E": (255, 182, 64),
                     "h": EMBER_HOT}).save(items / "salamander_charm.png")
    models = ASSETS / "models/item"
    for name in ("heartwood_talisman", "wraithsilk_veil", "salamander_charm"):
        write_json(models / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    base, stages = crust()
    blocks = ASSETS / "textures/block"
    base.save(blocks / "lava_crust.png")
    for age, img in enumerate(stages):
        img.save(blocks / f"lava_crust_cracks_{age}.png")
        write_json(ASSETS / f"models/block/lava_crust_{age}.json", crust_model(age))
    # Each crust block turned a random way, so the cracks don't repeat block to block.
    write_json(ASSETS / "blockstates/lava_crust.json",
               {"variants": {f"age={age}": [{"model": f"{NS}block/lava_crust_{age}", "y": turn} for turn in (0, 90, 180, 270)]
                             for age in range(4)}})
    recipes = DATA / "recipe"
    write_json(recipes / "heartwood_talisman.json", charm_recipe("heartwood_talisman", "minecraft:string", "heartwood", "earth"))
    write_json(recipes / "wraithsilk_veil.json", charm_recipe("wraithsilk_veil", "minecraft:phantom_membrane", "wraith_silk", "ice"))
    write_json(recipes / "salamander_charm.json", charm_recipe("salamander_charm", "minecraft:gold_nugget", "salamander_scale", "fire"))


PLUME = [
    "................",
    "............LL..",
    "...........LFFL.",
    "..........LFFFL.",
    ".........LFFFL..",
    "........LFFFL...",
    ".......LFFFL....",
    "......LFFdL.....",
    ".....LFFdL......",
    "....LFdFL.......",
    "....LdFL........",
    "...gdLL.........",
    "..gg............",
    ".gg.............",
    "g...............",
    "................",
]
PRISM = [
    "................",
    ".......L........",
    "......LLV.......",
    ".....LLVVV......",
    "....LLVVVVd.....",
    "...LLVVVVVdd....",
    "..LLLVVVVVddd...",
    "..VVVVVHVVVVd...",
    "..dVVVVVVVVdd...",
    "...ddVVVVVdd....",
    "....ddVVVdd.....",
    ".....ddVdd......",
    "......ddd.......",
    ".......d........",
    "................",
    "................",
]
PEARL = [
    "................",
    "................",
    "................",
    ".....dddddd.....",
    "....dpppppPd....",
    "...dppHHpppPd...",
    "...dpHHppppPd...",
    "...dpppppppPd...",
    "...dppppppPPd...",
    "...dpppppPPPd...",
    "....dpPPPPPd....",
    ".....dddddd.....",
    "................",
    "................",
    "................",
    "................",
]


# --- Data ---------------------------------------------------------------------------------------

def item(name, weight=1, low=None, high=None, looting=False):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = []
    if low is not None:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}})
    if looting:
        fns.append({"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                    "count": {"type": "minecraft:uniform", "min": 0, "max": 1}})
    if fns:
        e["functions"] = fns
    return e


def rare(name, chance):
    return {"rolls": 1, "entries": [item(name)], "conditions": [
        {"condition": "minecraft:killed_by_player"},
        {"condition": "minecraft:random_chance_with_enchanted_bonus", "enchantment": "minecraft:looting",
         "unenchanted_chance": chance, "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.05, "per_level_above_first": 0.05}}]}


def loot(essence, common, low, high, material, chance):
    return {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [item(f"{NS}{essence}_essence", 1, 1, 3)], "conditions": [{"condition": "minecraft:killed_by_player"}]},
        {"rolls": 1, "entries": [item(common, 1, low, high, looting=True)]},
        rare(f"{NS}{material}", chance),
    ]}


def add_to_tag(path, names):
    tag = json.loads(path.read_text()) if path.exists() else {"values": []}
    for name in names:
        if name not in tag["values"]:
            tag["values"].append(name)
    write_json(path, tag)


def wild_two():
    entity = ASSETS / "textures/entity/wild"
    harpy().save(entity / "gale_harpy.png")
    skin, glow = crawler()
    skin.save(entity / "crystal_crawler.png")
    glow.save(entity / "crystal_crawler_glow.png")
    lurker().save(entity / "bog_lurker.png")
    items = ASSETS / "textures/item"
    icon(PLUME, {"L": FEATHER_LIGHT, "F": FEATHER, "d": FEATHER_DARK, "g": (214, 176, 70)}).save(items / "harpy_plume.png")
    icon(PRISM, {"L": CRYSTAL_LIGHT, "V": CRYSTAL, "d": CRYSTAL_DARK, "H": (255, 255, 255)}).save(items / "prism_core.png")
    icon(PEARL, {"d": (66, 84, 54), "p": (176, 196, 150), "P": (130, 150, 108), "H": (236, 246, 226)}).save(items / "bog_pearl.png")
    models = ASSETS / "models/item"
    for name in ("harpy_plume", "prism_core", "bog_pearl"):
        write_json(models / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    for name in ("gale_harpy", "crystal_crawler", "bog_lurker"):
        write_json(models / f"{name}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    tables = DATA / "loot_table/entities"
    write_json(tables / "gale_harpy.json", loot("wind", "minecraft:feather", 1, 3, "harpy_plume", 0.15))
    write_json(tables / "crystal_crawler.json", loot("crystal", "minecraft:amethyst_shard", 1, 3, "prism_core", 0.12))
    lurker_loot = loot("water", "minecraft:slime_ball", 0, 2, "bog_pearl", 0.15)
    lurker_loot["pools"].insert(2, {"rolls": 1, "entries": [item("minecraft:clay_ball", 1, 1, 3)]})
    write_json(tables / "bog_lurker.json", lurker_loot)
    # The lurker breathes under water (the tag vanilla reads).
    add_to_tag(DATA.parent / "minecraft/tags/entity_type/can_breathe_under_water.json", [f"{NS}bog_lurker"])


def main():
    entity = ASSETS / "textures/entity/wild"
    entity.mkdir(parents=True, exist_ok=True)
    for wood in WOODS:
        treant(wood).save(entity / f"treant_{wood}.png")
    treant_eyes().save(entity / "treant_eyes.png")
    wraith().save(entity / "frost_wraith.png")
    wraith_eyes().save(entity / "frost_wraith_eyes.png")
    skin, glow = salamander()
    skin.save(entity / "ember_salamander.png")
    glow.save(entity / "ember_salamander_glow.png")

    items = ASSETS / "textures/item"
    icon(HEARTWOOD, {"b": (92, 66, 38), "w": (176, 138, 82), "r": (138, 104, 60), "a": (230, 170, 60),
                     "H": (255, 236, 140)}).save(items / "heartwood.png")
    icon(WRAITH_SILK, {"s": (120, 160, 196), "L": (214, 234, 248), "f": (255, 255, 255)}).save(items / "wraith_silk.png")
    icon(SAL_SCALE, {"d": SCALE_DARK, "s": SCALE, "e": EMBER, "E": (255, 182, 64), "h": EMBER_HOT}).save(items / "salamander_scale.png")
    icon(ROOTED, {"r": (120, 84, 46), "d": (98, 70, 44), "D": (70, 48, 28)}).save(ASSETS / "textures/mob_effect/rooted.png")
    models = ASSETS / "models/item"
    for name in ("heartwood", "wraith_silk", "salamander_scale"):
        write_json(models / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/{name}"}})
    for name in ("thornwood_treant", "frost_wraith", "ember_salamander"):
        write_json(models / f"{name}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})

    tables = DATA / "loot_table/entities"
    write_json(tables / "thornwood_treant.json", loot("earth", "minecraft:stick", 2, 5, "heartwood", 0.15))
    write_json(tables / "frost_wraith.json", loot("ice", "minecraft:snowball", 1, 4, "wraith_silk", 0.2))
    write_json(tables / "ember_salamander.json", loot("fire", "minecraft:magma_cream", 0, 2, "salamander_scale", 0.2))

    biomes = DATA / "tags/worldgen/biome/wild"
    write_json(biomes / "treant.json", {"values": TREANT_LANDS})
    write_json(biomes / "frost_wraith.json", {"values": WRAITH_LANDS})
    write_json(biomes / "salamander.json", {"values": SALAMANDER_LANDS})
    write_json(biomes / "gale_harpy.json", {"values": HARPY_LANDS})
    write_json(biomes / "bog_lurker.json", {"values": LURKER_LANDS})
    innate = DATA / "tags/entity_type/innate"
    add_to_tag(innate / "earth.json", [f"{NS}thornwood_treant"])
    add_to_tag(innate / "ice.json", [f"{NS}frost_wraith"])
    add_to_tag(innate / "fire.json", [f"{NS}ember_salamander"])
    add_to_tag(innate / "wind.json", [f"{NS}gale_harpy"])
    add_to_tag(innate / "crystal.json", [f"{NS}crystal_crawler"])
    add_to_tag(innate / "water.json", [f"{NS}bog_lurker"])
    add_to_tag(DATA / "tags/entity_type/can_attune.json", [f"{NS}thornwood_treant", f"{NS}frost_wraith", f"{NS}ember_salamander",
                                                           f"{NS}gale_harpy", f"{NS}crystal_crawler", f"{NS}bog_lurker"])
    write_json(DATA / "tags/entity_type/wild_creatures.json",
               {"values": [f"{NS}{name}" for name in ("thornwood_treant", "frost_wraith", "ember_salamander", "gale_harpy",
                                                       "crystal_crawler", "bog_lurker")]})
    wild_two()

    trophies()
    print("wild art and data written")


if __name__ == "__main__":
    main()
