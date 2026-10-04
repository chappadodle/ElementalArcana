#!/usr/bin/env python3
"""Generates the Arcane Crypts' art and data (docs/superpowers/specs/2026-10-04-arcane-crypts-design.md):

- textures: each element's rune sigil (runestones) and glyph (floor traps), white for the game to
  tint; the gate keystone's faces; the shimmering runic seal; the coffin's lid, hollow and item
  icon; the grave flame (white, tinted) and its coals; the Revenant's bones, robe (grey, tinted) and
  eyes;
- block models, blockstates and item models for the crypt's blocks;
- the crypt's loot tables (urns, storeroom, library, reliquary, per element where they hold
  Essence and the element's relic) and the Revenant's (its Phylactery now and then).

Run from the project root:  python3 tools/gen_crypts.py
"""
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/elementalarcana"
DATA = ROOT / "src/main/resources/data/elementalarcana"
NS = "elementalarcana:"
ELEMENTS = ["fire", "water", "ice", "wind", "earth", "crystal", "lightning", "radiance"]
RELICS = {"fire": "ember_heart", "water": "tidecallers_pearl", "ice": "rimeheart_locket", "wind": "feather_of_the_gale",
          "earth": "stoneheart_idol", "crystal": "prism_of_the_deep", "lightning": "storm_sigil", "radiance": "sunstone"}
FAMILY = {"fire": "fire", "water": "water", "ice": "water", "wind": "wind", "earth": "earth",
          "crystal": "earth", "lightning": "wind", "radiance": "fire"}

SIGILS = {
    "fire": [
        "....W.....",
        "...WW.....",
        "...WWW....",
        "..WWWW..W.",
        "..WW.WWWW.",
        ".WW..WWW..",
        ".WW...WW..",
        ".WWW.WWW..",
        "..WWWWW...",
        "...WWW....",
    ],
    "water": [
        "....W.....",
        "....W.....",
        "...WWW....",
        "..WW.WW...",
        "..W...W...",
        ".WW...WW..",
        ".W.WWW.W..",
        ".WW...WW..",
        "..WW.WW...",
        "...WWW....",
    ],
    "ice": [
        "....W.....",
        "..W.W.W...",
        "...WWW....",
        "W..WWW..W.",
        ".WWWWWWW..",
        "W..WWW..W.",
        "...WWW....",
        "..W.W.W...",
        "....W.....",
        "..........",
    ],
    "wind": [
        "..........",
        ".WWWWW....",
        "......W...",
        ".WWWW..W..",
        ".....W.W..",
        ".WW..W.W..",
        "....W..W..",
        ".WWW..W...",
        ".....W....",
        "..WWW.....",
    ],
    "earth": [
        "..........",
        "....W.....",
        "...WWW....",
        "...W.W....",
        "..W...W...",
        "..W.W.W...",
        ".W.WWW.W..",
        ".W.....W..",
        "WWWWWWWWW.",
        "..........",
    ],
    "crystal": [
        "....W.....",
        "...W.W....",
        "..W.W.W...",
        ".W..W..W..",
        "W...W...W.",
        ".W..W..W..",
        "..W.W.W...",
        "...W.W....",
        "....W.....",
        "..........",
    ],
    "lightning": [
        ".....WWW..",
        "....WWW...",
        "...WWW....",
        "..WWWWWW..",
        ".....WW...",
        "....WW....",
        "...WW.....",
        "..WW......",
        ".WW.......",
        ".W........",
    ],
    "radiance": [
        "....W.....",
        ".W..W..W..",
        "..W...W...",
        "...WWW....",
        "WW.WWW.WW.",
        "...WWW....",
        "..W...W...",
        ".W..W..W..",
        "....W.....",
        "..........",
    ],
}


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def to_image(img):
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")


def stone(seed, w=16, h=16, base=(62, 62, 70), light=(84, 84, 94), dark=(40, 40, 47)):
    """Dark deepslate-like stone, blocky noise in 2x2 cells."""
    rng = np.random.default_rng(seed)
    noise = rng.random((h, w))
    blocky = np.kron(noise.reshape(h // 2, 2, w // 2, 2).mean(axis=(1, 3)), np.ones((2, 2)))
    img = np.zeros((h, w, 4))
    for c in range(3):
        img[..., c] = np.where(blocky < 0.36, dark[c], np.where(blocky > 0.66, light[c], base[c])) + (noise - 0.5) * 8
    img[..., 3] = 255
    return img


def stamp(img, pattern, top, left, color=(255, 255, 255, 255)):
    for row, line in enumerate(pattern):
        for col, ch in enumerate(line):
            if ch != ".":
                img[top + row, left + col] = color


# ---- Runestones and glyphs ----

def rune(element):
    """The runestone's side overlay: a carved frame and the element's sigil, white on clear."""
    img = np.zeros((16, 16, 4))
    for i in range(2, 14):
        for (y, x) in ((1, i), (14, i), (i, 1), (i, 14)):
            img[y, x] = (255, 255, 255, 255)
    for (y, x) in ((1, 1), (1, 14), (14, 1), (14, 14)):
        img[y, x] = (0, 0, 0, 0)
    stamp(img, SIGILS[element], 3, 3)
    return to_image(img)


def glyph(element):
    """A floor glyph: a ring with four ticks and the element's sigil, white on clear."""
    img = np.zeros((16, 16, 4))
    y, x = np.mgrid[0:16, 0:16] + 0.5
    r = np.hypot(x - 8, y - 8)
    ring = (r > 6.6) & (r < 7.6)
    img[ring] = (255, 255, 255, 200)
    for (ty, tx) in ((0, 7), (0, 8), (15, 7), (15, 8), (7, 0), (8, 0), (7, 15), (8, 15)):
        img[ty, tx] = (255, 255, 255, 255)
    stamp(img, SIGILS[element], 3, 3)
    return to_image(img)


# ---- The gate keystone and the seal ----

def lock_face(progress, opened):
    """The keystone's face overlay: a carved ring with three notches; lit notches glow, and an open one's eye."""
    img = np.zeros((16, 16, 4))
    y, x = np.mgrid[0:16, 0:16] + 0.5
    r = np.hypot(x - 8, y - 8)
    groove = (r > 5.0) & (r < 6.0)
    img[groove] = (22, 20, 28, 255)
    lit = (214, 170, 255, 255)
    dim = (52, 44, 66, 255)
    notches = [(2, 7), (11, 2), (11, 12)]
    for i, (ny, nx) in enumerate(notches):
        color = lit if opened or i < progress else dim
        img[ny:ny + 3, nx:nx + 2] = color
    eye = r < 2.2
    img[eye] = lit if opened else (30, 26, 38, 255)
    if opened:
        img[groove] = (150, 110, 210, 255)
    return to_image(img)


def seal_frames():
    """The runic seal: a translucent violet sheet with rune marks drifting up, 8 frames."""
    rng = np.random.default_rng(907)
    marks = [(rng.integers(0, 16), rng.integers(0, 16), rng.integers(0, 3)) for _ in range(9)]
    shapes = [["W.W", ".W.", "W.W"], ["WWW", "W..", "WWW"], [".W.", "WWW", ".W."]]
    frames = []
    for f in range(8):
        img = np.zeros((16, 16, 4))
        y, x = np.mgrid[0:16, 0:16]
        wave = 0.5 + 0.5 * np.sin((x + y * 0.5 + f * 2) / 3.0)
        img[..., 0] = 120 + 40 * wave
        img[..., 1] = 60 + 30 * wave
        img[..., 2] = 200 + 40 * wave
        img[..., 3] = 96 + 40 * wave
        edge = (x == 0) | (x == 15) | (y == 0) | (y == 15)
        img[edge, 3] = 170
        for (mx, my, kind) in marks:
            top = (my - f * 2) % 16
            for row, line in enumerate(shapes[kind]):
                for col, ch in enumerate(line):
                    if ch == "W":
                        img[(top + row) % 16, (mx + col) % 16] = (235, 210, 255, 230)
        frames.append(img)
    return to_image(np.concatenate(frames, axis=0))


# ---- Coffins ----

def coffin_lid(upper):
    """A coffin's carved lid: a raised border and half of a robed figure with crossed arms."""
    img = stone(1101 if upper else 1102, base=(70, 70, 78), light=(92, 92, 102), dark=(48, 48, 56))
    edge = (32, 32, 38, 255)
    img[:, 0] = edge
    img[:, 15] = edge
    img[0 if upper else 15, :] = edge
    figure = (96, 96, 108, 255)
    shade = (54, 54, 62, 255)
    if upper:
        # Head (a helm with eye slits), shoulders and arms crossed on the chest.
        img[2:7, 5:11] = figure
        img[4, 6:8] = shade
        img[4, 9:10] = shade
        img[4, 8] = shade
        img[7:16, 3:13] = figure
        img[10:12, 4:12] = shade
        for i in range(6):
            img[9 + i // 2, 4 + i] = (120, 120, 132, 255)
            img[9 + i // 2, 11 - i] = (120, 120, 132, 255)
    else:
        # Robe folds down to the feet.
        img[0:13, 3:13] = figure
        for x in (5, 8, 10):
            img[1:12, x] = shade
        img[13:15, 4:7] = shade
        img[13:15, 9:12] = shade
    return to_image(img)


def coffin_inside():
    rng = np.random.default_rng(1103)
    img = np.zeros((16, 16, 4))
    noise = rng.random((16, 16))
    for c in range(3):
        img[..., c] = 18 + noise * 10
    img[..., 3] = 255
    return to_image(img)


def coffin_item():
    """The coffin's icon: a tall carved lid, the figure on it."""
    img = np.zeros((16, 16, 4))
    body = (70, 70, 78, 255)
    edge = (34, 34, 40, 255)
    img[0:16, 4:12] = edge
    img[1:15, 5:11] = body
    img[2:5, 6:10] = (100, 100, 112, 255)
    img[3, 7] = edge
    img[3, 9] = edge
    img[5:14, 6:10] = (96, 96, 108, 255)
    img[7, 6:10] = (56, 56, 64, 255)
    return to_image(img)


# ---- The grave flame ----

def flame_frames():
    """A pale flame, 8 frames, for the game to tint the crypt's colour."""
    frames = []
    rng = np.random.default_rng(1201)
    for f in range(8):
        img = np.zeros((16, 16, 4))
        y, x = np.mgrid[0:16, 0:16] + 0.5
        sway = np.sin((y + f * 2) / 3.0) * 1.2 * (1 - y / 16)
        width = 1.0 + 5.5 * (y / 16) ** 0.7
        d = np.abs(x - 8 - sway) / width
        inside = (d < 1) & (y > 1 + (f % 3))
        core = d < 0.45
        flicker = rng.random((16, 16)) * 30
        img[inside, :3] = 200
        img[inside & core, :3] = 255
        img[inside, :3] = np.clip(img[inside, :3] - flicker[inside, None], 0, 255)
        img[inside, 3] = 255
        frames.append(img)
    return to_image(np.concatenate(frames, axis=0))


def coals():
    rng = np.random.default_rng(1202)
    img = np.zeros((16, 16, 4))
    noise = rng.random((16, 16))
    for c in range(3):
        img[..., c] = 30 + noise * 18
    hot = noise > 0.86
    img[hot, :3] = (210, 205, 200)
    img[..., 3] = 255
    return to_image(img)


# ---- The Revenant ----

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


def fill(img, rect, color):
    x0, y0, x1, y1 = rect
    img[y0:y1, x0:x1] = color


def bone_noise(img, rect, seed, base=(214, 208, 192)):
    rng = np.random.default_rng(seed)
    x0, y0, x1, y1 = rect
    h, w = y1 - y0, x1 - x0
    noise = rng.random((h, w))
    for c in range(3):
        img[y0:y1, x0:x1, c] = base[c] - noise * 26
    img[y0:y1, x0:x1, 3] = 255


def revenant_body():
    """Bleached bones (head, ribs, thin limbs) and, on the hat layer, a crown of dark gold."""
    img = np.zeros((32, 64, 4))
    head = box_faces(0, 0, 8, 8, 8)
    for i, rect in enumerate(head.values()):
        bone_noise(img, rect, 1300 + i)
    x0, y0, _, _ = head["front"]
    hollow = (26, 20, 24, 255)
    img[y0 + 3:y0 + 5, x0 + 1:x0 + 3] = hollow
    img[y0 + 3:y0 + 5, x0 + 5:x0 + 7] = hollow
    img[y0 + 5, x0 + 3:x0 + 5] = hollow
    for x in range(x0 + 1, x0 + 7):
        img[y0 + 7, x] = hollow if x % 2 else (190, 184, 168, 255)
    body = box_faces(16, 16, 8, 12, 4)
    for i, rect in enumerate(body.values()):
        bone_noise(img, rect, 1310 + i)
    fx, fy, fx1, fy1 = body["front"]
    for row in range(fy + 1, fy + 9, 2):
        img[row, fx:fx1] = hollow
    img[fy:fy1, fx + 3:fx + 5] = (200, 194, 178, 255)
    for u0 in (40, 0):
        limb = box_faces(u0, 16, 2, 12, 2)
        for i, rect in enumerate(limb.values()):
            bone_noise(img, rect, 1320 + u0 + i)
    # The crown, round the top of the hat layer.
    gold = (150, 112, 44, 255)
    dark = (96, 70, 30, 255)
    hat = box_faces(32, 0, 8, 8, 8)
    for name in ("front", "back", "left", "right"):
        x0, y0, x1, y1 = hat[name]
        img[y0 + 1:y0 + 3, x0:x1] = gold
        for x in range(x0, x1, 2):
            img[y0, x] = dark
    return to_image(img)


def revenant_robe():
    """The robe, hood and sleeves in greys (tinted with the element): the face left open."""
    img = np.zeros((32, 64, 4))
    rng = np.random.default_rng(1400)

    def cloth(rect, shade=0):
        x0, y0, x1, y1 = rect
        h, w = y1 - y0, x1 - x0
        noise = rng.random((h, w))
        for c in range(3):
            img[y0:y1, x0:x1, c] = 150 - shade + noise * 30 - np.arange(h)[:, None] * 2
        img[y0:y1, x0:x1, 3] = 255

    head = box_faces(0, 0, 8, 8, 8)
    for name, rect in head.items():
        if name != "bottom":
            cloth(rect, 40)
    fx, fy, fx1, fy1 = head["front"]
    img[fy + 2:fy1, fx + 1:fx1 - 1, 3] = 0
    body = box_faces(16, 16, 8, 12, 4)
    for rect in body.values():
        cloth(rect)
    bx, by, bx1, by1 = body["front"]
    img[by:by1, bx + 3:bx + 5, :3] *= 0.7
    img[by + 6, bx:bx1, :3] = 70
    for u0, shade in ((40, 10), (0, 0)):
        for rect in box_faces(u0, 16, 4, 12, 4).values():
            cloth(rect, shade)
    # Ragged hems at the sleeves' and robe's ends.
    for u0 in (40, 0):
        x0, y0, x1, y1 = box_faces(u0, 16, 4, 12, 4)["front"]
        for x in range(u0, u0 + 16, 3):
            img[31, x, 3] = 0
    return to_image(img)


def revenant_eyes():
    img = np.zeros((32, 64, 4))
    x0, y0 = 8, 8
    img[y0 + 3:y0 + 5, x0 + 1:x0 + 3] = (255, 255, 255, 255)
    img[y0 + 3:y0 + 5, x0 + 5:x0 + 7] = (255, 255, 255, 255)
    return to_image(img)


# ---- Models and blockstates ----

def cube_faces(texture, sides=("north", "east", "south", "west", "up", "down"), tint=False, cull=True):
    faces = {}
    for side in sides:
        face = {"texture": texture}
        if cull:
            face["cullface"] = side
        if tint:
            face["tintindex"] = 0
        faces[side] = face
    return faces


GLOW = {"block_light": 15, "sky_light": 15}


def models():
    block = ASSETS / "models/block"
    item = ASSETS / "models/item"
    states = ASSETS / "blockstates"
    for e in ELEMENTS:
        write_json(block / f"runestone_{e}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"particle": "minecraft:block/polished_deepslate", "side": "minecraft:block/polished_deepslate",
                         "top": "minecraft:block/deepslate_tiles", "rune": f"{NS}block/rune_{e}"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16],
                 "faces": {**cube_faces("#side", ("north", "east", "south", "west")), **cube_faces("#top", ("up", "down"))}},
                {"from": [0, 0, 0], "to": [16, 16, 16], "neoforge_data": GLOW,
                 "faces": cube_faces("#rune", ("north", "east", "south", "west"), tint=True)},
            ],
        })
        write_json(block / f"glyph_{e}.json", {
            "parent": "minecraft:block/thin_block",
            "render_type": "minecraft:cutout",
            "ambientocclusion": False,
            "textures": {"particle": f"{NS}block/glyph_{e}", "glyph": f"{NS}block/glyph_{e}"},
            "elements": [{"from": [0, 0, 0], "to": [16, 0.25, 16], "neoforge_data": GLOW,
                          "faces": {"up": {"texture": "#glyph", "tintindex": 0},
                                    "down": {"texture": "#glyph", "tintindex": 0, "cullface": "down"}}}],
        })
    write_json(states / "runestone.json", {"variants": {f"element={e}": {"model": f"{NS}block/runestone_{e}"} for e in ELEMENTS}})
    write_json(states / "glyph.json", {"variants": {f"element={e}": {"model": f"{NS}block/glyph_{e}"} for e in ELEMENTS}})
    write_json(item / "runestone.json", {"parent": f"{NS}block/runestone_fire"})
    write_json(item / "glyph.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}block/glyph_fire"}})

    # The keystone: a face for each count of lit runes, and open.
    names = [f"rune_lock_{n}" for n in range(4)] + ["rune_lock_open"]
    for name in names:
        lit = name != "rune_lock_0"
        overlay = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {"north": {"texture": "#face", "cullface": "north"}}}
        if lit:
            overlay["neoforge_data"] = GLOW
        write_json(block / f"{name}.json", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"particle": "minecraft:block/polished_deepslate", "side": "minecraft:block/polished_deepslate",
                         "front": "minecraft:block/polished_deepslate", "face": f"{NS}block/{name}"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {**cube_faces("#front", ("north",)), **cube_faces("#side", ("east", "south", "west", "up", "down"))}},
                         overlay],
        })
    rotation = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for facing, y in rotation.items():
        variants[f"facing={facing},open=true"] = {"model": f"{NS}block/rune_lock_open", "y": y}
        for n in range(4):
            variants[f"facing={facing},open=false,progress={n}"] = {"model": f"{NS}block/rune_lock_{n}", "y": y}
    write_json(states / "rune_lock.json", {"variants": variants})
    write_json(item / "rune_lock.json", {"parent": f"{NS}block/rune_lock_0"})

    write_json(block / "runic_seal.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                           "textures": {"all": f"{NS}block/runic_seal"}})
    write_json(states / "runic_seal.json", {"variants": {"": {"model": f"{NS}block/runic_seal"}}})
    write_json(item / "runic_seal.json", {"parent": f"{NS}block/runic_seal"})

    # Coffins: closed, a solid block with its lid north; open, the hollow's back, sides (and roof up top).
    for half in ("lower", "upper"):
        write_json(block / f"coffin_{half}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": "minecraft:block/polished_deepslate", "side": "minecraft:block/polished_deepslate",
                         "lid": f"{NS}block/coffin_lid_{half}"},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                          "faces": {**cube_faces("#lid", ("north",)), **cube_faces("#side", ("east", "south", "west", "up", "down"))}}],
        })
        parts = [
            {"from": [0, 0, 14], "to": [16, 16, 16], "faces": {
                "north": {"texture": "#inside"}, "south": {"texture": "#side", "cullface": "south"},
                "east": {"texture": "#side", "cullface": "east"}, "west": {"texture": "#side", "cullface": "west"},
                "up": {"texture": "#side"}, "down": {"texture": "#side", "cullface": "down"}}},
            {"from": [0, 0, 0], "to": [2, 16, 14], "faces": {
                "north": {"texture": "#side"}, "east": {"texture": "#inside"}, "west": {"texture": "#side", "cullface": "west"},
                "up": {"texture": "#side"}, "down": {"texture": "#side", "cullface": "down"}}},
            {"from": [14, 0, 0], "to": [16, 16, 14], "faces": {
                "north": {"texture": "#side"}, "west": {"texture": "#inside"}, "east": {"texture": "#side", "cullface": "east"},
                "up": {"texture": "#side"}, "down": {"texture": "#side", "cullface": "down"}}},
        ]
        if half == "upper":
            parts.append({"from": [2, 13, 0], "to": [14, 16, 14], "faces": {
                "north": {"texture": "#side"}, "down": {"texture": "#inside"}, "up": {"texture": "#side", "cullface": "up"}}})
        else:
            parts.append({"from": [2, 0, 0], "to": [14, 1, 14], "faces": {
                "north": {"texture": "#side"}, "up": {"texture": "#inside"}, "down": {"texture": "#side", "cullface": "down"}}})
        write_json(block / f"coffin_{half}_open.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": "minecraft:block/polished_deepslate", "side": "minecraft:block/polished_deepslate",
                         "inside": f"{NS}block/coffin_inside"},
            "elements": parts,
        })
    variants = {}
    for facing, y in rotation.items():
        for half in ("lower", "upper"):
            for opened in ("false", "true"):
                model = f"{NS}block/coffin_{half}" + ("_open" if opened == "true" else "")
                variants[f"facing={facing},half={half},open={opened}"] = {"model": model, "y": y}
    write_json(states / "coffin.json", {"variants": variants})
    write_json(item / "coffin.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}item/coffin"}})

    # The grave flame: a stone brazier, its flame two crossed planes tinted with the crypt's colour.
    brazier = [
        {"from": [2, 0, 2], "to": [14, 2, 14], "faces": {**cube_faces("#stone", ("north", "east", "south", "west", "up")),
                                                        **cube_faces("#stone", ("down",))}},
        {"from": [4, 2, 4], "to": [12, 6, 12], "faces": cube_faces("#stone", ("north", "east", "south", "west"), cull=False)},
        {"from": [2, 6, 2], "to": [14, 10, 14], "faces": {**cube_faces("#rim", ("north", "east", "south", "west", "down"), cull=False),
                                                         "up": {"texture": "#coals"}}},
    ]
    flame = [
        {"from": [8, 9, 2], "to": [8, 25, 14], "shade": False, "neoforge_data": GLOW,
         "faces": {"east": {"texture": "#flame", "tintindex": 0}, "west": {"texture": "#flame", "tintindex": 0}}},
        {"from": [2, 9, 8], "to": [14, 25, 8], "shade": False, "neoforge_data": GLOW,
         "faces": {"north": {"texture": "#flame", "tintindex": 0}, "south": {"texture": "#flame", "tintindex": 0}}},
    ]
    textures = {"particle": "minecraft:block/polished_deepslate", "stone": "minecraft:block/polished_deepslate",
                "rim": "minecraft:block/chiseled_deepslate", "coals": f"{NS}block/grave_flame_coals", "flame": f"{NS}block/grave_flame"}
    write_json(block / "grave_flame.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                            "textures": textures, "elements": brazier + flame})
    write_json(block / "grave_flame_out.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                                "textures": textures, "elements": brazier})
    write_json(states / "grave_flame.json", {"variants": {"lit=true": {"model": f"{NS}block/grave_flame"},
                                                          "lit=false": {"model": f"{NS}block/grave_flame_out"}}})
    write_json(item / "grave_flame.json", {"parent": f"{NS}block/grave_flame"})
    write_json(item / "revenant_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


# ---- Loot ----

def entry(name, weight, low=None, high=None, functions=None):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = list(functions or [])
    if low is not None:
        fns.insert(0, {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": low, "max": high}})
    if fns:
        e["functions"] = fns
    return e


def empty(weight):
    return {"type": "minecraft:empty", "weight": weight}


def pool(entries, low, high=None):
    rolls = low if high is None else {"type": "minecraft:uniform", "min": low, "max": high}
    return {"rolls": rolls, "entries": entries}


def chest(pools):
    return {"type": "minecraft:chest", "pools": pools}


def with_element(e):
    return [{"function": "minecraft:set_components", "components": {NS + "element": e}}]


ENCHANTED_BOOK = {"function": "minecraft:enchant_randomly"}
MANA_DRAUGHT = [{"function": "minecraft:set_potion", "id": NS + "mana_draught"}]


def lore_book(key, title, author):
    return {"type": "minecraft:item", "name": "minecraft:written_book", "weight": 1, "functions": [
        {"function": "minecraft:set_book_cover", "title": title, "author": author},
        {"function": "minecraft:set_written_book_pages", "mode": "replace_all",
         "pages": [{"translate": f"lore.elementalarcana.{key}.1"}, {"translate": f"lore.elementalarcana.{key}.2"}]},
        {"function": "minecraft:set_custom_data", "tag": "{elementalarcana_lore:1b,elementalarcana_page:\"" + key + "\"}"},
    ]}


def loot():
    tables = DATA / "loot_table"
    for e in ELEMENTS:
        essence = f"{NS}{e}_essence"
        catalyst = f"{NS}{FAMILY[e]}_catalyst"
        write_json(tables / f"chests/crypt_urn_{e}.json", chest([pool([
            entry("minecraft:bone", 8, 1, 3),
            entry("minecraft:gold_nugget", 6, 2, 5),
            entry("minecraft:arrow", 4, 2, 6),
            entry("minecraft:emerald", 3, 1, 1),
            entry(essence, 4, 1, 2),
            entry("minecraft:rotten_flesh", 3, 1, 2),
            entry("minecraft:experience_bottle", 1, 1, 1),
        ], 1)]))
        write_json(tables / f"chests/crypt_store_{e}.json", chest([
            pool([
                entry("minecraft:iron_ingot", 8, 2, 5),
                entry("minecraft:gold_ingot", 6, 1, 3),
                entry("minecraft:emerald", 6, 1, 4),
                entry(essence, 6, 1, 3),
                entry("minecraft:arrow", 4, 4, 10),
                entry("minecraft:coal", 4, 2, 6),
                entry("minecraft:candle", 2, 1, 3),
                entry("minecraft:potion", 2, functions=MANA_DRAUGHT),
            ], 3, 6),
            pool([empty(8), entry(f"{NS}apprentice_wand", 1, functions=with_element(e)), entry(f"{NS}apprentice_hood", 1)], 1),
        ]))
        write_json(tables / f"chests/crypt_reliquary_{e}.json", chest([
            pool([
                entry("minecraft:gold_ingot", 8, 2, 5),
                entry(essence, 8, 2, 4),
                entry("minecraft:emerald", 6, 2, 6),
                entry("minecraft:experience_bottle", 5, 2, 4),
                entry("minecraft:diamond", 4, 1, 3),
                entry("minecraft:book", 4, functions=[ENCHANTED_BOOK]),
            ], 3, 5),
            pool([entry(essence, 1, 2, 4)], 1),
            pool([entry(f"{NS}tome_of_insight", 2), empty(3)], 1),
            pool([entry(catalyst, 3), empty(7)], 1),
            pool([entry(f"{NS}scroll_of_unbinding", 1), empty(9)], 1),
            pool([entry(f"{NS}adept_staff", 1, functions=with_element(e)), empty(4)], 1),
            pool([entry(f"{NS}{RELICS[e]}", 3), empty(7)], 1),
        ]))
    write_json(tables / "chests/crypt_library.json", chest([
        pool([
            entry("minecraft:book", 8, 1, 3),
            entry("minecraft:paper", 8, 2, 6),
            entry("minecraft:book", 5, functions=[ENCHANTED_BOOK]),
            entry("minecraft:experience_bottle", 4, 1, 3),
            entry("minecraft:ink_sac", 3, 1, 3),
            entry("minecraft:feather", 3, 1, 3),
            entry("minecraft:candle", 2, 1, 2),
        ], 3, 5),
        pool([empty(2),
              lore_book("wardens", "The Wardens' Rest", "A Keeper of the Low Halls"),
              lore_book("revenants", "Of Revenants", "Notes of a Crypt-Warden")], 1),
    ]))
    write_json(tables / "entities/revenant.json", {"type": "minecraft:entity", "pools": [
        pool([entry("minecraft:bone", 1, 2, 5, functions=[{"function": "minecraft:enchanted_count_increase",
                                                            "enchantment": "minecraft:looting", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}])], 1),
        pool([entry("minecraft:gold_ingot", 1, 1, 3)], 1),
        pool([entry(f"{NS}tome_of_insight", 7), empty(13)], 1),
        pool([entry(f"{NS}revenants_phylactery", 1), empty(3)], 1),
    ]})


def main():
    block = ASSETS / "textures/block"
    for e in ELEMENTS:
        save(rune(e), block / f"rune_{e}.png")
        save(glyph(e), block / f"glyph_{e}.png")
    for n in range(4):
        save(lock_face(n, False), block / f"rune_lock_{n}.png")
    save(lock_face(3, True), block / "rune_lock_open.png")
    save(seal_frames(), block / "runic_seal.png")
    write_json(block / "runic_seal.png.mcmeta", {"animation": {"frametime": 3, "interpolate": True}})
    save(coffin_lid(True), block / "coffin_lid_upper.png")
    save(coffin_lid(False), block / "coffin_lid_lower.png")
    save(coffin_inside(), block / "coffin_inside.png")
    save(coffin_item(), ASSETS / "textures/item/coffin.png")
    save(flame_frames(), block / "grave_flame.png")
    write_json(block / "grave_flame.png.mcmeta", {"animation": {"frametime": 2}})
    save(coals(), block / "grave_flame_coals.png")
    entity = ASSETS / "textures/entity/revenant"
    save(revenant_body(), entity / "revenant.png")
    save(revenant_robe(), entity / "robe.png")
    save(revenant_eyes(), entity / "eyes.png")
    models()
    loot()
    print("crypt art and data written")


if __name__ == "__main__":
    main()
