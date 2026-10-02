#!/usr/bin/env python3
"""Generates the art for the world's magic places (docs/superpowers/specs/2026-10-02-shrines-design.md):

- the Shrine Core: a carved pedestal (side and top) and a pale crystal the game tints with the
  shrine's element;
- the effect icons (18x18): Place of Power and the four elemental blessings.

Run from the project root:  python3 tools/gen_world.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


def stone(seed, size=16, base=(92, 86, 98), light=(122, 116, 128), dark=(58, 54, 64)):
    rng = np.random.default_rng(seed)
    noise = rng.random((size, size))
    blocky = np.kron(noise.reshape(size // 2, 2, size // 2, 2).mean(axis=(1, 3)), np.ones((2, 2)))
    img = np.zeros((size, size, 4), dtype=np.float64)
    for c in range(3):
        img[..., c] = np.where(blocky < 0.36, dark[c], np.where(blocky > 0.66, light[c], base[c])) + (noise - 0.5) * 10
    img[..., 3] = 255
    return img


def to_image(img):
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")


def shrine_side():
    """The pedestal's side: dark carved stone, a gold band, and rune marks in the lower part."""
    img = stone(701)
    gold = (226, 186, 74, 255)
    dark_gold = (150, 112, 36, 255)
    img[10, :] = gold
    img[15, :] = dark_gold
    for x in (3, 7, 11):
        img[12, x] = gold
        img[13, x - 1:x + 2] = gold
        img[14, x] = dark_gold
    return to_image(img)


def shrine_top():
    """The pedestal's top: a gold ring set in the stone, with a dark socket for the crystal."""
    img = stone(702)
    y, x = np.mgrid[0:16, 0:16] + 0.5
    r = np.hypot(x - 8, y - 8)
    ring = (r > 4.6) & (r < 5.8)
    img[ring] = (226, 186, 74, 255)
    img[r < 2.6] = (34, 30, 40, 255)
    return to_image(img)


def crystal():
    """The floating crystal, in light greys (tinted in game): a bright core and facet lines."""
    y, x = np.mgrid[0:16, 0:16] + 0.5
    shade = 190 + 50 * np.cos((x - 8) / 16 * np.pi) + 15 * np.sin(y / 3)
    img = np.zeros((16, 16, 4))
    for c in range(3):
        img[..., c] = shade
    img[..., 3] = 255
    for i in range(16):
        img[i, (i // 2 + 5) % 16, :3] = 255
    img[:, 7:9, :3] = np.clip(img[:, 7:9, :3] + 30, 0, 255)
    return to_image(img)


def effect_icon(color, rune, seed):
    """An 18x18 effect icon: a soft glow of the colour with a pixel rune in the middle."""
    size = 18
    y, x = np.mgrid[0:size, 0:size] + 0.5
    r = np.hypot(x - 9, y - 9)
    img = np.zeros((size, size, 4))
    glow = np.clip(1 - r / 8.5, 0, 1)
    for c in range(3):
        img[..., c] = color[c] * (0.5 + 0.5 * glow)
    img[..., 3] = np.where(r < 8.5, 255 * np.clip(glow * 1.6, 0, 1), 0)
    for row, line in enumerate(rune):
        for col, ch in enumerate(line):
            if ch != ".":
                py, px = 4 + row, 4 + col
                img[py, px] = (255, 255, 255, 255) if ch == "W" else (*[min(255, v + 90) for v in color], 255)
    return to_image(img)


RUNES = {
    "place_of_power": [
        "....W.....",
        "...WcW....",
        "..WcccW...",
        ".WccWccW..",
        "WccWWWccW.",
        ".WccWccW..",
        "..WcccW...",
        "...WcW....",
        "....W.....",
        "..........",
    ],
    "fire_blessing": [
        "....W.....",
        "...WW.....",
        "...WcW....",
        "..WccW..W.",
        "..WcccWWW.",
        ".WccWccW..",
        ".WcWWWcW..",
        ".WccWccW..",
        "..WcccW...",
        "...WWW....",
    ],
    "water_blessing": [
        "....W.....",
        "....W.....",
        "...WcW....",
        "...WcW....",
        "..WcccW...",
        ".WccWccW..",
        ".WcWccW...",
        ".WccccW...",
        "..WccW....",
        "...WW.....",
    ],
    "wind_blessing": [
        "..........",
        ".WWWWW....",
        "......W...",
        ".WWWW..W..",
        ".....W.W..",
        "WWWWW..W..",
        "......W...",
        ".WWWWW....",
        "..........",
        "..........",
    ],
    "earth_blessing": [
        "..........",
        "...WWWW...",
        "..WccccW..",
        ".WccWccW..",
        ".WcWWWcW..",
        ".WccWccW..",
        "..WccccW..",
        "...WWWW...",
        "..........",
        "..........",
    ],
}
COLORS = {
    "place_of_power": (176, 112, 255),
    "fire_blessing": (255, 122, 31),
    "water_blessing": (63, 156, 255),
    "wind_blessing": (143, 227, 192),
    "earth_blessing": (181, 137, 90),
}


def main():
    save(shrine_side(), ASSETS / "block/shrine_core_side.png")
    save(shrine_top(), ASSETS / "block/shrine_core_top.png")
    save(crystal(), ASSETS / "block/shrine_crystal.png")
    for i, (name, rune) in enumerate(RUNES.items()):
        save(effect_icon(COLORS[name], rune, 800 + i), ASSETS / f"mob_effect/{name}.png")


if __name__ == "__main__":
    main()
