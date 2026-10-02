#!/usr/bin/env python3
"""Generates the creature art (docs/superpowers/specs/2026-10-02-wisps-design.md) as pixel art:

- the five elemental wisps (32x32 entity textures, laid out for client/WispModel): a bright core
  (a 4-pixel cube), a shell around it (7 pixels, see-through except Earth's cracked stone), and
  small motes (2 pixels) that circle it.

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
        else:
            paint_shell(img, rect, rng, body, edge, alpha, edge_alpha)
    return Image.fromarray(img, "RGBA")


def main():
    for i, name in enumerate(WISPS):
        save(wisp(name, 900 + i), ASSETS / f"entity/wisp/{name}.png")


if __name__ == "__main__":
    main()
