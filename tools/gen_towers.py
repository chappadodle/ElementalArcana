#!/usr/bin/env python3
"""Generates the mage towers' art (docs/superpowers/specs/2026-10-02-mage-towers-design.md):

- the tower's people, an Acolyte and a Magister for each element (64x64, the vanilla illager model's
  texture layout): hooded robes in the element's colours, the Magister's richer, with glowing eyes;
- the Guardian Core item, in two layers: a dark cage and a grey gem the game tints with its element.

Run from the project root:  python3 tools/gen_towers.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/elementalarcana/textures"

SKIN = (176, 164, 154)
SKIN_DARK = (140, 128, 120)
BROW = (52, 44, 40)
BOOT = (40, 30, 30)

# robe, robe shadow, trim, sash/glow
ELEMENTS = {
    "fire": ((138, 38, 28), (96, 24, 18), (232, 172, 62), (255, 140, 40)),
    "water": ((32, 74, 152), (20, 48, 104), (206, 224, 238), (70, 206, 236)),
    "ice": ((150, 198, 230), (104, 152, 194), (244, 252, 255), (130, 210, 255)),
    "wind": ((84, 156, 126), (56, 108, 88), (232, 246, 236), (176, 244, 214)),
    "earth": ((112, 84, 54), (78, 56, 34), (196, 164, 92), (124, 166, 66)),
}


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ASSETS.parent.parent.parent.parent.parent.parent))


def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


class Canvas:
    def __init__(self, seed):
        self.px = np.zeros((64, 64, 4), dtype=np.uint8)
        self.rng = np.random.default_rng(seed)

    def fill(self, rect, base, dark, speckle=0.15, glint=None, glint_chance=0.0):
        x, y, w, h = rect
        for py in range(h):
            for px in range(w):
                n = self.rng.random()
                c = dark if n < speckle else base
                if glint is not None and self.rng.random() < glint_chance:
                    c = glint
                self.px[y + py, x + px] = (*c, 255)

    def row(self, rect, offset, color):
        x, y, w, h = rect
        self.px[y + offset, x:x + w] = (*color, 255)

    def col(self, rect, offset, color, start=0):
        x, y, w, h = rect
        self.px[y + start:y + h, x + offset] = (*color, 255)

    def clear(self, rect):
        x, y, w, h = rect
        self.px[y:y + h, x:x + w] = 0


def head(c, eyes):
    hd = faces(0, 0, 8, 10, 8)
    for rect in hd.values():
        c.fill(rect, SKIN, SKIN_DARK, 0.12)
    x, y, w, h = hd["front"]
    c.px[y + 3, x + 1:x + 7] = (*BROW, 255)  # one heavy brow
    c.px[y + 4, x + 1:x + 3] = (240, 240, 236, 255)
    c.px[y + 4, x + 5:x + 7] = (240, 240, 236, 255)
    c.px[y + 4, x + 2] = (*eyes, 255)
    c.px[y + 4, x + 5] = (*eyes, 255)
    c.px[y + 7, x + 2:x + 6] = (*SKIN_DARK, 255)  # a thin mouth
    nose = faces(24, 0, 2, 4, 2)
    for rect in nose.values():
        c.fill(rect, SKIN, SKIN_DARK, 0.3)


def hood(c, robe, dark, trim):
    hd = faces(32, 0, 8, 12, 8)
    for name, rect in hd.items():
        c.fill(rect, robe, dark, 0.2)
    x, y, w, h = hd["front"]
    c.clear((x + 1, y + 2, w - 2, h - 2))  # the face shows through
    c.px[y + 1, x:x + w] = (*trim, 255)
    for name in ("right", "left", "back"):
        c.row(hd[name], hd[name][3] - 1, trim)


def robe(c, robe_color, dark, trim, sash, magister):
    body = faces(16, 20, 8, 12, 6)
    for rect in body.values():
        c.fill(rect, robe_color, dark, 0.18)
    jacket = faces(0, 38, 8, 20, 6)
    for name, rect in jacket.items():
        c.fill(rect, robe_color, dark, 0.18, glint=trim if magister else None, glint_chance=0.03 if magister else 0)
        if name in ("right", "front", "left", "back"):
            c.row(rect, 7, sash)  # a sash
            c.row(rect, rect[3] - 1, trim)
            if magister:
                c.row(rect, rect[3] - 2, trim)
    c.col(jacket["front"], 3, trim, start=8)
    c.col(jacket["front"], 4, trim, start=8)
    if magister:
        # A mantle over the shoulders.
        for name in ("right", "front", "left", "back"):
            c.row(jacket[name], 0, trim)
            c.row(jacket[name], 1, dark)
        c.fill(jacket["top"], trim, dark, 0.3)


def legs(c, dark):
    leg = faces(0, 22, 4, 12, 4)
    for name, rect in leg.items():
        c.fill(rect, dark, BOOT, 0.2)
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 2, BOOT)
            c.row(rect, rect[3] - 1, BOOT)


def arms(c, robe_color, dark, trim):
    # Crossed arms: sleeves, and the hands in the middle of the forearm box.
    side = faces(44, 22, 4, 8, 4)
    for name, rect in side.items():
        c.fill(rect, robe_color, dark, 0.18)
        if name in ("right", "front", "left", "back"):
            c.row(rect, rect[3] - 1, trim)
    middle = faces(40, 38, 8, 4, 4)
    for name, rect in middle.items():
        c.fill(rect, robe_color, dark, 0.18)
    for name in ("front", "top", "bottom"):
        x, y, w, h = middle[name]
        c.fill((x + 2, y, w - 4, h), SKIN, SKIN_DARK, 0.2)
    # Free arms (casting): sleeves down to the wrist, then the hand.
    arm = faces(40, 46, 4, 12, 4)
    for name, rect in arm.items():
        c.fill(rect, robe_color, dark, 0.18)
        if name in ("right", "front", "left", "back"):
            x, y, w, h = rect
            c.row(rect, h - 4, trim)
            c.fill((x, y + h - 3, w, 3), SKIN, SKIN_DARK, 0.2)
    c.fill(arm["bottom"], SKIN, SKIN_DARK, 0.2)


def mage(element, magister, seed):
    robe_color, dark, trim, sash = ELEMENTS[element]
    if magister:
        robe_color = tuple(max(0, int(v * 0.8)) for v in robe_color)
        dark = tuple(max(0, int(v * 0.75)) for v in dark)
    c = Canvas(seed)
    head(c, sash if magister else (50, 46, 60))
    hood(c, robe_color, dark, trim)
    robe(c, robe_color, dark, trim, sash, magister)
    legs(c, dark)
    arms(c, robe_color, dark, trim)
    return Image.fromarray(c.px, "RGBA")


CORE = [
    "................",
    "......kkkk......",
    ".....kGggGk.....",
    "....kg....gk....",
    "...kg......gk...",
    "..kG........Gk..",
    "..kg........gk..",
    "..kg........gk..",
    "..kg........gk..",
    "..kG........Gk..",
    "...kg......gk...",
    "....kg....gk....",
    ".....kGggGk.....",
    "......kkkk......",
    "................",
    "................",
]
CORE_GEM = [
    "................",
    "................",
    "................",
    "......bbbb......",
    ".....bwllmb.....",
    "....bwllllmb....",
    "....bllwllmb....",
    "....bllllmmb....",
    "....blllmmmb....",
    "....bmllmmmb....",
    ".....bmmmmb.....",
    "......bbbb......",
    "................",
    "................",
    "................",
    "................",
]


def grid_image(grid, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        assert len(row) == 16, (len(row), row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), palette[ch] + (255,))
    return img


def main():
    for i, element in enumerate(ELEMENTS):
        save(mage(element, False, 1300 + i), ASSETS / f"entity/tower_mage/acolyte_{element}.png")
        save(mage(element, True, 1400 + i), ASSETS / f"entity/tower_mage/magister_{element}.png")
    save(grid_image(CORE, {"k": (26, 22, 34), "g": (120, 104, 70), "G": (226, 186, 74)}), ASSETS / "item/guardian_core.png")
    save(grid_image(CORE_GEM, {"b": (110, 110, 110), "m": (170, 170, 170), "l": (215, 215, 215), "w": (255, 255, 255)}),
         ASSETS / "item/guardian_core_gem.png")


if __name__ == "__main__":
    main()
