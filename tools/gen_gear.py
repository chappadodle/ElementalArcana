#!/usr/bin/env python3
"""Generates the gear art (docs/superpowers/specs/2026-10-02-gear-design.md) as pixel art:

- foci: the Apprentice Wand and the Adept Staff, each as two layers: the wood and metal, and a
  grey gem that the game tints with the focus's element (tint index 1);
- robe icons: hood, robe, trousers and boots for the apprentice and adept sets;
- the robes as worn: armor textures in the game's 64x32 player layout (layer 1: hood, robe and
  boots; layer 2: trousers).

Run from the project root:  python3 tools/gen_gear.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"


def rgba(hex_color, alpha=255):
    return ((hex_color >> 16) & 0xFF, (hex_color >> 8) & 0xFF, hex_color & 0xFF, alpha)


def grid_image(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, (len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), rgba(palette[ch]))
    return img


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


# ---- foci ----
# Layer 0: wood (w/W), metal bands (g/G), outline (k). Layer 1: the gem, in greys (tinted in game).
WAND_BODY = [
    "................",
    "................",
    "................",
    "................",
    "..........kk....",
    ".........kGgk...",
    "........kgGk....",
    ".......kWwk.....",
    "......kWwk......",
    ".....kWwk.......",
    "....kWwk........",
    "...kWwk.........",
    "..kWwk..........",
    "..kwk...........",
    "...k............",
    "................",
]
WAND_GEM = [
    "................",
    "............b...",
    "...........bwb..",
    "..........blwlb.",
    "...........bmlb.",
    "............bb..",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
STAFF_BODY = [
    "................",
    "..........kkk...",
    ".........kG..k..",
    "........kg....k.",
    ".......kgk...k..",
    "......kWkgkkk...",
    ".....kWwk.......",
    ".....kWk........",
    "....kWwk........",
    "...kWwk.........",
    "...kWk..........",
    "..kWwk..........",
    ".kWwk...........",
    ".kwk............",
    "..k.............",
    "................",
]
STAFF_GEM = [
    "................",
    "................",
    "..........bww...",
    "..........wlwb..",
    "..........lmlb..",
    "...........bb...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
WOOD = {"k": 0x2B1A10, "W": 0x8A5A32, "w": 0x5E3B1E, "g": 0xC9A23E, "G": 0xF2D774}
DARK_WOOD = {"k": 0x1A0F14, "W": 0x5A3A4A, "w": 0x3A2230, "g": 0xC9A23E, "G": 0xF2D774}
GEM = {"b": 0x8A8A8A, "m": 0xB4B4B4, "l": 0xDCDCDC, "w": 0xFFFFFF}

# ---- robe icons ----
HOOD = [
    "................",
    "......kkkk......",
    ".....kccccck....",
    "....kcccccdck...",
    "...kccdkkkkcck..",
    "...kcdk....kck..",
    "...kck......kk..",
    "...kck......kk..",
    "...kcck....kck..",
    "...kcccttttcck..",
    "..kcccccccccdck.",
    "..ktttttttttttk.",
    "...kkkkkkkkkkk..",
    "................",
    "................",
    "................",
]
ROBE = [
    "................",
    "....kkk..kkk....",
    "...kcckkkkcck...",
    "..kcccctccccck..",
    ".kcdcccctcccdck.",
    ".kcdkccctccdkck.",
    ".ktk.kcctcck.ktk",
    "..k..kttttttk.k.",
    ".....kcctccck...",
    ".....kcctccck...",
    "....kccctcccdk..",
    "....kcccctcccdk.",
    "...kccccctcccdk.",
    "...kttttttttttk.",
    "...kkkkkkkkkkkk.",
    "................",
]
TROUSERS = [
    "................",
    "....kkkkkkkk....",
    "....kttttttk....",
    "....kcccccdk....",
    "....kccckccdk...",
    "...kcccdkcccdk..",
    "...kcccdkkcccdk.",
    "...kcccdk.kcccdk",
    "...kcccdk.kcccdk",
    "..kcccdk..kcccdk",
    "..kcccdk...kccck",
    "..ktttk....ktttk",
    "..kkkkk....kkkkk",
    "................",
    "................",
    "................",
]
BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "...kkkk...kkkk..",
    "...kttk...kttk..",
    "...kccdk..kccdk.",
    "...kccdk..kccdk.",
    "...kccdk..kccdk.",
    "...kcccdk.kcccdk",
    "..kccccdk.kcccdk",
    ".kcccccdkkccccdk",
    ".kkkkkkkkkkkkkkk",
    "................",
    "................",
    "................",
]
TIERS = {
    "apprentice": {"k": 0x141C33, "c": 0x3D5A9E, "d": 0x27396B, "t": 0xB08040},
    "adept": {"k": 0x1E1033, "c": 0x6A3D9E, "d": 0x452670, "t": 0xE8C34A},
}


# ---- worn robes (64x32 armor layout) ----
def cloth(rng, base, dark, w, h):
    """A cloth patch: the base colour with a little weave noise and darker folds."""
    noise = rng.random((h, w))
    patch = np.zeros((h, w, 4), dtype=np.uint8)
    for c in range(3):
        mix = np.where(noise < 0.18, dark[c], base[c]) + (noise - 0.5) * 10
        patch[..., c] = np.clip(mix, 0, 255)
    patch[..., 3] = 255
    return patch


def paint(img, rect, patch):
    x, y, w, h = rect
    img[y:y + h, x:x + w] = patch[:h, :w]


def hline(img, x, y, w, color):
    img[y, x:x + w] = color


def worn_layers(name, base_hex, dark_hex, trim_hex, seed):
    base = rgba(base_hex)
    dark = rgba(dark_hex)
    trim = np.array(rgba(trim_hex), dtype=np.uint8)
    leather = np.array(rgba(0x3A2614), dtype=np.uint8)
    rng = np.random.default_rng(seed)
    layer1 = np.zeros((32, 64, 4), dtype=np.uint8)
    layer2 = np.zeros((32, 64, 4), dtype=np.uint8)

    # Hood (head box at 0,0, 8x8x8): cloth all round, the face left open in front.
    for rect in ((8, 0, 8, 8), (16, 0, 8, 8), (0, 8, 8, 8), (16, 8, 8, 8), (24, 8, 8, 8)):
        paint(layer1, rect, cloth(rng, base, dark, rect[2], rect[3]))
    front = cloth(rng, base, dark, 8, 8)
    front[2:8, 1:7, 3] = 0
    paint(layer1, (8, 8, 8, 8), front)
    for x0 in (0, 8, 16, 24):
        hline(layer1, x0, 15, 8, trim)

    # Robe (body box at 16,16, 8x12x4; arms at 40,16, 4x12x4): a centre trim and a belt.
    for rect in ((20, 16, 8, 4), (28, 16, 8, 4), (16, 20, 4, 12), (20, 20, 8, 12), (28, 20, 4, 12), (32, 20, 8, 12)):
        paint(layer1, rect, cloth(rng, base, dark, rect[2], rect[3]))
    layer1[20:32, 23:25] = trim
    for x0, w in ((16, 4), (20, 8), (28, 4), (32, 8)):
        hline(layer1, x0, 28, w, trim)
    for rect in ((44, 16, 4, 4), (48, 16, 4, 4), (40, 20, 4, 12), (44, 20, 4, 12), (48, 20, 4, 12), (52, 20, 4, 12)):
        paint(layer1, rect, cloth(rng, base, dark, rect[2], rect[3]))
    for x0 in (40, 44, 48, 52):
        layer1[30:32, x0:x0 + 4] = trim

    # Boots (leg box at 0,16, 4x12x4): leather on the lower third of the leg.
    for x0 in (0, 4, 8, 12):
        layer1[27:32, x0:x0 + 4] = leather
        hline(layer1, x0, 27, 4, trim)
    layer1[16:20, 8:12] = leather

    # Trousers (layer 2): the legs in cloth, and the waist of the body box with a belt.
    for rect in ((4, 16, 4, 4), (0, 20, 4, 12), (4, 20, 4, 12), (8, 20, 4, 12), (12, 20, 4, 12)):
        paint(layer2, rect, cloth(rng, base, dark, rect[2], rect[3]))
    for rect in ((16, 28, 4, 4), (20, 28, 8, 4), (28, 28, 4, 4), (32, 28, 8, 4)):
        paint(layer2, rect, cloth(rng, base, dark, rect[2], rect[3]))
    for x0, w in ((16, 4), (20, 8), (28, 4), (32, 8)):
        hline(layer2, x0, 28, w, trim)
    for x0 in (0, 4, 8, 12):
        hline(layer2, x0, 31, 4, trim)

    save(Image.fromarray(layer1, "RGBA"), ASSETS / f"models/armor/{name}_layer_1.png")
    save(Image.fromarray(layer2, "RGBA"), ASSETS / f"models/armor/{name}_layer_2.png")


def main():
    save(grid_image(WAND_BODY, WOOD), ASSETS / "item/apprentice_wand.png")
    save(grid_image(WAND_GEM, GEM), ASSETS / "item/apprentice_wand_gem.png")
    save(grid_image(STAFF_BODY, DARK_WOOD), ASSETS / "item/adept_staff.png")
    save(grid_image(STAFF_GEM, GEM), ASSETS / "item/adept_staff_gem.png")
    for tier, palette in TIERS.items():
        for piece, grid in (("hood", HOOD), ("robe", ROBE), ("trousers", TROUSERS), ("boots", BOOTS)):
            save(grid_image(grid, palette), ASSETS / f"item/{tier}_{piece}.png")
    worn_layers("apprentice", 0x3D5A9E, 0x27396B, 0xB08040, 501)
    worn_layers("adept", 0x6A3D9E, 0x452670, 0xE8C34A, 502)


if __name__ == "__main__":
    main()
