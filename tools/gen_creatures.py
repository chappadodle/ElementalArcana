#!/usr/bin/env python3
"""Generates the creature art (docs/superpowers/specs/2026-10-02-wisps-design.md) as pixel art:

- the eight elemental wisps (32x32 entity textures, laid out for client/WispModel): a bright core
  (a 4-pixel cube), a shell around it (7 pixels: see-through glass, except Earth's cracked stone,
  Crystal's faceted amethyst and Lightning's glass with an arc crackling across it), and small
  motes (2 pixels) that circle it;
- the four Elemental Golems (128x64, laid out for client/GolemModel): stone of their element with
  cracks of light, a glowing core and eyes, and an emissive copy holding only the glow.

Run from the project root:  python3 tools/gen_creatures.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


def faces(u, v, w, h, d):
    """The six face rectangles (x, y, width, height) of a model cube's texture at (u, v)."""
    return [
        (u + d, v, w, d), (u + d + w, v, w, d),  # top, bottom
        (u, v + d, d, h), (u + d, v + d, w, h), (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h),  # sides
    ]


def paint_core(img, rect, core, hot):
    x, y, w, h = rect
    img[y:y + h, x:x + w] = (*core, 255)
    img[y + 1:y + h - 1, x + 1:x + w - 1] = (*hot, 255)


def paint_shell(img, rect, rng, body, edge, alpha, edge_alpha):
    x, y, w, h = rect
    for py in range(h):
        for px in range(w):
            border = px in (0, w - 1) or py in (0, h - 1)
            color = edge if border else body
            a = edge_alpha if border else alpha + int(rng.integers(-12, 13))
            if not border and rng.random() < 0.06:
                color, a = edge, edge_alpha  # a glint
            img[y + py, x + px] = (*color, max(0, min(255, a)))


def paint_stone_shell(img, rect, rng, stone, dark, crack):
    """Earth: stone with glowing cracks."""
    x, y, w, h = rect
    for py in range(h):
        for px in range(w):
            n = rng.random()
            c = dark if n < 0.25 else stone
            jitter = int(rng.integers(-10, 11))
            img[y + py, x + px] = (*[max(0, min(255, v + jitter)) for v in c], 240)
    # A crack across each face, glowing with the core's light.
    cx = int(rng.integers(1, w - 1))
    for py in range(h):
        img[y + py, x + cx] = (*crack, 255)
        if rng.random() < 0.5:
            cx = max(1, min(w - 2, cx + int(rng.choice([-1, 1]))))


def paint_crystal_shell(img, rect, rng, facets, glint):
    """Crystal: amethyst cut into diagonal planes of light and shade (3 pixels wide), with glints."""
    x, y, w, h = rect
    flip = rng.random() < 0.5
    for py in range(h):
        for px in range(w):
            plane = ((px + (h - 1 - py if flip else py)) // 3) % len(facets)
            color, a = facets[plane], 150 + int(rng.integers(-12, 13))
            if rng.random() < 0.05:
                color, a = glint, 235
            img[y + py, x + px] = (*color, a)


def paint_storm_shell(img, rect, rng, body, alpha, arc):
    """Lightning: faint glass with a jagged arc crackling across each face."""
    x, y, w, h = rect
    for py in range(h):
        for px in range(w):
            img[y + py, x + px] = (*body, max(0, alpha + int(rng.integers(-10, 11))))
    cy = int(rng.integers(1, h - 1))
    for px in range(w):
        img[y + cy, x + px] = (*arc, 255)
        if rng.random() < 0.6:
            cy = max(0, min(h - 1, cy + int(rng.choice([-1, 1]))))


def paint_mote(img, rect, color, hot):
    x, y, w, h = rect
    img[y:y + h, x:x + w] = (*color, 255)
    img[y, x] = (*hot, 255)


WISPS = {
    # core, hot centre, shell body, shell edge, shell alpha, edge alpha, mote
    # The shell is faint glass with brighter edges, so the core shows through it.
    "fire": ((255, 214, 110), (255, 250, 220), (255, 112, 32), (255, 186, 80), 62, 165, (255, 190, 60)),
    "water": ((150, 214, 255), (240, 252, 255), (48, 132, 255), (140, 204, 255), 58, 160, (110, 190, 255)),
    "ice": ((200, 240, 255), (255, 255, 255), (140, 205, 250), (230, 249, 255), 52, 170, (205, 240, 255)),
    "wind": ((200, 255, 228), (250, 255, 252), (120, 222, 178), (210, 255, 236), 46, 150, (185, 250, 215)),
    "earth": ((255, 196, 96), (255, 244, 196), None, None, 0, 0, (112, 150, 70)),
    # The derived elements (docs/superpowers/specs/2026-10-03-derived-elements-design.md).
    "crystal": ((255, 220, 255), (255, 255, 255), (200, 120, 255), (235, 200, 255), 70, 175, (220, 160, 255)),
    "lightning": ((255, 232, 90), (255, 255, 230), (255, 210, 40), (255, 245, 150), 70, 180, (255, 225, 80)),
    "radiance": ((255, 238, 160), (255, 255, 245), (255, 226, 140), (255, 248, 210), 58, 170, (255, 236, 170)),
}


def wisp(name, seed):
    core, hot, body, edge, alpha, edge_alpha, mote = WISPS[name]
    rng = np.random.default_rng(seed)
    img = np.zeros((32, 32, 4), dtype=np.uint8)
    for rect in faces(0, 0, 4, 4, 4):
        paint_core(img, rect, core, hot)
    for rect in faces(16, 0, 2, 2, 2):
        paint_mote(img, rect, mote, hot if name != "earth" else (150, 190, 100))
    for rect in faces(0, 8, 7, 7, 7):
        if name == "earth":
            paint_stone_shell(img, rect, rng, (120, 100, 78), (84, 70, 56), (255, 190, 90))
        elif name == "crystal":
            paint_crystal_shell(img, rect, rng, ((236, 204, 255), (190, 120, 245), (126, 64, 196)), (255, 255, 255))
        elif name == "lightning":
            paint_storm_shell(img, rect, rng, body, alpha, (255, 255, 225))
        else:
            paint_shell(img, rect, rng, body, edge, alpha, edge_alpha)
    return Image.fromarray(img, "RGBA")


# Appended to tools/gen_creatures.py by the golem patch: the four Elemental Golems' textures.

# name: (light, mid, dark, accent, accent_share, glow, core)
GOLEMS = {
    "fire": ((78, 66, 72), (52, 44, 50), (32, 27, 31), (110, 40, 20), 0.10, (255, 136, 40), (255, 214, 120)),
    "water": ((140, 204, 190), (92, 162, 152), (58, 116, 118), (190, 236, 226), 0.12, (110, 226, 255), (214, 250, 255)),
    "wind": ((232, 232, 222), (204, 204, 196), (170, 172, 168), (186, 240, 214), 0.14, (150, 246, 206), (236, 255, 246)),
    "earth": ((132, 128, 120), (108, 104, 98), (80, 78, 74), (86, 118, 62), 0.22, (255, 178, 76), (255, 216, 130)),
}

# The model's boxes: (u, v, width, height, depth), as client/GolemModel lays them out.
GOLEM_BOXES = {
    "head": (0, 0, 8, 8, 8),
    "body": (0, 16, 14, 17, 8),
    "arm": (44, 16, 6, 20, 6),
    "leg": (0, 41, 6, 14, 6),
}
GOLEM_CORE = (32, 0, 4, 4, 1)


def paint_golem_face(img, glow_img, rect, rng, palette, crack_chance):
    light, mid, dark, accent, accent_share, glow, _core = palette
    x, y, w, h = rect
    for py in range(h):
        for px in range(w):
            n = rng.random()
            c = dark if n < 0.22 else light if n > 0.8 else mid
            if rng.random() < accent_share:
                c = accent
            jitter = int(rng.integers(-8, 9))
            img[y + py, x + px] = (*[max(0, min(255, v + jitter)) for v in c], 255)
            # Blocky edges: a darker rim, like stone bricks.
            if px in (0, w - 1) or py in (0, h - 1):
                img[y + py, x + px, :3] = [max(0, int(v) - 18) for v in img[y + py, x + px, :3]]
    # Cracks of light: short jagged runs across the face.
    if w >= 4 and h >= 4 and rng.random() < crack_chance:
        cx, cy = int(rng.integers(1, w - 1)), int(rng.integers(1, h - 1))
        for _ in range(int(rng.integers(3, 3 + max(w, h) // 2))):
            img[y + cy, x + cx] = (*glow, 255)
            glow_img[y + cy, x + cx] = (*glow, 255)
            cx = max(0, min(w - 1, cx + int(rng.integers(-1, 2))))
            cy = max(0, min(h - 1, cy + int(rng.integers(0, 2))))


def golem(name, seed):
    palette = GOLEMS[name]
    rng = np.random.default_rng(seed)
    img = np.zeros((64, 128, 4), dtype=np.uint8)
    glow_img = np.zeros((64, 128, 4), dtype=np.uint8)
    glow_img[..., 3] = 255
    for part, (u, v, w, h, d) in GOLEM_BOXES.items():
        for rect in faces(u, v, w, h, d):
            paint_golem_face(img, glow_img, rect, rng, palette, 0.5 if part in ("body", "arm") else 0.25)
    # The core: its element's light, brightest in the middle.
    core = palette[6]
    glow = palette[5]
    for rect in faces(*GOLEM_CORE):
        x, y, w, h = rect
        img[y:y + h, x:x + w] = (*glow, 255)
        glow_img[y:y + h, x:x + w] = (*glow, 255)
    fx, fy = GOLEM_CORE[0] + GOLEM_CORE[4], GOLEM_CORE[1] + GOLEM_CORE[4]
    img[fy + 1:fy + 3, fx + 1:fx + 3] = (*core, 255)
    glow_img[fy + 1:fy + 3, fx + 1:fx + 3] = (*core, 255)
    # Eyes: two slits of light on the head's front.
    hx, hy = GOLEM_BOXES["head"][0] + GOLEM_BOXES["head"][4], GOLEM_BOXES["head"][1] + GOLEM_BOXES["head"][4]
    for ex in (hx + 1, hx + 5):
        img[hy + 3, ex:ex + 2] = (*core, 255)
        glow_img[hy + 3, ex:ex + 2] = (*core, 255)
    return Image.fromarray(img, "RGBA"), Image.fromarray(glow_img, "RGBA")


def main():
    for i, name in enumerate(WISPS):
        save(wisp(name, 900 + i), ASSETS / f"entity/wisp/{name}.png")
    for i, name in enumerate(GOLEMS):
        body, glow = golem(name, 950 + i)
        save(body, ASSETS / f"entity/golem/{name}.png")
        save(glow, ASSETS / f"entity/golem/{name}_glow.png")


if __name__ == "__main__":
    main()
