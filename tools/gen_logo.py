#!/usr/bin/env python3
"""Draws the mod's logo (the Mods screen's picture, and the mod page's): "ELEMENTAL ARCANA" in a
blocky pixel font, lit gold on top and shaded violet at the foot with a dark drop shadow, as
Minecraft's own title is, over an arc of the eight elements' orbs.

Run from the project root:  python3 tools/gen_logo.py [output.png]
(default: src/main/resources/elementalarcana_logo.png)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "src/main/resources/elementalarcana_logo.png"

# 5 by 7 glyphs.
GLYPHS = {
    "E": ["#####", "#....", "#....", "####.", "#....", "#....", "#####"],
    "L": ["#....", "#....", "#....", "#....", "#....", "#....", "#####"],
    "M": ["#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"],
    "N": ["#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"],
    "T": ["#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."],
    "A": [".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"],
    "R": ["####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"],
    "C": [".####", "#....", "#....", "#....", "#....", "#....", ".####"],
    " ": [".....", ".....", ".....", ".....", ".....", ".....", "....."],
}
# The elements, in the order of their orbs (base elements, then the ones they make).
ELEMENTS = [(0xFF7A28, "fire"), (0x3F9CFF, "water"), (0x8FE8C8, "wind"), (0xB08850, "earth"),
            (0xA8E4FF, "ice"), (0xD08CFF, "crystal"), (0xFFE04D, "lightning"), (0xFFF2B0, "radiance")]
TOP = np.array([255, 236, 150])
BOTTOM = np.array([176, 104, 236])
SHADOW = (38, 18, 56, 255)
OUTLINE = (24, 10, 36, 255)


def rgb(color):
    return np.array([color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF])


def text(img, words, x0, y0, px):
    """Draws {@code words} with its top-left at (x0, y0), each font pixel {@code px} wide."""
    cursor = x0
    for ch in words:
        glyph = GLYPHS[ch]
        for gy, row in enumerate(glyph):
            for gx, cell in enumerate(row):
                if cell != "#":
                    continue
                x, y = cursor + gx * px, y0 + gy * px
                # Shadow down and right, an outline round it, then the face lit from the top.
                img[y + px // 2:y + px + px // 2, x + px // 2:x + px + px // 2] = SHADOW
        cursor += (len(glyph[0]) + 1) * px
    cursor = x0
    for ch in words:
        glyph = GLYPHS[ch]
        for gy, row in enumerate(glyph):
            for gx, cell in enumerate(row):
                if cell != "#":
                    continue
                x, y = cursor + gx * px, y0 + gy * px
                img[y - 2:y + px + 2, x - 2:x + px + 2] = OUTLINE
        cursor += (len(glyph[0]) + 1) * px
    cursor = x0
    for ch in words:
        glyph = GLYPHS[ch]
        for gy, row in enumerate(glyph):
            for gx, cell in enumerate(row):
                if cell != "#":
                    continue
                x, y = cursor + gx * px, y0 + gy * px
                t = gy / 6
                face = (TOP * (1 - t) + BOTTOM * t).astype(int)
                img[y:y + px, x:x + px] = (*face, 255)
                # Bevelled like a block lit from above: a light lip where the glyph's top is open,
                # a dark one where its foot is.
                band = max(1, px // 4)
                if gy == 0 or glyph[gy - 1][gx] != "#":
                    img[y:y + band, x:x + px] = (*np.minimum(face + 45, 255), 255)
                if gy == len(glyph) - 1 or glyph[gy + 1][gx] != "#":
                    img[y + px - band:y + px, x:x + px] = (*(face * 0.72).astype(int), 255)
        cursor += (len(glyph[0]) + 1) * px


def width(words, px):
    return sum((len(GLYPHS[ch][0]) + 1) * px for ch in words) - px


def orb(img, cx, cy, radius, color):
    """A pixel orb: dark rim, the element's colour, a bright highlight up and left, a soft glow round it."""
    base = rgb(color)
    h, w = img.shape[:2]
    for y in range(int(cy - radius * 2), int(cy + radius * 2) + 1):
        for x in range(int(cx - radius * 2), int(cx + radius * 2) + 1):
            if not (0 <= x < w and 0 <= y < h):
                continue
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= radius:
                light = ((x - cx) + (y - cy)) / (2 * radius)
                shade = np.clip(base * (1.05 - 0.45 * light), 0, 255).astype(int)
                if d > radius - 2.5:
                    shade = (base * 0.45).astype(int)
                if ((x - (cx - radius * 0.35)) ** 2 + (y - (cy - radius * 0.35)) ** 2) ** 0.5 < radius * 0.28:
                    shade = np.minimum(base + 120, 255).astype(int)
                img[y, x] = (*shade, 255)
            elif d <= radius * 1.9 and img[y, x, 3] == 0:
                glow = int(110 * (1 - (d - radius) / (radius * 0.9)))
                img[y, x] = (*base, max(0, glow))


def main():
    w, h = 640, 256
    img = np.zeros((h, w, 4), dtype=np.uint8)
    # The orbs, in an arc over the words.
    for i, (color, _) in enumerate(ELEMENTS):
        t = i / (len(ELEMENTS) - 1)
        cx = 70 + t * (w - 140)
        cy = 40 + 26 * (2 * t - 1) ** 2
        orb(img, cx, cy, 15, color)
    top_px, bottom_px = 8, 11
    line1, line2 = "ELEMENTAL", "ARCANA"
    text(img, line1, (w - width(line1, top_px)) // 2, 84, top_px)
    text(img, line2, (w - width(line2, bottom_px)) // 2, 156, bottom_px)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(img, "RGBA").save(OUT)
    print("logo written:", OUT)


if __name__ == "__main__":
    main()
