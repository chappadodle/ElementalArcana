#!/usr/bin/env python3
"""Generates the Wonders of the Wild's art and data (docs/superpowers/specs/2026-10-06-wonders-of-the-wild-design.md):

- a glowmoth's skin for each element (32x32, client/GlowmothModel's layout): a dark furry body, pale
  antennae, wings in the element's colour with paler edges and an eye-spot;
- the skyray (128x128, client/SkyrayModel's layout): deep blue above, pale beneath, bands of spots
  on its back and wings, and those spots alone in a layer of their own (drawn glowing);
- the Bottled Glowmoth (a bottle, and the moth in it on a layer the game tints by element) and the
  Moth Jar (glass, a cork lid, the moth inside, tinted the same way): models and blockstate;
- the two creatures' (empty) loot tables: they drop nothing.

Run from the project root:  python3 tools/gen_wonders.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"

COLORS = {"fire": (255, 122, 31), "water": (63, 156, 255), "ice": (158, 230, 255), "wind": (143, 227, 192), "earth": (181, 137, 90),
          "crystal": (208, 140, 255), "lightning": (255, 225, 77), "radiance": (255, 241, 184)}


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def fill(px, rect, color, rng=None, speckle=0.0, dark=None):
    x, y, w, h = rect
    for py in range(h):
        for qx in range(w):
            c = dark if rng is not None and dark is not None and rng.random() < speckle else color
            px[y + py, x + qx] = (*c, 255)


# ---- glowmoths ----

WING = [
    "LLLL.",
    "LCCLL",
    "LCWCL",
    "LCCCL",
    ".LLL.",
]


def glowmoth(color, seed):
    rng = np.random.default_rng(seed)
    px = np.zeros((32, 32, 4), dtype=np.uint8)
    fur = mix((46, 36, 40), color, 0.15)
    for rect in faces(0, 0, 1, 1, 4).values():
        fill(px, rect, fur, rng, 0.3, mix(fur, (0, 0, 0), 0.4))
    for rect in faces(0, 6, 1, 2, 0).values():
        if rect[2] > 0 and rect[3] > 0:
            fill(px, rect, (230, 224, 210))
    light = mix(color, (255, 255, 255), 0.55)
    palette = {"C": color, "L": light, "W": (255, 255, 255)}
    wing = faces(0, 16, 5, 0, 5)
    for name in ("top", "bottom"):
        x, y, w, h = wing[name]
        for row, line in enumerate(WING):
            for col, ch in enumerate(line):
                if ch != ".":
                    px[y + row, x + col] = (*palette[ch], 255)
    return Image.fromarray(px, "RGBA")


# ---- the skyray ----

BLUE = (30, 58, 110)
BLUE_DARK = (20, 40, 82)
BELLY = (176, 196, 222)
SPOT = (150, 236, 255)


def skyray(seed):
    rng = np.random.default_rng(seed)
    px = np.zeros((128, 128, 4), dtype=np.uint8)
    glow = np.zeros((128, 128, 4), dtype=np.uint8)
    boxes = [(0, 0, 20, 4, 28), (80, 32, 2, 3, 6), (0, 32, 16, 2, 20), (0, 56, 14, 1, 16), (0, 80, 1, 1, 36)]
    for u, v, w, h, d in boxes:
        f = faces(u, v, w, h, d)
        for name, rect in f.items():
            if rect[2] <= 0 or rect[3] <= 0:
                continue
            if name == "bottom":
                fill(px, rect, BELLY, rng, 0.15, mix(BELLY, BLUE, 0.25))
            else:
                fill(px, rect, BLUE, rng, 0.2, BLUE_DARK)
        # Spots in bands along the back and wings (the top faces), every few pixels.
        x, y, w, d = f["top"]
        if w >= 6 and d >= 6:
            for py in range(1, d - 1, 4):
                for qx in range(1 + (py // 4) % 2 * 2, w - 1, 4):
                    for gx, gy in ((qx, py), (qx + 1, py)):
                        if gx < w and gy < d:
                            px[y + gy, x + gx] = (*SPOT, 255)
                            glow[y + gy, x + gx] = (*SPOT, 255)
    return Image.fromarray(px, "RGBA"), Image.fromarray(glow, "RGBA")


# ---- the bottle and the jar (16x16 grids) ----

BOTTLE = [
    "................",
    "......kkkk......",
    "......kcck......",
    ".......kk.......",
    "......kggk......",
    ".....kg..gk.....",
    "....kg....gk....",
    "...kg......gk...",
    "...kg......gk...",
    "...kg......gk...",
    "...kg......gk...",
    "...kgg....ggk...",
    "....kgggggggk...",
    ".....kkkkkkk....",
    "................",
    "................",
]
BOTTLE_MOTH = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....ww..ww.....",
    "....wllwwllw....",
    "....wlllllw.....",
    ".....wwddww.....",
    "......w..w......",
    "................",
    "................",
    "................",
    "................",
]
JAR_GLASS = [
    "kkkkkkkkkkkkkkkk",
    "kggffffffffffffk",
    "kgfffffffffffffk",
    "kgfffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kffffffffffffffk",
    "kfffffffffffffgk",
    "kfffffffffffffgk",
    "kffffffffffffggk",
    "kgffffffffffgggk",
    "kkkkkkkkkkkkkkkk",
]
CORK = [
    "cCcCcCcCcCcCcCcC",
    "CcdcCcCdcCcCcdcC",
    "cCcCcdcCcCcCcCcc",
    "CcCcCcCcCdcCcCcC",
    "cdcCcCcCcCcCdcCc",
    "CcCcCdcCcCcCcCcC",
    "cCcCcCcCcCdcCcCc",
    "CcCdcCcCcCcCcCdC",
    "cCcCcCcCdcCcCcCc",
    "CcCcCcCcCcCcCcCc",
    "cCdcCcCcCcCdcCcC",
    "CcCcCcdcCcCcCcCc",
    "cCcCcCcCcCcCcdcC",
    "CcdcCcCcCcCcCcCc",
    "cCcCcCcdcCcCcCcC",
    "CcCcCcCcCcCcCcCc",
]
JAR_MOTH = [
    "................",
    "................",
    "................",
    "................",
    "..ww........ww..",
    ".wllww....wwllw.",
    ".wllllwwwwllllw.",
    ".wlllllddlllllw.",
    "..wlllldddllll..",
    "...wwlldddllw...",
    ".....ww.d.ww....",
    "......w...w.....",
    "................",
    "................",
    "................",
    "................",
]
PALETTE = {
    "k": (200, 222, 236, 205), "g": (255, 255, 255, 170), "f": (214, 236, 250, 48), "c": (150, 110, 70, 255), "C": (176, 132, 86, 255),
    "d": (60, 50, 50, 255), "w": (255, 255, 255, 255), "l": (214, 214, 214, 255),
}


def grid(rows):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), PALETTE[ch])
    return img


def jar_model():
    """The jar: glass sides and top, a cork lid, and the moth inside on two crossed planes, tinted."""
    def cube(frm, to, texture, tint=False):
        face = {"texture": texture}
        if tint:
            face["tintindex"] = 0
        return {"from": frm, "to": to, "faces": {side: dict(face) for side in ("north", "south", "east", "west", "up", "down")}}
    # The whole moth on each plane (a face's own UVs would only crop part of it).
    moth = {"texture": "#moth", "tintindex": 0, "uv": [1, 3, 15, 13]}
    return {
        "ambientocclusion": False, "render_type": "minecraft:translucent",
        "textures": {"particle": f"{NS}block/moth_jar", "glass": f"{NS}block/moth_jar", "cork": f"{NS}block/moth_jar_cork",
                     "moth": f"{NS}block/moth_jar_moth"},
        "elements": [
            {"from": [7.5, 2, 4.5], "to": [8.5, 8, 11.5], "rotation": {"origin": [8, 5, 8], "axis": "y", "angle": 45},
             "faces": {"east": dict(moth), "west": dict(moth)}},
            {"from": [4.5, 2, 7.5], "to": [11.5, 8, 8.5], "rotation": {"origin": [8, 5, 8], "axis": "y", "angle": 45},
             "faces": {"north": dict(moth), "south": dict(moth)}},
            cube([4, 0, 4], [12, 9, 12], "#glass"),
            cube([5, 9, 5], [11, 11, 11], "#cork"),
        ]}


def main():
    textures = ASSETS / "textures"
    for i, (element, color) in enumerate(COLORS.items()):
        save(glowmoth(color, 3100 + i), textures / f"entity/glowmoth/{element}.png")
    body, glow = skyray(3200)
    save(body, textures / "entity/skyray.png")
    save(glow, textures / "entity/skyray_glow.png")
    save(grid(BOTTLE), textures / "item/bottled_glowmoth.png")
    save(grid(BOTTLE_MOTH), textures / "item/bottled_glowmoth_moth.png")
    save(grid(JAR_GLASS), textures / "block/moth_jar.png")
    save(grid(CORK), textures / "block/moth_jar_cork.png")
    save(grid(JAR_MOTH), textures / "block/moth_jar_moth.png")
    models = ASSETS / "models"
    write_json(models / "block/moth_jar.json", jar_model())
    write_json(models / "item/bottled_glowmoth.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"{NS}item/bottled_glowmoth", "layer1": f"{NS}item/bottled_glowmoth_moth"}})
    for egg in ("glowmoth_spawn_egg", "skyray_spawn_egg"):
        write_json(models / f"item/{egg}.json", {"parent": "minecraft:item/template_spawn_egg"})
    write_json(ASSETS / "blockstates/moth_jar.json", {"variants": {
        f"element={i}": {"model": f"{NS}block/moth_jar"} for i in range(len(COLORS))}})
    for creature in ("glowmoth", "skyray"):
        write_json(DATA / f"loot_table/entities/{creature}.json", {"type": "minecraft:entity", "pools": []})
    print("wonders art and data written")


if __name__ == "__main__":
    main()
