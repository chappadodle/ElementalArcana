#!/usr/bin/env python3
"""Generates the brews' art (docs/superpowers/specs/2026-10-02-brews-design.md):

- effect icons (18x18, like the shrine effects in gen_world.py): Mana Restoration, Clarity, Focus
  and Warding;
- the Wisp Mote item: a little spark of pale mana.

Run from the project root:  python3 tools/gen_brews.py
"""
import numpy as np
from PIL import Image

from gen_world import ASSETS, effect_icon, save

RUNES = {
    "mana_restoration": [
        "....W.....",
        "...WcW....",
        "..WcccW...",
        "..WcWcW...",
        ".WcWWWcW..",
        ".WccWccW..",
        ".WcccccW..",
        "..WcccW...",
        "...WWW....",
        "..........",
    ],
    "clarity": [
        "..........",
        "...WWWW...",
        "..WccccW..",
        ".WcWWWWcW.",
        ".WcWccWcW.",
        ".WcWWWWcW.",
        "..WccccW..",
        "...WWWW...",
        "..........",
        "..........",
    ],
    "focus": [
        "....W.....",
        "....W.....",
        "..WWWWW...",
        "..Wc.cW...",
        "WWW.W.WWW.",
        "..Wc.cW...",
        "..WWWWW...",
        "....W.....",
        "....W.....",
        "..........",
    ],
    "warding": [
        ".WWWWWWW..",
        ".WcccccW..",
        ".WccWccW..",
        ".WcWWWcW..",
        ".WccWccW..",
        "..WcccW...",
        "...WcW....",
        "....W.....",
        "..........",
        "..........",
    ],
}
COLORS = {
    "mana_restoration": (106, 91, 255),
    "clarity": (143, 227, 255),
    "focus": (255, 211, 106),
    "warding": (127, 184, 201),
}


def wisp_mote():
    """A small spark: a white-hot centre, a violet glow, four short rays."""
    y, x = np.mgrid[0:16, 0:16] + 0.5
    r = np.hypot(x - 8, y - 8)
    img = np.zeros((16, 16, 4))
    glow = np.clip(1 - r / 5.5, 0, 1)
    img[..., 0] = 150 + 105 * glow
    img[..., 1] = 120 + 135 * glow
    img[..., 2] = 255
    img[..., 3] = np.where(r < 5.5, np.clip(glow * 2.2, 0, 1) * 255, 0)
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        for step in (5, 6):
            img[int(8 + dy * step - (dy > 0)), int(8 + dx * step - (dx > 0))] = (210, 200, 255, 200)
    return Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")


def main():
    for i, (name, rune) in enumerate(RUNES.items()):
        save(effect_icon(COLORS[name], rune, 1500 + i), ASSETS / f"mob_effect/{name}.png")
    save(wisp_mote(), ASSETS / "item/wisp_mote.png")


if __name__ == "__main__":
    main()
